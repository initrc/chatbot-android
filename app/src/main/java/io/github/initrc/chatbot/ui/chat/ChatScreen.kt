package io.github.initrc.chatbot.ui.chat

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.initrc.chatbot.data.db.ConversationSummary
import io.github.initrc.chatbot.ui.settings.ApiSettingsBottomSheet
import io.github.initrc.chatbot.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

private const val TAG = "ChatScreen"

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = hiltViewModel(),
    conversationViewModel: ConversationViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    speechToTextViewModel: SpeechToTextViewModel = hiltViewModel(),
    modifier: Modifier
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val chatState by chatViewModel.chatState.collectAsStateWithLifecycle()
    val conversationId by chatViewModel.conversationId.collectAsStateWithLifecycle()
    val speechState by speechToTextViewModel.speechState.collectAsStateWithLifecycle()
    val recentConversations by conversationViewModel.recentConversations.collectAsStateWithLifecycle()
    val currentModel by settingsViewModel.currentModel.collectAsStateWithLifecycle()
    val allModels by settingsViewModel.allModels.collectAsStateWithLifecycle()
    val apiKey by settingsViewModel.apiKey.collectAsStateWithLifecycle()
    val baseUrl by settingsViewModel.baseUrl.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingConversationDeletionIds = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun hideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    var showApiSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var draftText by rememberSaveable { mutableStateOf("") }
    val openApiSettingsSheet = { showApiSettingsSheet = true }
    val visibleConversations = recentConversations.filterNot { conversation ->
        pendingConversationDeletionIds.contains(conversation.id)
    }
    val latestCurrentModel by rememberUpdatedState(currentModel)
    val latestChatState by rememberUpdatedState(chatState)
    val requestSpeechRecording = rememberSpeechRecordingPermissionRequest(
        canStartRecording = {
            chatState == ChatState.IDLE &&
                speechState !is SpeechToTextState.Recording &&
                speechState !is SpeechToTextState.Transcribing
        },
        onPermissionGranted = speechToTextViewModel::startRecording,
        onPermissionDenied = {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission denied")
            }
        },
    )

    LaunchedEffect(speechToTextViewModel) {
        speechToTextViewModel.speechResults.collect { speechResult ->
            val transcript = speechResult.text
            draftText = transcript
            if (
                speechResult.autoSend &&
                transcript.isNotBlank() &&
                latestChatState == ChatState.IDLE
            ) {
                chatViewModel.onSendClick(transcript, latestCurrentModel)
                draftText = ""
            }
        }
    }

    LaunchedEffect(speechState) {
        val errorState = speechState as? SpeechToTextState.Error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(errorState.message)
        speechToTextViewModel.clearError()
    }

    DisposableEffect(lifecycleOwner, speechToTextViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                speechToTextViewModel.cancelRecording()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speechToTextViewModel.cancelRecording()
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ConversationDrawerLayout(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            ConversationDrawerSheet(
                recentConversations = visibleConversations,
                selectedConversationId = conversationId,
                onNewChatClick = {
                    speechToTextViewModel.cancelRecording()
                    chatViewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onApiSettingsClick = {
                    speechToTextViewModel.cancelRecording()
                    scope.launch {
                        drawerState.close()
                        openApiSettingsSheet()
                    }
                },
                onConversationClick = { selectedConversationId ->
                    speechToTextViewModel.cancelRecording()
                    chatViewModel.loadConversation(selectedConversationId)
                    scope.launch { drawerState.close() }
                },
                onConversationDeleteClick = { conversation ->
                    scope.launch {
                        handleConversationDeleteClick(
                            conversation = conversation,
                            chatViewModel = chatViewModel,
                            conversationViewModel = conversationViewModel,
                            drawerState = drawerState,
                            snackbarHostState = snackbarHostState,
                            pendingConversationDeletionIds = pendingConversationDeletionIds,
                        )
                    }
                },
                canDeleteConversation = { candidateConversationId ->
                    chatState == ChatState.IDLE || candidateConversationId != conversationId
                },
            )
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            ChatScreenContent(
                messages = messages,
                chatState = chatState,
                draftText = draftText,
                onDraftTextChange = { draftText = it },
                speechState = speechState,
                onConversationListClick = {
                    hideKeyboard()
                    scope.launch { drawerState.open() }
                },
                onSendClick = chatViewModel::onSendClick,
                onMicClick = requestSpeechRecording,
                onStopSpeechClick = {
                    speechToTextViewModel.stopAndTranscribe(autoSend = false)
                },
                onSendSpeechClick = {
                    speechToTextViewModel.stopAndTranscribe(autoSend = true)
                },
                currentModel = currentModel,
                allModels = allModels,
                onModelSelect = settingsViewModel::setCurrentModel,
                apiKey = apiKey,
                baseUrl = baseUrl,
                onApiSettingsClick = openApiSettingsSheet,
                modifier = Modifier.fillMaxSize(),
            )
            if (showApiSettingsSheet) {
                ApiSettingsBottomSheet(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    onApiSettingsChange = settingsViewModel::setApiSettings,
                    onDismissRequest = { showApiSettingsSheet = false },
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }
}

private suspend fun handleConversationDeleteClick(
    conversation: ConversationSummary,
    chatViewModel: ChatViewModel,
    conversationViewModel: ConversationViewModel,
    drawerState: DrawerState,
    snackbarHostState: SnackbarHostState,
    pendingConversationDeletionIds: MutableList<String>,
) {
    val wasSelected = conversation.id == chatViewModel.conversationId.value
    pendingConversationDeletionIds.add(conversation.id)
    try {
        if (wasSelected) {
            chatViewModel.startNewChat()
        }
        drawerState.close()

        val snackbarResult = snackbarHostState.showSnackbar(
            message = "Conversation deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Long,
        )

        if (snackbarResult == SnackbarResult.ActionPerformed) {
            if (wasSelected && chatViewModel.conversationId.value == null) {
                chatViewModel.loadConversation(conversation.id)
            }
            return
        }

        val deleted = conversationViewModel.deleteConversation(conversation.id)
        if (!deleted && wasSelected && chatViewModel.conversationId.value == null) {
            Log.w(
                TAG,
                "Conversation delete affected 0 rows for id=${conversation.id}; restoring selection",
            )
            chatViewModel.loadConversation(conversation.id)
        } else if (chatViewModel.conversationId.value == conversation.id) {
            chatViewModel.startNewChat()
        }
    } finally {
        pendingConversationDeletionIds.remove(conversation.id)
    }
}
