# Chat Thread History Panel Technical Plan

## Goal

Add persistent chat threads to the app and expose them from `ModelHeader` through a start-side panel. The panel should behave like a modal bottom sheet conceptually, but slide in from the left/start edge, list recent conversations, and include an extended floating action button labeled `Chat` with a create-chat icon. Selecting a conversation should navigate to that thread and load its messages from local storage.

## Current State

- `ChatScreen.kt` owns the main Compose layout, including `ModelHeader`, `MessageList`, and `SendView`.
- `ModelHeader` currently shows the selected model in the center and a right-side overflow menu for API setup.
- `ChatViewModel` stores `messages` in an in-memory `MutableStateFlow`; messages are lost when the process is killed or a new chat is started.
- `ChatRepository` only prepares messages for `ChatRemoteDataSource`; it has no durable chat-history API.
- `ChatLocalDataSource` is a fake `ChatService` implementation, not a persistence layer.
- The project has Hilt and KSP already, but no Room dependency, Room database, DAO, or app navigation graph.

## Proposed Architecture

Use Room as the local source of truth for chat history, and use Compose Material 3 `ModalNavigationDrawer` for the start-side panel.

High-level flow:

1. `MainActivity` hosts a `NavHost` with chat destinations.
2. `ChatScreen` is wrapped in a `ModalNavigationDrawer`.
3. `ModelHeader` gets a start-aligned thread icon button that opens the drawer.
4. `ChatViewModel` observes:
   - the active thread messages from Room,
   - recent conversation summaries from Room,
   - transient send state.
5. `ChatRepository` coordinates persistence and remote streaming:
   - create or load a conversation,
   - insert the user message,
   - insert an assistant placeholder,
   - stream remote chunks,
   - update the assistant message row as chunks arrive.

Room is the right fit because chat history is structured relational data, Room verifies SQL at compile time, and Room DAOs can expose observable `Flow` queries for the recent-conversation list and active thread messages.

## Dependencies

Update `gradle/libs.versions.toml` and `app/build.gradle.kts`.

Add Room:

```toml
[versions]
room = "2.8.4"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
```

```kotlin
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)
testImplementation(libs.androidx.room.testing)
```

Add Navigation Compose if it is not already available transitively through the existing Hilt navigation dependency. Prefer an explicit catalog entry for `androidx.navigation:navigation-compose` so route ownership is clear.

## Data Model

Create a new persistence package, for example:

- `app/src/main/java/io/github/initrc/chatbot/data/db/ChatDatabase.kt`
- `app/src/main/java/io/github/initrc/chatbot/data/db/ConversationEntity.kt`
- `app/src/main/java/io/github/initrc/chatbot/data/db/MessageEntity.kt`
- `app/src/main/java/io/github/initrc/chatbot/data/db/ChatDao.kt`
- `app/src/main/java/io/github/initrc/chatbot/data/db/ChatDatabaseModule.kt`

Proposed entities:

```kotlin
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val model: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val lastMessagePreview: String?,
    val messageCount: Int,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("conversationId"),
        Index(value = ["conversationId", "position"], unique = true),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val position: Int,
    val role: String,
    val content: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val status: String,
)
```

Recommended enum storage:

- Store `role` as a stable string matching the API roles: `system`, `user`, `assistant`.
- Store `status` as `complete`, `streaming`, or `failed`.
- Do not store the global system prompt as a visible message. Keep injecting it in `ChatRemoteDataSource.createChatRequest()`.

Conversation summary model used by the UI:

```kotlin
data class ConversationSummary(
    val id: String,
    val title: String,
    val preview: String?,
    val updatedAtMillis: Long,
    val messageCount: Int,
)
```

## DAO Contract

Use `Flow` for observable reads and `suspend` for one-shot writes.

```kotlin
@Dao
interface ChatDao {
    @Query(
        """
        SELECT id, title, lastMessagePreview AS preview, updatedAtMillis, messageCount
        FROM conversations
        WHERE messageCount > 0
        ORDER BY updatedAtMillis DESC
        LIMIT :limit
        """
    )
    fun observeRecentConversations(limit: Int): Flow<List<ConversationSummary>>

    @Query(
        """
        SELECT *
        FROM messages
        WHERE conversationId = :conversationId
        ORDER BY position ASC
        """
    )
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT *
        FROM messages
        WHERE conversationId = :conversationId
        ORDER BY position ASC
        """
    )
    suspend fun getMessagesSnapshot(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConversation(entity: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(entity: MessageEntity)

    @Query(
        """
        UPDATE messages
        SET content = :content, updatedAtMillis = :updatedAtMillis, status = :status
        WHERE id = :messageId
        """
    )
    suspend fun updateMessageContent(
        messageId: String,
        content: String,
        updatedAtMillis: Long,
        status: String,
    )

    @Query(
        """
        UPDATE conversations
        SET title = :title,
            lastMessagePreview = :preview,
            updatedAtMillis = :updatedAtMillis,
            messageCount = messageCount + :messageDelta
        WHERE id = :conversationId
        """
    )
    suspend fun updateConversationSummary(
        conversationId: String,
        title: String,
        preview: String?,
        updatedAtMillis: Long,
        messageDelta: Int,
    )
}
```

For consistency, wrap multi-step writes in `@Transaction` methods on a local data source or DAO where practical.

## Repository Changes

Extend `ChatRepository` beyond remote streaming.

New read APIs:

```kotlin
fun observeRecentConversations(limit: Int = 30): Flow<List<ConversationSummary>>
fun observeMessages(conversationId: String): Flow<List<Message>>
suspend fun createConversation(model: String): String
suspend fun loadConversationMessages(conversationId: String): List<Message>
```

Replace the current `sendMessage(messages, model)` usage with a thread-aware method:

```kotlin
suspend fun sendMessage(
    conversationId: String,
    promptText: String,
    model: String,
): Flow<SendProgress>
```

Send algorithm:

1. Trim and validate the prompt. Ignore blank sends at the UI boundary and repository boundary.
2. Insert a user `MessageEntity`.
3. Insert an assistant placeholder `MessageEntity` with `status = streaming`.
4. Read the full conversation snapshot from Room.
5. Compress the snapshot with `ChatContextCompressor`.
6. Start `ChatRemoteDataSource.sendMessage(...)`.
7. For each emitted chunk:
   - append to an in-memory assistant buffer,
   - update the assistant placeholder row in Room,
   - update conversation preview and timestamp.
8. On completion, mark the assistant message `complete`.
9. On error, mark it `failed`, preserve partial assistant content if any, and expose the error through `ChatUiState`.

This makes Room the display source of truth. The UI should not separately append streamed text to `_messages`; it should observe Room updates.

## Navigation

Introduce explicit chat routes in `MainActivity`.

Recommended destinations:

```kotlin
object ChatRoutes {
    const val New = "chat/new/{draftId}"
    const val Thread = "chat/thread/{conversationId}"

    fun newChat(draftId: String) = "chat/new/$draftId"
    fun thread(conversationId: String) = "chat/thread/$conversationId"
}
```

Use `chat/new/{draftId}` instead of a single static `chat/new` route so tapping the `Chat` FAB while already on a new chat can still create a fresh destination and reset the draft state.

Behavior:

- App start navigates to `ChatRoutes.newChat(UUID.randomUUID().toString())`.
- The drawer FAB navigates to a new draft route.
- A recent conversation row navigates to `ChatRoutes.thread(conversationId)`.
- `ChatViewModel` receives `conversationId` or `draftId` from `SavedStateHandle`.
- For a draft route, the conversation is created lazily on first send to avoid empty conversations in the recent list.
- After the first message creates a real conversation, navigate or replace route to `ChatRoutes.thread(newConversationId)` so process recreation can reload it.

If adding Navigation Compose is considered too much for this feature, a smaller alternative is to keep one `ChatScreen` and treat "navigate" as changing `activeConversationId` in the view model. The explicit route approach is preferable because it preserves thread identity across process recreation and back stack operations.

## ViewModel State

Replace separate `messages` and `chatState` flows with a single UI state.

```kotlin
data class ChatUiState(
    val activeConversationId: String?,
    val messages: List<Message> = emptyList(),
    val recentConversations: List<ConversationSummary> = emptyList(),
    val chatState: ChatState = ChatState.IDLE,
    val errorMessage: String? = null,
)
```

Responsibilities:

- Collect recent conversations once for the drawer.
- Collect active thread messages when `conversationId` changes.
- Keep draft messages empty until first send creates a persisted conversation.
- Cancel active message collection when navigating to a different thread.
- Guard `onSendClick` while `chatState == BUSY`.
- Surface send errors without losing persisted messages.

## UI Plan

### Header

Refactor `ModelHeader` from a centered `Box` into a width-aware row:

- Start: thread icon `IconButton`, content description `Open chats`.
- Center: model selector `TextButton`, `maxLines = 1`, `overflow = TextOverflow.Ellipsis`.
- End: existing overflow menu.

Add parameters:

```kotlin
onThreadsClick: () -> Unit
```

Keep model and API setup behavior unchanged.

### Drawer

Wrap the private `ChatScreen(...)` body in:

```kotlin
ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = { RecentConversationsDrawer(...) },
) {
    // existing chat screen content
}
```

Drawer content:

- Use `ModalDrawerSheet`.
- Header text: `Chats`.
- `LazyColumn` of recent conversations using `NavigationDrawerItem` or a custom compact row.
- Show selected state when `summary.id == activeConversationId`.
- Use title as primary text and preview or formatted updated time as secondary text.
- Keep rows at a stable height to avoid layout shift while streaming updates change previews.
- Put an `ExtendedFloatingActionButton` at the bottom:

```kotlin
ExtendedFloatingActionButton(
    text = { Text("Chat") },
    icon = {
        Icon(
            painter = painterResource(R.drawable.create_chat_24),
            contentDescription = null,
        )
    },
    onClick = onNewChatClick,
)
```

Click behavior:

- Thread icon opens the drawer with `drawerState.open()`.
- Recent conversation click closes the drawer, then navigates to the thread.
- FAB click closes the drawer, then navigates to a new draft chat.
- The drawer should remain start-edge aware by relying on `ModalNavigationDrawer` rather than a custom offset animation.

### Icons

The project currently uses drawable vector icons instead of Compose Material Icons. Keep that pattern:

- Add `app/src/main/res/drawable/chat_threads_24.xml` for the header icon.
- Add `app/src/main/res/drawable/create_chat_24.xml` for the FAB icon.

## File-Level Implementation Sequence

1. Add Room and Navigation Compose dependencies.
2. Add Room entities, DAO, database, and Hilt module.
3. Add mapper functions between `MessageEntity`, API `Message`, `ChatRole`, and UI summaries.
4. Add `ChatHistoryLocalDataSource` for DAO transactions.
5. Update `ChatRepository` to expose recent threads, active messages, create-thread, and thread-aware send APIs.
6. Refactor `ChatViewModel` to use `ChatUiState`, route arguments, and Room flows.
7. Update `MainActivity` to host the chat navigation graph.
8. Update `ChatScreen.kt`:
   - add drawer state,
   - pass recent conversations and navigation callbacks,
   - add `RecentConversationsDrawer`,
   - add start icon to `ModelHeader`,
   - wire FAB and row clicks.
9. Add vector drawables.
10. Update previews with sample `ConversationSummary` data and no-op navigation callbacks.
11. Add tests and run verification.

## Testing Plan

Unit tests:

- DAO tests with `room-testing` and an in-memory database:
  - inserts a conversation and messages,
  - observes recent conversations sorted by `updatedAtMillis DESC`,
  - filters empty conversations from recents,
  - deletes messages through cascade when a conversation is deleted.
- Repository tests with a fake remote data source:
  - creates a conversation on first send,
  - persists user and assistant messages,
  - updates assistant content incrementally during streaming,
  - marks assistant message failed when streaming throws.
- ViewModel tests:
  - new draft starts empty,
  - thread route loads messages from Room,
  - recent conversation click target is exposed to UI callback,
  - busy state disables duplicate sends.

UI or instrumentation tests:

- Header thread icon opens the start drawer.
- Drawer lists recent conversations.
- `Chat` FAB creates or navigates to a new chat.
- Selecting a recent conversation closes the drawer and shows persisted messages.

Verification commands:

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Edge Cases

- Blank prompt: do not create a conversation or message.
- First send failure in a draft: either keep the failed persisted conversation visible for debugging, or delete it if no assistant content was produced. Choose one behavior before implementation.
- Switching threads while streaming: repository updates the original assistant message by ID; the active screen observes only its selected thread.
- Long model names in `ModelHeader`: use ellipsis to avoid overlapping the new start icon.
- Long histories: use Room ordering and context compression for sends; consider Paging only after the recent list or message list becomes large enough to justify it.
- Process death on draft route before first send: no persisted conversation is expected.
- Process death during streaming: the assistant message may remain `streaming`; on next load, normalize stale `streaming` rows to `failed` or `interrupted`.

## Open Decisions

- Whether empty conversations should ever appear in recents. The recommended default is no.
- Whether to support delete, rename, or search in this first implementation. The recommended default is out of scope.
- Whether failed assistant messages should show a retry affordance. The recommended default is to persist failure status now and add retry later.
- Whether conversation titles should be generated locally from the first user message or via an LLM summary. The recommended default is local truncation of the first user message.

## References

- Android Compose navigation drawer guidance: `ModalNavigationDrawer`, `ModalDrawerSheet`, `DrawerState`, and `NavigationDrawerItem`.
- Android Room guidance: use Room for structured local persistence, `suspend` DAO methods for one-shot writes, and `Flow` DAO methods for observable reads.
