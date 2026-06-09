package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldMint,
    secondary = PinkAccent,
    tertiary = CyanAccent,
    background = SlateBlack,
    surface = CardBg,
    onPrimary = SlateBlack,
    onSecondary = SlateBlack,
    onTertiary = SlateBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor,
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Social Sentry uses an elegant dark mode to match focus branding
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
