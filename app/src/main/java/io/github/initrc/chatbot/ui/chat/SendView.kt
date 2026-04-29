package io.github.initrc.chatbot.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.initrc.chatbot.R
import io.github.initrc.chatbot.ui.common.CircleIconButton
import io.github.initrc.chatbot.ui.theme.ChatbotTheme

@Composable
fun SendView(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: (String, String) -> Unit,
    speechState: SpeechToTextState,
    onMicClick: () -> Unit,
    onStopSpeechClick: () -> Unit,
    onSendSpeechClick: () -> Unit,
    isEnabled: Boolean,
    model: String,
    modifier: Modifier,
) {
    val focusManager = LocalFocusManager.current
    val canSend = isEnabled && text.isNotEmpty()
    val recordingState = speechState as? SpeechToTextState.Recording

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (recordingState == null) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(text = "Ask AI")
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
                maxLines = 5,
            )
            if (isEnabled) {
                CircleIconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = if (canSend) 0.dp else 4.dp),
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic_24),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        contentDescription = "Start voice input",
                    )
                }
            }
            if (canSend) {
                CircleIconButton(
                    onClick = {
                        focusManager.clearFocus()
                        onSendClick(text, model)
                        onTextChange("")
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.send_24),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = "Send",
                    )
                }
            }
        } else {
            SpeechRecordingControls(
                amplitudes = recordingState.amplitudes,
                onStopClick = onStopSpeechClick,
                onSendClick = onSendSpeechClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "SendView text", showBackground = true)
@Preview(
    name = "SendView text dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SendViewTextPreview() {
    ChatbotTheme {
        Surface {
            SendView(
                text = "Summarize this idea",
                onTextChange = {},
                onSendClick = { _, _ -> },
                speechState = SpeechToTextState.Idle,
                onMicClick = {},
                onStopSpeechClick = {},
                onSendSpeechClick = {},
                isEnabled = true,
                model = "llama-3.1-8b-instant",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "SendView recording", showBackground = true)
@Preview(
    name = "SendView recording dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SendViewRecordingPreview() {
    ChatbotTheme {
        Surface {
            SendView(
                text = "",
                onTextChange = {},
                onSendClick = { _, _ -> },
                speechState = SpeechToTextState.Recording(
                    amplitudes = List(28) { 0f } + listOf(
                        0.18f,
                        0.35f,
                        0.72f,
                        0.46f,
                        0.86f,
                        0.58f,
                        0.3f,
                        0.64f,
                        0.42f,
                        0.2f,
                        0.5f,
                        0.76f,
                    ),
                    durationMillis = 2_400L,
                ),
                onMicClick = {},
                onStopSpeechClick = {},
                onSendSpeechClick = {},
                isEnabled = true,
                model = "llama-3.1-8b-instant",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SpeechRecordingControls(
    amplitudes: List<Float>,
    onStopClick: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoiceWaveform(
            amplitudes = amplitudes,
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .padding(end = 16.dp),
        )
        CircleIconButton(
            onClick = onStopClick,
            modifier = Modifier.align(Alignment.CenterVertically),
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Icon(
                painter = painterResource(R.drawable.stop_circle_24),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                contentDescription = "Stop recording",
            )
        }
        CircleIconButton(
            onClick = onSendClick,
            modifier = Modifier.align(Alignment.CenterVertically),
        ) {
            Icon(
                painter = painterResource(R.drawable.send_24),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                contentDescription = "Send recording",
            )
        }
    }
}

@Composable
private fun VoiceWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    val gap = 3.dp

    Canvas(modifier = modifier) {
        if (amplitudes.isEmpty()) return@Canvas

        val maxGapPx = size.width / (amplitudes.size * 2f)
        val gapPx = gap.toPx().coerceAtMost(maxGapPx)
        val barWidth = ((size.width - gapPx * (amplitudes.size - 1)) / amplitudes.size)
            .coerceAtLeast(1f)
        val centerY = size.height / 2f

        amplitudes.forEachIndexed { index, amplitude ->
            val barHeight = (size.height * amplitude.coerceIn(0f, 1f))
            if (barHeight > 0f) {
                val left = index * (barWidth + gapPx)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, centerY - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
    }
}
