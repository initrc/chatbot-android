package io.github.initrc.chatbot.data

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.Closeable
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val AUDIO_RECORDINGS_DIR_NAME = "speech-recordings"
private const val AUDIO_FILE_PREFIX = "speech-"
private const val AUDIO_FILE_SUFFIX = ".m4a"
private const val AMPLITUDE_POLL_INTERVAL_MILLIS = 50L
private const val WAVEFORM_BAR_INTERVAL_MILLIS = 200L
private const val WAVEFORM_BAR_COUNT = 40
private const val MAX_MEDIA_RECORDER_AMPLITUDE = 32_767f
private const val AUDIO_ENCODING_BIT_RATE = 128_000
private const val AUDIO_SAMPLING_RATE = 44_100
private const val AMPLITUDE_SAMPLES_PER_BAR =
    (WAVEFORM_BAR_INTERVAL_MILLIS / AMPLITUDE_POLL_INTERVAL_MILLIS).toInt()

class SpeechAudioRecorder @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val emptyAmplitudeBars = List(WAVEFORM_BAR_COUNT) { 0f }
    private val _amplitudes = MutableStateFlow(emptyAmplitudeBars)
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var amplitudeJob: Job? = null

    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    val isRecording: Boolean
        get() = recorder != null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        check(recorder == null) { "Audio recording is already in progress." }

        val file = createOutputFile()
        val mediaRecorder = newMediaRecorder()
        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioEncodingBitRate(AUDIO_ENCODING_BIT_RATE)
                setAudioSamplingRate(AUDIO_SAMPLING_RATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            mediaRecorder.release()
            file.delete()
            throw IOException("Failed to start audio recording.", e)
        }

        outputFile = file
        recorder = mediaRecorder
        startAmplitudePolling(mediaRecorder)
    }

    fun stop(): File {
        val mediaRecorder = checkNotNull(recorder) { "No audio recording is in progress." }
        val file = checkNotNull(outputFile) { "No audio output file is available." }

        stopAmplitudePolling()
        try {
            mediaRecorder.stop()
            return file
        } catch (e: RuntimeException) {
            file.delete()
            throw IOException("Failed to stop audio recording.", e)
        } finally {
            releaseCurrentRecorder()
        }
    }

    fun cancel() {
        stopAmplitudePolling()
        outputFile?.delete()
        releaseCurrentRecorder()
    }

    override fun close() {
        cancel()
        scope.cancel()
    }

    private fun startAmplitudePolling(mediaRecorder: MediaRecorder) {
        check(recorder === mediaRecorder) { "Amplitude polling requires an active recorder." }
        _amplitudes.value = emptyAmplitudeBars
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            val pendingSamples = mutableListOf<Float>()
            while (isActive) {
                pendingSamples += mediaRecorder.normalizedAmplitude()
                if (pendingSamples.size == AMPLITUDE_SAMPLES_PER_BAR) {
                    val barAmplitude = pendingSamples.average().toFloat()
                    _amplitudes.value = _amplitudes.value
                        .drop(1) + barAmplitude
                    pendingSamples.clear()
                }
                delay(AMPLITUDE_POLL_INTERVAL_MILLIS)
            }
        }
    }

    private fun stopAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _amplitudes.value = emptyAmplitudeBars
    }

    private fun releaseCurrentRecorder() {
        recorder?.release()
        recorder = null
        outputFile = null
    }

    private fun createOutputFile(): File {
        val recordingsDir = File(context.cacheDir, AUDIO_RECORDINGS_DIR_NAME)
        if (!recordingsDir.exists() && !recordingsDir.mkdirs()) {
            throw IOException("Failed to create audio recordings cache directory.")
        }
        return File.createTempFile(AUDIO_FILE_PREFIX, AUDIO_FILE_SUFFIX, recordingsDir)
    }

    private fun newMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}

private fun MediaRecorder.normalizedAmplitude(): Float {
    return try {
        (maxAmplitude / MAX_MEDIA_RECORDER_AMPLITUDE).coerceIn(0f, 1f)
    } catch (_: IllegalStateException) {
        0f
    }
}
