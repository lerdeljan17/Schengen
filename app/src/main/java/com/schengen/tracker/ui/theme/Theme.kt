package com.schengen.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = SlateBlue,
    secondary = SoftGold,
    tertiary = Mint
)

private val DarkScheme = darkColorScheme(
    primary = SoftGold,
    secondary = SlateBlue,
    tertiary = Mint
)

@Composable
fun SchengenTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography,
        content = content
    )
}
