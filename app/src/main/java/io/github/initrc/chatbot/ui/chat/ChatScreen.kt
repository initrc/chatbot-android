package io.github.initrc.chatbot.ui.chat

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import io.github.initrc.chatbot.ui.common.CircleIconButton
import io.github.initrc.chatbot.ui.settings.SettingsViewModel
import io.github.initrc.chatbot.ui.theme.ChatbotTheme
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val chatState by chatViewModel.chatState.collectAsStateWithLifecycle()
    val currentModel by settingsViewModel.currentModel.collectAsStateWithLifecycle()
    val allModels by settingsViewModel.allModels.collectAsStateWithLifecycle()
    ChatScreen(messages, chatState, chatViewModel::onSendClick, currentModel, allModels, settingsViewModel::setCurrentModel, modifier)
}

@Composable
private fun ChatScreen(
    messages: List<Message>,
    chatState: ChatState,
    onSendClick: (String, String) -> Unit,
    currentModel: String,
    allModels: List<String>,
    onModelSelect: (String) -> Unit,
    modifier: Modifier
) {
    var sendViewHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxSize()) {
        ModelHeader(
            currentModel = currentModel,
            allModels = allModels,
            onModelSelect = onModelSelect,
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
    currentModel: String,
    allModels: List<String>,
    onModelSelect: (String) -> Unit,
    modifier: Modifier,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val sortedModels = remember(allModels) { allModels.sorted() }

    Row(
        modifier = modifier
            .clickable { showBottomSheet = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$currentModel ▾",
            style = MaterialTheme.typography.titleMedium,
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
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
                                    showBottomSheet = false
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        state = listState,
    ) {
        items(
            items = messages
        ) { message ->
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            .clip(RoundedCornerShape(28.dp))
            .fillMaxWidth(0.8f)
            .heightIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .wrapContentHeight(align = Alignment.CenterVertically)
    } else {
        Modifier
    }
    Text(
        text = message.content,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
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
    onSendClick: (String, String) -> Unit,
    isEnabled: Boolean,
    model: String,
    modifier: Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

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
        CircleIconButton(
            isEnabled = isEnabled,
            onClick = {
                focusManager.clearFocus()
                onSendClick(text, model)
                text = ""
            },
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp)
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
                onSendClick = { _: String, _: String -> },
                currentModel = "llama-3.1-8b-instant",
                allModels = listOf("llama-3.1-8b-instant"),
                onModelSelect = {},
                modifier = Modifier,
            )
        }
    }
}