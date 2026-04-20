package io.github.initrc.chatbot.ui.chat

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState

@Composable
internal fun ChatMarkdown(
    content: String,
    modifier: Modifier = Modifier,
) {
    val markdownState = rememberMarkdownState(
        content = content,
        retainState = true,
    )
    val markdownComponents = rememberChatMarkdownComponents()
    SelectionContainer(modifier = modifier) {
        Markdown(
            markdownState = markdownState,
            components = markdownComponents,
            typography = chatMarkdownTypography(),
            modifier = Modifier,
        )
    }
}

@Composable
private fun rememberChatMarkdownComponents(): MarkdownComponents = remember {
    markdownComponents(
        codeBlock = {
            MarkdownHighlightedCodeBlock(
                content = it.content,
                node = it.node,
                style = it.typography.code,
                showHeader = true,
            )
        },
        codeFence = {
            MarkdownHighlightedCodeFence(
                content = it.content,
                node = it.node,
                style = it.typography.code,
                showHeader = true,
            )
        },
        checkbox = { MarkdownCheckBox(it.content, it.node, it.typography.text) },
    )
}

@Composable
private fun chatMarkdownTypography() = markdownTypography(
    h1 = MaterialTheme.typography.titleLarge,
    h2 = MaterialTheme.typography.titleMedium,
    h3 = MaterialTheme.typography.titleSmall,
    h4 = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
    h5 = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
    h6 = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
)
