package io.github.initrc.chatbot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.initrc.chatbot.data.ChatRepository
import io.github.initrc.chatbot.data.db.ConversationSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the conversation summary state used by the thread drawer UI.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val _recentConversations = MutableStateFlow(emptyList<ConversationSummary>())
    val recentConversations = _recentConversations.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.observeRecentConversations().collect { conversations ->
                _recentConversations.value = conversations
            }
        }
    }

    /**
     * Returns the latest known summary for a conversation shown in the drawer list.
     */
    fun conversationSummary(conversationId: String): ConversationSummary? {
        return _recentConversations.value.firstOrNull { conversation ->
            conversation.id == conversationId
        }
    }
}
