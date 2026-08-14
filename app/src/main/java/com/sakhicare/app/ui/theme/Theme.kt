package com.sakhicare.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = AccentIndigo,
    onSecondary = Color.White,
    background = BackgroundSoft,
    surface = SurfaceWhite,
    onBackground = Neutral900,
    onSurface = Neutral900,
    outline = Neutral200,
    surfaceVariant = Neutral50,
)

@Composable
fun SakhiCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
