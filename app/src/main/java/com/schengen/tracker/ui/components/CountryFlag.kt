package com.schengen.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schengen.tracker.location.SchengenCountryCatalog

/**
 * Convert a two-letter ISO 3166-1 alpha-2 country code (e.g. "HR")
 * into a regional-indicator flag emoji (e.g. the Croatia flag).
 */
fun countryCodeToFlagEmoji(code: String): String {
    val normalized = SchengenCountryCatalog.normalizeCode(code)
        ?: code.trim().takeIf { it.length == 2 }?.uppercase()
        ?: return "🗺"
    if (normalized.length != 2) return "🗺"
    val first = normalized[0].code - 'A'.code + 0x1F1E6
    val second = normalized[1].code - 'A'.code + 0x1F1E6
    if (first !in 0x1F1E6..0x1F1FF || second !in 0x1F1E6..0x1F1FF) return "🗺"
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

@Composable
fun CountryFlagAvatar(
    countryCodes: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val primary = countryCodes.firstOrNull()
    val emoji = if (primary != null) countryCodeToFlagEmoji(primary) else "🗺"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = TextStyle(fontSize = 24.sp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
