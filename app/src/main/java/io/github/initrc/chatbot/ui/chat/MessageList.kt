package io.github.initrc.chatbot.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.initrc.chatbot.data.ChatRole
import io.github.initrc.chatbot.data.Message
import io.github.initrc.chatbot.ui.theme.ChatbotTheme

@Composable
fun MessageList(
    messages: List<Message>,
    modifier: Modifier,
    bottomContentPadding: Dp = 0.dp,
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
            items = messages,
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
                        },
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

private fun Message.isFromMe(): Boolean = role == ChatRole.USER

@Preview(
    name = "MessageList (light theme)",
    showBackground = true,
)
@Preview(
    name = "MessageList (dark theme)",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
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
                        """.trimIndent(),
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
                        """.trimIndent(),
                    ),
                    Message(
                        role = ChatRole.USER,
                        content = "Thanks. Also make **user markdown** render inside my bubble.",
                    ),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
