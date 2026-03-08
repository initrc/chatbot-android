package io.github.initrc.chatbot.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.initrc.chatbot.R
import io.github.initrc.chatbot.data.Message
import io.github.initrc.chatbot.data.ChatRole
import io.github.initrc.chatbot.ui.common.CircleIconButton
import io.github.initrc.chatbot.ui.theme.ChatbotTheme

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = hiltViewModel(),
    modifier: Modifier
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val chatState by chatViewModel.chatState.collectAsStateWithLifecycle()
    ChatScreen(messages, chatState, chatViewModel::onSendClick, modifier)
}

@Composable
private fun ChatScreen(
    messages: List<Message>,
    chatState: ChatState,
    onSendClick: (String) -> Unit,
    modifier: Modifier
) {
    var sendViewHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(all = 8.dp)
    ) {
        MessageList(
            messages = messages,
            modifier = Modifier.fillMaxSize(),
            bottomContentPadding = sendViewHeight + 8.dp
        )
        SendView(
            onSendClick = onSendClick,
            isEnabled = chatState == ChatState.IDLE,
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

@Composable
fun MessageList(
    messages: List<Message>,
    modifier: Modifier,
    bottomContentPadding: Dp = 0.dp
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        items(
            items = messages
        ) { message ->
            MessageView(message)
        }
        item { Spacer(modifier = Modifier.height(bottomContentPadding)) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }
}

@Composable
fun MessageView(message: Message) {
    val isFromMe = message.isFromMe()
    val modifier = if (isFromMe) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    } else {
        Modifier.padding(8.dp)
    }
    Text(
        text = message.content,
        modifier = modifier,
        style = if (isFromMe) {
            MaterialTheme.typography.bodyMedium
        } else {
            MaterialTheme.typography.bodyLarge

        },
    )
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
                    Message(role = ChatRole.USER, content = "Text from me"),
                    Message(role = ChatRole.ASSISTANT, content = "Text from bot")
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SendView(
    onSendClick: (String) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
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
        CircleIconButton(
            isEnabled = isEnabled,
            onClick = {
                focusManager.clearFocus()
                onSendClick(text)
                text = ""
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 2.dp, end = 2.dp),
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
                    Message(role = ChatRole.USER, content = "Text from me"),
                ) + List(30) { index ->
                    Message(role = ChatRole.ASSISTANT, content = "Text $index from bot")
                },
                chatState = ChatState.IDLE,
                onSendClick = {},
                modifier = Modifier,
            )
        }
    }
}