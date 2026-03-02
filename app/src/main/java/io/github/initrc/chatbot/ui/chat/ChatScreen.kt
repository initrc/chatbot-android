package io.github.initrc.chatbot.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.initrc.chatbot.data.Message
import io.github.initrc.chatbot.ui.theme.ChatbotTheme

@Composable
fun ChatScreen(
    messages: List<Message>,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        MessageList(messages)
    }
}

@Composable
fun MessageList(messages: List<Message>) {
    LazyColumn(
        modifier = Modifier.padding(all = 8.dp),
    ) {
        items(
            items = messages
        ) { message ->
            MessageView(message)
        }
    }
}

@Composable
fun MessageView(message: Message) {
    val modifier = if (message.isFromMe) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    } else {
        Modifier.padding(8.dp)
    }
    Text(
        text = message.body,
        modifier = modifier,
        style = if (message.isFromMe) {
            MaterialTheme.typography.bodyMedium
        } else {
            MaterialTheme.typography.bodyLarge

        },
    )
}

@Preview(
    name = "light theme",
    showBackground = true
)
@Preview(
    name = "dark theme",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun MessageListPreview() {
    ChatbotTheme {
        Surface {
            MessageList(
                messages = listOf(
                    Message("Text from me", true),
                    Message("Text from bot", false)
                )
            )
        }
    }
}
