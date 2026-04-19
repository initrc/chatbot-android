package io.github.initrc.chatbot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatContextCompressorTest {

    private val compressor = ChatContextCompressor()

    @Test
    fun keepsAllMessagesWhenTheyFit() {
        val messages = listOf(
            Message(role = ChatRole.USER, content = "How should I structure the app?"),
            Message(role = ChatRole.ASSISTANT, content = "Use a small repository layer."),
            Message(role = ChatRole.USER, content = "What about tests?"),
        )

        val compressed = compressor.compress(messages, contextWindow = 8192)

        assertEquals(messages, compressed)
    }

    @Test
    fun compactsOlderMessagesAndKeepsLatestMessage() {
        val messages = listOf(
            Message(
                role = ChatRole.USER,
                content = "Project goal: build a Groq-backed Android chat client."
            )
        ) + (1..30).flatMap { index ->
            listOf(
                Message(
                    role = ChatRole.USER,
                    content = "Question $index " + "x".repeat(800)
                ),
                Message(
                    role = ChatRole.ASSISTANT,
                    content = "Answer $index " + "y".repeat(800)
                )
            )
        } + Message(
            role = ChatRole.USER,
            content = "Given all that, what should I do next?"
        )

        val compressed = compressor.compress(messages, contextWindow = 8192)

        assertTrue(compressed.size < messages.size)
        assertEquals(ChatRole.SYSTEM, compressed.first().role)
        assertTrue(compressed.first().content.contains("compacted"))
        assertEquals("Given all that, what should I do next?", compressed.last().content)
    }

    @Test
    fun truncatesLatestMessageWhenItExceedsBudgetByItself() {
        val oversizedPrompt = "a".repeat(5000)

        val compressed = compressor.compress(
            messages = listOf(Message(role = ChatRole.USER, content = oversizedPrompt)),
            contextWindow = 600
        )

        assertEquals(1, compressed.size)
        assertTrue(compressed.last().content.contains("[truncated]"))
        assertTrue(compressed.last().content.length < oversizedPrompt.length)
    }

    @Test
    fun responseReserveUsesQuarterOfContextWindowCappedAtMaximumCompletionTokens() {
        assertEquals(2048, compressor.maxCompletionTokens(contextWindow = 8192))
        assertEquals(4096, compressor.maxCompletionTokens(contextWindow = 32768))
    }
}
