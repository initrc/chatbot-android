package io.github.initrc.chatbot.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.initrc.chatbot.R
import io.github.initrc.chatbot.data.Message
import io.github.initrc.chatbot.ui.theme.ChatbotTheme

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    modifier: Modifier
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    ChatScreen(messages, chatViewModel::onSendClick, modifier)
}
@Composable
private fun ChatScreen(
    messages: List<Message>,
    onSendClick: (String) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(all = 8.dp)) {
        MessageList(
            messages = messages,
            modifier = Modifier.fillMaxSize()
        )
        SendView(
            onSendClick = onSendClick,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
fun MessageList(
    messages: List<Message>,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 120.dp)
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
                    Message("Text from me", true),
                    Message("Text from bot", false)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SendView(
    onSendClick: (String) -> Unit,
    modifier: Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth()
        )
        IconButton(
            onClick = {
                onSendClick(text)
                text = ""
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 8.dp, end = 8.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                painter = painterResource(R.drawable.send_24),
                contentDescription = "Send",
            )
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
            ChatScreen(
                messages = listOf(
                    Message("Text from me", true),
                ) + List(30) { index ->
                    Message("Text $index from bot", false)
                },
                onSendClick = {},
                modifier = Modifier,
            )
        }
    }
}