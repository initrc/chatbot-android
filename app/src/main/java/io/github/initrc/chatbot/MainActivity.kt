package io.github.initrc.chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import io.github.initrc.chatbot.data.ChatRepository
import io.github.initrc.chatbot.ui.chat.ChatScreen
import io.github.initrc.chatbot.ui.chat.ChatViewModel
import io.github.initrc.chatbot.ui.theme.ChatbotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatbotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val chatViewModel = ChatViewModel(ChatRepository())
                    ChatScreen(
                        chatViewModel = chatViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
