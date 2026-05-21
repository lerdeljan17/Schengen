package com.schengen.tracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class ColorThemeSpec(
    val id: String,
    val displayName: String,
    val seed: Color,
    val light: ColorScheme,
    val dark: ColorScheme
)

private fun darkSchemeFor(primary: Color, primaryContainer: Color, secondary: Color): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = Color.White,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.25f).compositeOver(SchengenSurface),
        onSecondaryContainer = SchengenOnSurface,
        tertiary = secondary,
        onTertiary = Color.White,
        background = SchengenInk,
        onBackground = SchengenOnSurface,
        surface = SchengenInkElevated,
        onSurface = SchengenOnSurface,
        surfaceVariant = SchengenSurfaceVariant,
        onSurfaceVariant = SchengenOnSurfaceMuted,
        surfaceTint = primary,
        outline = SchengenOutline,
        outlineVariant = SchengenOutline.copy(alpha = 0.6f),
        error = Color(0xFFFF6B6B),
        onError = Color.White,
        errorContainer = StatusErrorContainer,
        onErrorContainer = StatusErrorText
    )

private fun lightSchemeFor(primary: Color, primaryContainer: Color, secondary: Color): ColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = Color(0xFF0A1F3A),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.18f).compositeOver(SchengenLightSurface),
        onSecondaryContainer = SchengenLightOnSurface,
        tertiary = secondary,
        onTertiary = Color.White,
        background = SchengenLightSurfaceVariant,
        onBackground = SchengenLightOnSurface,
        surface = SchengenLightSurface,
        onSurface = SchengenLightOnSurface,
        surfaceVariant = SchengenLightSurfaceVariant,
        onSurfaceVariant = SchengenLightOnSurfaceMuted,
        surfaceTint = primary,
        outline = SchengenLightOutline,
        outlineVariant = SchengenLightOutline.copy(alpha = 0.7f),
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = StatusErrorContainerLight,
        onErrorContainer = StatusErrorTextLight
    )

private fun Color.compositeOver(background: Color): Color {
    val alpha = this.alpha
    val invAlpha = 1f - alpha
    return Color(
        red = this.red * alpha + background.red * invAlpha,
        green = this.green * alpha + background.green * invAlpha,
        blue = this.blue * alpha + background.blue * invAlpha,
        alpha = 1f
    )
}

object ColorThemes {
    val Blue = ColorThemeSpec(
        id = "blue",
        displayName = "Blue",
        seed = Color(0xFF3B82F6),
        dark = darkSchemeFor(
            primary = Color(0xFF3B82F6),
            primaryContainer = Color(0xFF2E5BB0).copy(alpha = 0.55f).compositeOver(SchengenSurface),
            secondary = Color(0xFF60A5FA)
        ),
        light = lightSchemeFor(
            primary = Color(0xFF1E66E0),
            primaryContainer = Color(0xFFDCE7FF),
            secondary = Color(0xFF1F4FB8)
        )
    )

    val Green = ColorThemeSpec(
        id = "green",
        displayName = "Green",
        seed = Color(0xFF22C55E),
        dark = darkSchemeFor(
            primary = Color(0xFF22C55E),
            primaryContainer = Color(0xFF1F6A3D).copy(alpha = 0.55f).compositeOver(SchengenSurface),
            secondary = Color(0xFF4ADE80)
        ),
        light = lightSchemeFor(
            primary = Color(0xFF158E45),
            primaryContainer = Color(0xFFCDEFD8),
            secondary = Color(0xFF11703A)
        )
    )

    val Purple = ColorThemeSpec(
        id = "purple",
        displayName = "Purple",
        seed = Color(0xFFA855F7),
        dark = darkSchemeFor(
            primary = Color(0xFFA855F7),
            primaryContainer = Color(0xFF6B3F9F).copy(alpha = 0.55f).compositeOver(SchengenSurface),
            secondary = Color(0xFFC084FC)
        ),
        light = lightSchemeFor(
            primary = Color(0xFF7E3AD1),
            primaryContainer = Color(0xFFEDDCFF),
            secondary = Color(0xFF6028AE)
        )
    )

    val Orange = ColorThemeSpec(
        id = "orange",
        displayName = "Orange",
        seed = Color(0xFFF97316),
        dark = darkSchemeFor(
            primary = Color(0xFFF97316),
            primaryContainer = Color(0xFFB04C0E).copy(alpha = 0.55f).compositeOver(SchengenSurface),
            secondary = Color(0xFFFB923C)
        ),
        light = lightSchemeFor(
            primary = Color(0xFFC85603),
            primaryContainer = Color(0xFFFFE0CC),
            secondary = Color(0xFF9F4602)
        )
    )

    val Pink = ColorThemeSpec(
        id = "pink",
        displayName = "Pink",
        seed = Color(0xFFEC4899),
        dark = darkSchemeFor(
            primary = Color(0xFFEC4899),
            primaryContainer = Color(0xFFA8366E).copy(alpha = 0.55f).compositeOver(SchengenSurface),
            secondary = Color(0xFFF472B6)
        ),
        light = lightSchemeFor(
            primary = Color(0xFFC2317C),
            primaryContainer = Color(0xFFFFD9EB),
            secondary = Color(0xFF9C2562)
        )
    )

    val Teal = ColorThemeSpec(
        id = "teal",
        displayName = "Teal",
        seed = Color(0xFF14B8A6),
        dark = darkSchemeFor(
            primary = Color(0xFF14B8A6),
            primaryContainer = Color(0xFF0F7F73).copy(alpha = 0.55f).compositeOver(SchengenSurface),
            secondary = Color(0xFF2DD4BF)
        ),
        light = lightSchemeFor(
            primary = Color(0xFF0C8B7F),
            primaryContainer = Color(0xFFC6EFEA),
            secondary = Color(0xFF0A6E64)
        )
    )

    val Slate = ColorThemeSpec(
        id = "slate",
        displayName = "Slate",
        seed = Color(0xFF64748B),
        dark = darkSchemeFor(
            primary = Color(0xFF94A3B8),
            primaryContainer = Color(0xFF455066).copy(alpha = 0.55f).compositeOver(SchengenSurface),
            secondary = Color(0xFFB4BECC)
        ),
        light = lightSchemeFor(
            primary = Color(0xFF45526B),
            primaryContainer = Color(0xFFD6DCE8),
            secondary = Color(0xFF334155)
        )
    )

    val Red = ColorThemeSpec(
        id = "red",
        displayName = "Red",
        seed = Color(0xFFEF4444),
        dark = darkSchemeFor(
            primary = Color(0xFFEF4444),
            primaryContainer = Color(0xFFA62B2B).copy(alpha = 0.55f).compositeOver(SchengenSurface),
            secondary = Color(0xFFF87171)
        ),
        light = lightSchemeFor(
            primary = Color(0xFFC02323),
            primaryContainer = Color(0xFFFFDADA),
            secondary = Color(0xFF9A1818)
        )
    )

    val all: List<ColorThemeSpec> = listOf(Blue, Green, Purple, Orange, Pink, Teal, Slate, Red)
    val default: ColorThemeSpec = Blue

    fun byId(id: String?): ColorThemeSpec = all.firstOrNull { it.id == id } ?: default
}
