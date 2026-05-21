package com.schengen.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.schengen.tracker.SchengenApp
import androidx.compose.ui.platform.LocalContext

@Composable
fun SchengenTrackerTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SchengenApp
    val themeState by app.themePreferences.state.collectAsState()

    val useDark = when (themeState.appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    }
    val spec = ColorThemes.byId(themeState.colorThemeId)

    MaterialTheme(
        colorScheme = if (useDark) spec.dark else spec.light,
        typography = Typography,
        content = content
    )
}
