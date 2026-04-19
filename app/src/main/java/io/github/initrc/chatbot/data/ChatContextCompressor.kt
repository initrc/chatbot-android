package io.github.initrc.chatbot.data

import javax.inject.Inject
import kotlin.math.ceil

internal const val CHAT_SYSTEM_PROMPT = "You are a helpful assistant."

private const val DEFAULT_CONTEXT_WINDOW_TOKENS = 8192
private const val MIN_INPUT_BUDGET_TOKENS = 256
private const val MAX_COMPLETION_TOKENS = 4096
private const val RESPONSE_RESERVE_CONTEXT_DIVISOR = 4
private const val TOKEN_ESTIMATE_BUFFER_TOKENS = 512
private const val OLDER_CONTEXT_NOTE_BUDGET_DIVISOR = 2
private const val OLDER_CONTEXT_HEAD_MESSAGES = 2
private const val TARGET_OLDER_CONTEXT_EXCERPT_TOKENS = 512
private const val CHARS_PER_TOKEN = 3
private const val MESSAGE_OVERHEAD_TOKENS = 8
private const val TRUNCATION_MARKER = "\n...[truncated]...\n"

/**
 * Builds a model-sized request transcript while leaving the visible chat history untouched.
 */
class ChatContextCompressor @Inject constructor() {

    fun compress(messages: List<Message>, contextWindow: Int?): List<Message> {
        if (messages.isEmpty()) return emptyList()

        val availableTokens = messageBudgetFor(contextWindow)

        val fullBudgetMessages = packRecentMessages(messages, availableTokens)
        if (fullBudgetMessages.omittedCount == 0) {
            return fullBudgetMessages.messages
        }

        val olderContextNoteBudget =
            availableTokens / OLDER_CONTEXT_NOTE_BUDGET_DIVISOR
        val recentBudget = availableTokens - olderContextNoteBudget
        val recentWithNoteBudget = packRecentMessages(messages, recentBudget)
        val olderContextNote = buildOlderContextNote(
            omittedMessages = messages.take(recentWithNoteBudget.startIndex),
            tokenBudget = olderContextNoteBudget
        )

        return listOf(olderContextNote) + recentWithNoteBudget.messages
    }

    fun maxCompletionTokens(contextWindow: Int?): Int {
        return responseReserveFor(contextWindow?.takeIf { it > 0 } ?: DEFAULT_CONTEXT_WINDOW_TOKENS)
    }

    private fun messageBudgetFor(contextWindow: Int?): Int {
        val window = contextWindow?.takeIf { it > 0 } ?: DEFAULT_CONTEXT_WINDOW_TOKENS
        val responseReserve = responseReserveFor(window)
        val systemPromptCost = ChatTokenEstimator.estimateMessage(
            Message(role = ChatRole.SYSTEM, content = CHAT_SYSTEM_PROMPT)
        )
        return maxOf(
            MIN_INPUT_BUDGET_TOKENS,
            window - responseReserve - TOKEN_ESTIMATE_BUFFER_TOKENS - systemPromptCost
        )
    }

    private fun responseReserveFor(contextWindow: Int): Int {
        return minOf(MAX_COMPLETION_TOKENS, contextWindow / RESPONSE_RESERVE_CONTEXT_DIVISOR)
    }

    private fun packRecentMessages(
        messages: List<Message>,
        tokenBudget: Int
    ): PackedMessages {
        val selected = ArrayDeque<Message>()
        var usedTokens = 0
        var startIndex = messages.size

        for (index in messages.indices.reversed()) {
            val message = messages[index]
            val cost = ChatTokenEstimator.estimateMessage(message)
            val mustKeepLatest = index == messages.lastIndex

            if (usedTokens + cost <= tokenBudget) {
                selected.addFirst(message)
                usedTokens += cost
                startIndex = index
            } else if (mustKeepLatest && selected.isEmpty()) {
                val truncated = truncateMessageToBudget(message, tokenBudget)
                selected.addFirst(truncated)
                usedTokens += ChatTokenEstimator.estimateMessage(truncated)
                startIndex = index
            } else {
                break
            }
        }

        return PackedMessages(
            startIndex = startIndex,
            messages = selected.toList(),
        )
    }

    private fun buildOlderContextNote(
        omittedMessages: List<Message>,
        tokenBudget: Int
    ): Message {
        require(omittedMessages.isNotEmpty())
        val header = buildString {
            append("Earlier conversation was compacted to fit the model context. ")
            append("Use these excerpts as background and prefer the full recent transcript below.")
        }
        val selectedMessages = selectOlderContextMessages(omittedMessages, tokenBudget)
        val entryBudget = (tokenBudget -
                ChatTokenEstimator.estimateText(header) -
                MESSAGE_OVERHEAD_TOKENS) / selectedMessages.size

        val content = buildString {
            append(header)
            append("\n")
            selectedMessages.forEachIndexed { index, message ->
                append("\n")
                append(message.role.name.lowercase())
                append(": ")
                append(compactText(message.content, entryBudget))
                if (
                    index == OLDER_CONTEXT_HEAD_MESSAGES - 1 &&
                    selectedMessages.size < omittedMessages.size
                ) {
                    append("\n\n...older middle messages omitted from compact context...")
                }
            }
        }

        val contentBudget = tokenBudget - MESSAGE_OVERHEAD_TOKENS
        return Message(
            role = ChatRole.SYSTEM,
            content = compactText(content, contentBudget)
        )
    }

    private fun selectOlderContextMessages(
        messages: List<Message>,
        tokenBudget: Int
    ): List<Message> {
        val maxEntries = maxOf(
            OLDER_CONTEXT_HEAD_MESSAGES + 1,
            tokenBudget / TARGET_OLDER_CONTEXT_EXCERPT_TOKENS
        )
        if (messages.size <= maxEntries) return messages

        val tailCount = maxEntries - OLDER_CONTEXT_HEAD_MESSAGES
        return messages.take(OLDER_CONTEXT_HEAD_MESSAGES) +
                messages.takeLast(tailCount)
    }

    private fun truncateMessageToBudget(message: Message, tokenBudget: Int): Message {
        val contentBudget = tokenBudget - MESSAGE_OVERHEAD_TOKENS
        return message.copy(content = compactText(message.content, contentBudget))
    }

    private fun compactText(text: String, tokenBudget: Int): String {
        if (tokenBudget <= 0) return ""

        val maxChars = tokenBudget * CHARS_PER_TOKEN
        if (text.length <= maxChars) return text

        val markerLength = TRUNCATION_MARKER.length
        if (maxChars <= markerLength) {
            return text.take(maxChars)
        }

        val headLength = (maxChars - markerLength) / 2
        val tailLength = maxChars - markerLength - headLength
        return text.take(headLength) + TRUNCATION_MARKER + text.takeLast(tailLength)
    }

    private data class PackedMessages(
        val startIndex: Int,
        val messages: List<Message>,
    ) {
        val omittedCount: Int = startIndex
    }
}

internal object ChatTokenEstimator {
    fun estimateMessage(message: Message): Int {
        return MESSAGE_OVERHEAD_TOKENS + estimateText(message.content)
    }

    fun estimateText(text: String): Int {
        if (text.isEmpty()) return 1
        return ceil(text.length.toDouble() / CHARS_PER_TOKEN).toInt()
    }
}
