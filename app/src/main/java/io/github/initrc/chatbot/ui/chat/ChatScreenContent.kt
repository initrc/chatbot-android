package io.github.initrc.chatbot.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.initrc.chatbot.data.ChatRole
import io.github.initrc.chatbot.data.Message
import io.github.initrc.chatbot.ui.settings.ApiSettingsIconButton
import io.github.initrc.chatbot.ui.theme.ChatbotTheme

@Composable
internal fun ChatScreenContent(
    messages: List<Message>,
    chatState: ChatState,
    onConversationListClick: () -> Unit,
    onSendClick: (String, String) -> Unit,
    currentModel: String,
    allModels: List<String>,
    onModelSelect: (String) -> Unit,
    apiKey: String,
    baseUrl: String,
    onApiSettingsClick: () -> Unit,
    modifier: Modifier,
) {
    var sendViewHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val hasApiSettings = apiKey.isNotBlank() && baseUrl.isNotBlank()

    Column(modifier = modifier.fillMaxSize()) {
        ModelHeader(
            onConversationListClick = onConversationListClick,
            currentModel = currentModel,
            allModels = allModels,
            onModelSelect = onModelSelect,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            if (hasApiSettings) {
                MessageList(
                    messages = messages,
                    modifier = Modifier.fillMaxSize(),
                    bottomContentPadding = sendViewHeight + 8.dp,
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
                        },
                )
            } else {
                ApiSettingsEmptyState(
                    onApiSettingsClick = onApiSettingsClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ApiSettingsEmptyState(
    onApiSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            ApiSettingsIconButton(
                onClick = onApiSettingsClick,
                modifier = Modifier.padding(8.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Set up API to start chatting",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add a base URL and API key",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(
    name = "ChatScreen (light theme)",
    showBackground = true,
)
@Preview(
    name = "ChatScreen (dark theme)",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
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
                onApiSettingsClick = {},
                modifier = Modifier,
            )
        }
    }
}
