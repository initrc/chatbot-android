package io.github.initrc.chatbot.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.initrc.chatbot.R
import io.github.initrc.chatbot.ui.common.CircleIconButton

@Composable
fun SendView(
    onSendClick: (String, String) -> Unit,
    isEnabled: Boolean,
    model: String,
    modifier: Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val canSend = isEnabled && text.isNotEmpty()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
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
        if (canSend) {
            CircleIconButton(
                onClick = {
                    focusManager.clearFocus()
                    onSendClick(text, model)
                    text = ""
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
    }
}
