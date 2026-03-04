package io.github.initrc.chatbot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.initrc.chatbot.data.ChatRepository
import io.github.initrc.chatbot.data.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.listOf

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val _messages = MutableStateFlow(listOf<Message>())
    val messages: StateFlow<List<Message>> = _messages

    private val _chatState = MutableStateFlow(ChatState.IDLE)
    val chatState: StateFlow<ChatState> = _chatState

    fun onSendClick(inputText: String) {
        val promptText = inputText.trim()
        _messages.value += Message(promptText, true)

        var replyText = ""
        val replyMessage = Message("", isFromMe = false)
        _messages.value += replyMessage

        viewModelScope.launch {
            try {
                _chatState.value = ChatState.BUSY
                chatRepository.sendMessage(promptText)
                    .onCompletion {
                        _chatState.value = ChatState.IDLE
                    }
                    .collect { chunk ->
                        replyText += chunk
                        _messages.value = _messages.value.dropLast(1) +
                                replyMessage.copy(body = replyText)
                    }
            } catch (e: Exception) {
                // handle errors
            } finally {
                _chatState.value = ChatState.IDLE
            }
        }
    }
}

enum class ChatState {
    IDLE, BUSY
}
