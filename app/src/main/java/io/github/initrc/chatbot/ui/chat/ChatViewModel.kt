package io.github.initrc.chatbot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.initrc.chatbot.data.ChatRepository
import io.github.initrc.chatbot.data.ChatRole
import io.github.initrc.chatbot.data.Message
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val FLOW_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val _chatState = MutableStateFlow(ChatState.IDLE)
    val chatState = _chatState.asStateFlow()

    private val _conversationId = MutableStateFlow<String?>(null)
    val conversationId = _conversationId.asStateFlow()

    private val _streamingState = MutableStateFlow<StreamingState?>(null)

    private val persistedMessages = _conversationId
        .flatMapLatest { conversationId ->
            if (conversationId == null) {
                flowOf(PersistedMessages())
            } else {
                chatRepository.observeMessages(conversationId).map { messages ->
                    PersistedMessages(
                        conversationId = conversationId,
                        messages = messages,
                    )
                }.onStart {
                    emit(PersistedMessages(conversationId = conversationId))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FLOW_SUBSCRIPTION_TIMEOUT_MILLIS),
            initialValue = PersistedMessages(),
        )

    val messages = combine(
        persistedMessages,
        _streamingState,
    ) { persistedMessages, streamingState ->
        buildVisibleMessages(
            persistedMessages = persistedMessages.messages,
            conversationId = persistedMessages.conversationId,
            streamingState = streamingState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(FLOW_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = emptyList(),
    )

    fun startNewChat() {
        _conversationId.value = null
    }

    fun loadConversation(conversationId: String) {
        _conversationId.value = conversationId
    }

    fun onSendClick(inputText: String, model: String) {
        val promptText = inputText.trim()
        if (promptText.isEmpty() || _chatState.value == ChatState.BUSY) {
            return
        }

        viewModelScope.launch {
            val conversationId = ensureSelectedConversation(model)

            _streamingState.value = StreamingState(
                conversationId = conversationId,
                messageContent = "",
                isFinished = false,
            )
            _chatState.value = ChatState.BUSY

            try {
                chatRepository.sendMessage(conversationId, promptText, model).collect { chunk ->
                    appendStreamingChunk(conversationId, chunk)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // TODO surface error state to the UI
            } finally {
                _chatState.value = ChatState.IDLE
                markStreamingFinished(conversationId)
            }
        }
    }

    private suspend fun ensureSelectedConversation(model: String): String {
        _conversationId.value?.let { return it }

        val conversationId = chatRepository.createConversation(model)
        loadConversation(conversationId)
        return conversationId
    }

    private fun appendStreamingChunk(conversationId: String, chunk: String) {
        val currentStreamingState = _streamingState.value
        _streamingState.value = if (currentStreamingState == null ||
            currentStreamingState.conversationId != conversationId
        ) {
            StreamingState(
                conversationId = conversationId,
                messageContent = chunk,
                isFinished = false,
            )
        } else {
            currentStreamingState.copy(
                messageContent = currentStreamingState.messageContent + chunk,
                isFinished = false,
            )
        }
    }

    private fun markStreamingFinished(conversationId: String) {
        val currentStreamingState = _streamingState.value ?: return
        if (currentStreamingState.conversationId != conversationId) return
        _streamingState.value = currentStreamingState.copy(isFinished = true)
    }
}

/**
 * Freshest assistant message content for the single send currently in flight.
 *
 * Room persistence for streamed assistant content is intentionally throttled, so this state can be
 * slightly ahead of the database while the response is still arriving, and briefly after the
 * stream has finished.
 */
private data class StreamingState(
    val conversationId: String,
    val messageContent: String,
    val isFinished: Boolean,
)

private data class PersistedMessages(
    val conversationId: String? = null,
    val messages: List<Message> = emptyList(),
)

private fun List<Message>.lastAssistantMessageContent(): String? {
    return lastOrNull()
        ?.takeIf { it.role == ChatRole.ASSISTANT }
        ?.content
}

/**
 * Builds the message list rendered by the chat screen from persisted Room data and the freshest
 * in-memory assistant content for the currently streaming conversation.
 */
private fun buildVisibleMessages(
    persistedMessages: List<Message>,
    conversationId: String?,
    streamingState: StreamingState?,
): List<Message> {
    if (streamingState == null || conversationId != streamingState.conversationId) {
        return persistedMessages
    }

    if (streamingState.isFinished &&
        persistedMessages.lastAssistantMessageContent() == streamingState.messageContent
    ) {
        return persistedMessages
    }

    val visibleMessages = persistedMessages.toMutableList()
    if (visibleMessages.lastOrNull()?.role == ChatRole.ASSISTANT) {
        visibleMessages.replaceLast { lastMessage ->
            lastMessage.copy(content = streamingState.messageContent)
        }
    } else if (streamingState.messageContent.isNotEmpty()) {
        visibleMessages += Message(
            role = ChatRole.ASSISTANT,
            content = streamingState.messageContent,
        )
    }

    return visibleMessages
}

private inline fun MutableList<Message>.replaceLast(transform: (Message) -> Message) {
    val lastMessage = lastOrNull() ?: return
    this[lastIndex] = transform(lastMessage)
}

enum class ChatState {
    IDLE, BUSY
}
