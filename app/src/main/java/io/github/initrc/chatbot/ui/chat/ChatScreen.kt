package io.github.initrc.chatbot.ui.chat

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.initrc.chatbot.R
import io.github.initrc.chatbot.data.ChatRole
import io.github.initrc.chatbot.data.Message
import io.github.initrc.chatbot.data.db.ConversationSummary
import io.github.initrc.chatbot.ui.common.CircleIconButton
import io.github.initrc.chatbot.ui.settings.SettingsViewModel
import io.github.initrc.chatbot.ui.theme.ChatbotTheme
import kotlinx.coroutines.launch

private const val TAG = "ChatScreen"

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = hiltViewModel(),
    conversationViewModel: ConversationViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val chatState by chatViewModel.chatState.collectAsStateWithLifecycle()
    val conversationId by chatViewModel.conversationId.collectAsStateWithLifecycle()
    val recentConversations by conversationViewModel.recentConversations.collectAsStateWithLifecycle()
    val currentModel by settingsViewModel.currentModel.collectAsStateWithLifecycle()
    val allModels by settingsViewModel.allModels.collectAsStateWithLifecycle()
    val apiKey by settingsViewModel.apiKey.collectAsStateWithLifecycle()
    val baseUrl by settingsViewModel.baseUrl.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingConversationDeletionIds = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val visibleConversations = recentConversations.filterNot { conversation ->
        pendingConversationDeletionIds.contains(conversation.id)
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ConversationDrawerLayout(
        recentConversations = visibleConversations,
        selectedConversationId = conversationId,
        drawerState = drawerState,
        onNewChatClick = {
            chatViewModel.startNewChat()
            scope.launch { drawerState.close() }
        },
        onConversationClick = { selectedConversationId ->
            chatViewModel.loadConversation(selectedConversationId)
            scope.launch { drawerState.close() }
        },
        onConversationDeleteClick = { conversation ->
            scope.launch {
                handleConversationDeleteClick(
                    conversation = conversation,
                    chatViewModel = chatViewModel,
                    conversationViewModel = conversationViewModel,
                    drawerState = drawerState,
                    snackbarHostState = snackbarHostState,
                    pendingConversationDeletionIds = pendingConversationDeletionIds,
                )
            }
        },
        canDeleteConversation = { candidateConversationId ->
            chatState == ChatState.IDLE || candidateConversationId != conversationId
        },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            ChatScreenContent(
                messages = messages,
                chatState = chatState,
                onConversationListClick = {
                    scope.launch { drawerState.open() }
                },
                onSendClick = chatViewModel::onSendClick,
                currentModel = currentModel,
                allModels = allModels,
                onModelSelect = settingsViewModel::setCurrentModel,
                apiKey = apiKey,
                baseUrl = baseUrl,
                onApiKeyChange = settingsViewModel::setApiKey,
                onBaseUrlChange = settingsViewModel::setBaseUrl,
                modifier = Modifier.fillMaxSize(),
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }
}

private suspend fun handleConversationDeleteClick(
    conversation: ConversationSummary,
    chatViewModel: ChatViewModel,
    conversationViewModel: ConversationViewModel,
    drawerState: androidx.compose.material3.DrawerState,
    snackbarHostState: SnackbarHostState,
    pendingConversationDeletionIds: MutableList<String>,
) {
    val wasSelected = conversation.id == chatViewModel.conversationId.value
    pendingConversationDeletionIds.add(conversation.id)
    try {
        if (wasSelected) {
            chatViewModel.startNewChat()
        }
        drawerState.close()

        val snackbarResult = snackbarHostState.showSnackbar(
            message = "Conversation deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Long,
        )

        if (snackbarResult == SnackbarResult.ActionPerformed) {
            if (wasSelected && chatViewModel.conversationId.value == null) {
                chatViewModel.loadConversation(conversation.id)
            }
            return
        }

        val deleted = conversationViewModel.deleteConversation(conversation.id)
        if (!deleted && wasSelected && chatViewModel.conversationId.value == null) {
            Log.w(
                TAG,
                "Conversation delete affected 0 rows for id=${conversation.id}; restoring selection",
            )
            chatViewModel.loadConversation(conversation.id)
        } else if (chatViewModel.conversationId.value == conversation.id) {
            chatViewModel.startNewChat()
        }
    } finally {
        pendingConversationDeletionIds.remove(conversation.id)
    }
}

@Composable
private fun ChatScreenContent(
    messages: List<Message>,
    chatState: ChatState,
    onConversationListClick: () -> Unit,
    onSendClick: (String, String) -> Unit,
    currentModel: String,
    allModels: List<String>,
    onModelSelect: (String) -> Unit,
    apiKey: String,
    baseUrl: String,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    modifier: Modifier
) {
    var sendViewHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxSize()) {
        ModelHeader(
            onConversationListClick = onConversationListClick,
            currentModel = currentModel,
            allModels = allModels,
            onModelSelect = onModelSelect,
            apiKey = apiKey,
            baseUrl = baseUrl,
            onApiKeyChange = onApiKeyChange,
            onBaseUrlChange = onBaseUrlChange,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            MessageList(
                messages = messages,
                modifier = Modifier.fillMaxSize(),
                bottomContentPadding = sendViewHeight + 8.dp
            )
            SendView(
                onSendClick = onSendClick,
                isEnabled = chatState == ChatState.IDLE,
                model = currentModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        sendViewHeight = with(density) { coordinates.size.height.toDp() }
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelHeader(
    onConversationListClick: () -> Unit,
    currentModel: String,
    allModels: List<String>,
    onModelSelect: (String) -> Unit,
    apiKey: String,
    baseUrl: String,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    modifier: Modifier,
) {
    var showModelBottomSheet by remember { mutableStateOf(false) }
    var showApiBottomSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val sortedModels = remember(allModels) { allModels.sorted() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        ConversationDrawerButton(
            onClick = onConversationListClick,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        TextButton(
            onClick = { showModelBottomSheet = true },
            modifier = Modifier.height(48.dp)
        ) {
            Text(
                text = "$currentModel ▾",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Box(
           modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = "⋮",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Set up API") },
                    onClick = {
                        showMenu = false
                        showApiBottomSheet = true
                    },
                )
            }
        }

    }

    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelBottomSheet = false },
            sheetState = sheetState,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                item {
                    Text(
                        text = "Select Model",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                }
                items(sortedModels) { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    onModelSelect(model)
                                    sheetState.hide()
                                    showModelBottomSheet = false
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = model,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (model == currentModel) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (model == currentModel) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showApiBottomSheet) {
        var tempApiKey by remember { mutableStateOf(apiKey) }
        var tempBaseUrl by remember { mutableStateOf(baseUrl) }

        ModalBottomSheet(
            onDismissRequest = { showApiBottomSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Set up API",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = tempBaseUrl,
                    onValueChange = { tempBaseUrl = it },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = tempApiKey,
                    onValueChange = { tempApiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { showApiBottomSheet = false },
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            onApiKeyChange(tempApiKey)
                            onBaseUrlChange(tempBaseUrl)
                            showApiBottomSheet = false
                        },
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
@Composable
fun MessageList(
    messages: List<Message>,
    modifier: Modifier,
    bottomContentPadding: Dp = 0.dp
) {
    val listState = rememberLazyListState()
    val lastMessageContent = messages.lastOrNull()?.content
    var lastMessageHeight by remember { mutableIntStateOf(0) }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        state = listState,
    ) {
        itemsIndexed(
            items = messages
        ) { index, message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (index == messages.lastIndex) {
                            Modifier.onSizeChanged { size ->
                                lastMessageHeight = size.height
                            }
                        } else {
                            Modifier
                        }
                    ),
                horizontalArrangement = if (message.isFromMe()) {
                    Arrangement.End
                } else {
                    Arrangement.Start
                },
            ) {
                MessageView(message)
            }
        }
        item { Spacer(modifier = Modifier.height(bottomContentPadding)) }
    }

    LaunchedEffect(messages.size, lastMessageContent, lastMessageHeight) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size)
        }
    }
}

@Composable
fun MessageView(message: Message) {
    if (message.isFromMe()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .fillMaxWidth(0.8f)
                .heightIn(min = 56.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            ChatMarkdown(content = message.content)
        }
    } else {
        ChatMarkdown(
            content = message.content,
        )
    }
}

private fun Message.isFromMe(): Boolean = this.role == ChatRole.USER

@Preview(
    name = "MessageList (light theme)",
    showBackground = true
)
@Preview(
    name = "MessageList (dark theme)",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun MessageListPreview() {
    ChatbotTheme {
        Surface {
            MessageList(
                messages = listOf(
                    Message(
                        role = ChatRole.USER,
                        content = """
                            **Can you summarize this Kotlin snippet?**

                            ```kotlin
                            val tokens = text.length / 4.0
                            ```
                        """.trimIndent()
                    ),
                    Message(
                        role = ChatRole.ASSISTANT,
                        content = """
                            # Token estimate

                            The snippet uses a rough character-based estimate.

                            - `text.length` counts characters.
                            - Dividing by `4.0` approximates tokens.
                            - It is useful for quick budgeting, not exact accounting.

                            ```text
                            Estimated tokens: 7.5
                            ```

                            > For precise counts, use a tokenizer for the target model.

                            See the [Compose docs](https://developer.android.com/jetpack/compose).
                        """.trimIndent()
                    ),
                    Message(
                        role = ChatRole.USER,
                        content = "Thanks. Also make **user markdown** render inside my bubble."
                    ),
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SendView(
    onSendClick: (String, String) -> Unit,
    isEnabled: Boolean,
    model: String,
    modifier: Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val isEnabled = isEnabled && text.isNotEmpty()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = {
                Text(text = "Ask AI")
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f),
            maxLines = 5,
        )
        if (isEnabled) {
            CircleIconButton(
                onClick = {
                    focusManager.clearFocus()
                    onSendClick(text, model)
                    text = ""
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.send_24),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentDescription = "Send",
                )
            }
        }
    }
}

@Preview(
    name = "ChatScreen (light theme)",
    showBackground = true
)
@Preview(
    name = "ChatScreen (dark theme)",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun ChatScreenPreview() {
    ChatbotTheme {
        Surface {
            ChatScreenContent(
                messages = listOf(
                    Message(role = ChatRole.USER, content = "Text from me"),
                ) + List(30) { index ->
                    Message(role = ChatRole.ASSISTANT, content = "Text $index from bot")
                },
                chatState = ChatState.IDLE,
                onConversationListClick = {},
                onSendClick = { _: String, _: String -> },
                currentModel = "llama-3.1-8b-instant",
                allModels = listOf("llama-3.1-8b-instant"),
                onModelSelect = {},
                apiKey = "",
                baseUrl = "https://api.groq.com/openai/v1",
                onApiKeyChange = {},
                onBaseUrlChange = {},
                modifier = Modifier,
            )
        }
    }
}
