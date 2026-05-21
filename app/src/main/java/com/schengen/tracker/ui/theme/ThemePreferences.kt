package com.schengen.tracker.ui.theme

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromKey(key: String?): AppearanceMode = when (key) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }

    val key: String
        get() = name.lowercase()
}

data class ThemeState(
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val colorThemeId: String = ColorThemes.default.id,
    val startWeekOnSunday: Boolean = false
)

class ThemePreferences(private val prefs: SharedPreferences) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<ThemeState> = _state.asStateFlow()

    val current: ThemeState
        get() = _state.value

    fun setAppearanceMode(mode: AppearanceMode) {
        prefs.edit().putString(KEY_APPEARANCE_MODE, mode.key).apply()
        _state.value = _state.value.copy(appearanceMode = mode)
    }

    fun setColorTheme(id: String) {
        prefs.edit().putString(KEY_COLOR_THEME, id).apply()
        _state.value = _state.value.copy(colorThemeId = id)
    }

    fun setStartWeekOnSunday(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_START_WEEK_ON_SUNDAY, enabled).apply()
        _state.value = _state.value.copy(startWeekOnSunday = enabled)
    }

    private fun load(): ThemeState = ThemeState(
        appearanceMode = AppearanceMode.fromKey(prefs.getString(KEY_APPEARANCE_MODE, null)),
        colorThemeId = prefs.getString(KEY_COLOR_THEME, ColorThemes.default.id)
            ?: ColorThemes.default.id,
        startWeekOnSunday = prefs.getBoolean(KEY_START_WEEK_ON_SUNDAY, false)
    )

    companion object {
        private const val KEY_APPEARANCE_MODE = "theme_appearance_mode"
        private const val KEY_COLOR_THEME = "theme_color_id"
        private const val KEY_START_WEEK_ON_SUNDAY = "preference_start_week_on_sunday"
    }
}
