package io.github.initrc.chatbot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD97757),
    onPrimary = Color(0xFFFAF9F5),
    primaryContainer = Color(0xFFD97757),
    onPrimaryContainer = Color(0xFFFAF9F5),
    secondaryContainer = Color(0xFF141413),
    onSecondaryContainer = Color(0xFFDDDDD0),
    background = Color(0xFF262624),
    onBackground = Color(0xFFDDDDD0),
    surface = Color(0xFF262624),
    onSurface = Color(0xFFDDDDD0),
    surfaceVariant = Color(0xFF161614),
    onSurfaceVariant = Color(0xFFADADA0),
    surfaceContainer = Color(0xFF30302E),
    surfaceContainerLow = Color(0xFF262624),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD97757),
    onPrimary = Color(0xFFFAF9F5),
    primaryContainer = Color(0xFFD97757),
    onPrimaryContainer = Color(0xFFFAF9F5),
    secondaryContainer = Color(0xFFF0EEE6),
    onSecondaryContainer = Color(0xFF000000),
    background = Color(0xFFFAF9F5),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFAF9F5),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFEAE9E5),
    onSurfaceVariant = Color(0xFF303030),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF9F5),
)

@Composable
fun ChatbotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
