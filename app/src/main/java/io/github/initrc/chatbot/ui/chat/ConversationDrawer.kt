package io.github.initrc.chatbot.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.initrc.chatbot.R
import io.github.initrc.chatbot.data.db.ConversationSummary
import io.github.initrc.chatbot.ui.theme.ChatbotTheme

@Composable
fun ConversationDrawerLayout(
    recentConversations: List<ConversationSummary>,
    selectedConversationId: String?,
    drawerState: DrawerState,
    onNewChatClick: () -> Unit,
    onConversationClick: (String) -> Unit,
    onConversationDeleteClick: (ConversationSummary) -> Unit,
    canDeleteConversation: (String) -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            ConversationDrawerSheet(
                recentConversations = recentConversations,
                selectedConversationId = selectedConversationId,
                onNewChatClick = onNewChatClick,
                onConversationClick = onConversationClick,
                onConversationDeleteClick = onConversationDeleteClick,
                canDeleteConversation = canDeleteConversation,
            )
        },
        content = content,
    )
}

@Composable
fun ConversationDrawerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.menu_24),
            contentDescription = "Recent conversations",
        )
    }
}

@Composable
private fun ConversationDrawerSheet(
    recentConversations: List<ConversationSummary>,
    selectedConversationId: String?,
    onNewChatClick: () -> Unit,
    onConversationClick: (String) -> Unit,
    onConversationDeleteClick: (ConversationSummary) -> Unit,
    canDeleteConversation: (String) -> Boolean,
) {
    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 360.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Text(
                        text = "Recent conversations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                if (recentConversations.isEmpty()) {
                    item {
                        Text(
                            text = "No saved conversations yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                } else {
                    items(
                        items = recentConversations,
                        key = { conversation -> conversation.id },
                    ) { conversation ->
                        ConversationDrawerItem(
                            conversation = conversation,
                            selected = conversation.id == selectedConversationId,
                            onClick = { onConversationClick(conversation.id) },
                            onDeleteClick = { onConversationDeleteClick(conversation) },
                            canDelete = canDeleteConversation(conversation.id),
                        )
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = onNewChatClick,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit_square_24),
                        contentDescription = null,
                    )
                },
                text = {
                    Text("Chat")
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .height(56.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationDrawerItem(
    conversation: ConversationSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    canDelete: Boolean,
) {
    var showMenu by remember(conversation.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            color = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.Transparent
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { showMenu = true },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                ConversationDrawerItemLabel(conversation)
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                enabled = canDelete,
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
            )
        }
    }
}

@Composable
private fun ConversationDrawerItemLabel(
    conversation: ConversationSummary,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = conversation.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        conversation.preview?.takeIf { preview -> preview.isNotBlank() }?.let { preview ->
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(
    name = "ConversationDrawer (light theme)",
    showBackground = true,
)
@Preview(
    name = "ConversationDrawer (dark theme)",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun ConversationDrawerPreview() {
    ChatbotTheme {
        Surface {
            ConversationDrawerLayout(
                recentConversations = listOf(
                    ConversationSummary(
                        id = "1",
                        title = "New chat",
                        preview = "What are the best options for streaming updates?",
                        updatedAtMillis = 0L,
                        messageCount = 4,
                    ),
                    ConversationSummary(
                        id = "2",
                        title = "Room schema",
                        preview = "Is the composite index enough here?",
                        updatedAtMillis = 0L,
                        messageCount = 6,
                    ),
                ),
                selectedConversationId = "2",
                drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
                onNewChatClick = {},
                onConversationClick = {},
                onConversationDeleteClick = {},
                canDeleteConversation = { true },
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {}
            }
        }
    }
}
