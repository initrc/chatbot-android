package io.github.initrc.chatbot.ui.chat

import android.Manifest
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.initrc.chatbot.data.SpeechAudioRecorder
import io.github.initrc.chatbot.data.SpeechToTextRemoteDataSource
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val RECORDING_START_ERROR_MESSAGE = "Unable to start recording"
private const val RECORDING_STOP_ERROR_MESSAGE = "Unable to finish recording"
private const val TRANSCRIPTION_ERROR_MESSAGE = "Unable to transcribe audio"

@HiltViewModel
class SpeechToTextViewModel @Inject constructor(
    private val audioRecorder: SpeechAudioRecorder,
    private val speechToTextRemoteDataSource: SpeechToTextRemoteDataSource,
) : ViewModel() {
    private val _speechState = MutableStateFlow<SpeechToTextState>(SpeechToTextState.Idle)
    val speechState = _speechState.asStateFlow()

    private val _speechResults = MutableSharedFlow<SpeechResult>()
    val speechResults = _speechResults.asSharedFlow()

    private var recordingStartedAtMillis = 0L
    private var recordingStateJob: Job? = null
    private var transcriptionJob: Job? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        when (_speechState.value) {
            SpeechToTextState.Idle,
            is SpeechToTextState.Error -> Unit
            is SpeechToTextState.Recording,
            SpeechToTextState.Transcribing -> return
        }
        check(!audioRecorder.isRecording) { "Recorder is active while speech state is not recording." }

        try {
            audioRecorder.start()
            recordingStartedAtMillis = SystemClock.elapsedRealtime()
            _speechState.value = SpeechToTextState.Recording(
                amplitudes = audioRecorder.amplitudes.value,
                durationMillis = 0L,
            )
            observeRecordingState()
        } catch (_: Exception) {
            audioRecorder.cancel()
            stopRecordingStateUpdates()
            _speechState.value = SpeechToTextState.Error(RECORDING_START_ERROR_MESSAGE)
        }
    }

    fun stopAndTranscribe(autoSend: Boolean) {
        if (_speechState.value !is SpeechToTextState.Recording) {
            return
        }

        stopRecordingStateUpdates()
        val audioFile = try {
            audioRecorder.stop()
        } catch (_: Exception) {
            audioRecorder.cancel()
            _speechState.value = SpeechToTextState.Error(RECORDING_STOP_ERROR_MESSAGE)
            return
        }

        _speechState.value = SpeechToTextState.Transcribing
        transcriptionJob?.cancel()
        transcriptionJob = viewModelScope.launch {
            transcribe(audioFile, autoSend)
        }
    }

    fun cancelRecording() {
        if (_speechState.value !is SpeechToTextState.Recording) {
            return
        }
        stopRecordingStateUpdates()
        audioRecorder.cancel()
        _speechState.value = SpeechToTextState.Idle
    }

    fun clearError() {
        if (_speechState.value is SpeechToTextState.Error) {
            _speechState.value = SpeechToTextState.Idle
        }
    }

    override fun onCleared() {
        stopRecordingStateUpdates()
        transcriptionJob?.cancel()
        audioRecorder.close()
        super.onCleared()
    }

    private fun observeRecordingState() {
        recordingStateJob?.cancel()
        recordingStateJob = viewModelScope.launch {
            audioRecorder.amplitudes.collect { amplitudes ->
                _speechState.value = SpeechToTextState.Recording(
                    amplitudes = amplitudes,
                    durationMillis = SystemClock.elapsedRealtime() - recordingStartedAtMillis,
                )
            }
        }
    }

    private suspend fun transcribe(audioFile: File, autoSend: Boolean) {
        try {
            val text = speechToTextRemoteDataSource.transcribe(audioFile)
            if (text.isNotBlank()) {
                _speechResults.emit(SpeechResult(text = text, autoSend = autoSend))
            }
            _speechState.value = SpeechToTextState.Idle
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _speechState.value = SpeechToTextState.Error(TRANSCRIPTION_ERROR_MESSAGE)
        } finally {
            audioFile.delete()
        }
    }

    private fun stopRecordingStateUpdates() {
        recordingStateJob?.cancel()
        recordingStateJob = null
    }
}

sealed interface SpeechToTextState {
    data object Idle : SpeechToTextState

    data class Recording(
        val amplitudes: List<Float>,
        val durationMillis: Long,
    ) : SpeechToTextState

    data object Transcribing : SpeechToTextState

    data class Error(
        val message: String,
    ) : SpeechToTextState
}

data class SpeechResult(
    val text: String,
    val autoSend: Boolean,
)
