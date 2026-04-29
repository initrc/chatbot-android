package io.github.initrc.chatbot.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal fun rememberSpeechRecordingPermissionRequest(
    canStartRecording: () -> Boolean,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val requestAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            if (canStartRecording()) {
                onPermissionGranted()
            }
        } else {
            onPermissionDenied()
        }
    }

    return {
        if (canStartRecording()) {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onPermissionGranted()
            } else {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}
