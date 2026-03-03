package io.github.initrc.chatbot.ui.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.initrc.chatbot.data.ChatRepository
import io.github.initrc.chatbot.data.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.collections.listOf

class ChatViewModel(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val _messages = MutableStateFlow(listOf<Message>())
    val messages: StateFlow<List<Message>> = _messages

    fun onSendClick(inputText: String) {
        val text = inputText.trim()
        _messages.value += Message(text, true)
        viewModelScope.launch {
            val reply = sendMessage(text)
            _messages.value += reply
        }

    }

    private suspend fun sendMessage(text: String): Message {
        return Message(
            body = chatRepository.sendMessage(text),
            isFromMe = false,
        )
    }
}