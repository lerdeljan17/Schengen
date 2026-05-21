package com.schengen.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schengen.tracker.domain.TripLimitStatus
import com.schengen.tracker.ui.theme.StatusErrorContainer
import com.schengen.tracker.ui.theme.StatusErrorContainerLight
import com.schengen.tracker.ui.theme.StatusErrorText
import com.schengen.tracker.ui.theme.StatusErrorTextLight
import com.schengen.tracker.ui.theme.StatusOkContainer
import com.schengen.tracker.ui.theme.StatusOkContainerLight
import com.schengen.tracker.ui.theme.StatusOkText
import com.schengen.tracker.ui.theme.StatusOkTextLight
import com.schengen.tracker.ui.theme.StatusWarnContainer
import com.schengen.tracker.ui.theme.StatusWarnContainerLight
import com.schengen.tracker.ui.theme.StatusWarnText
import com.schengen.tracker.ui.theme.StatusWarnTextLight

@Composable
fun StatusBadge(
    status: TripLimitStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = statusColorsFor(status)
    val icon = statusIconFor(status)
    val label = statusLabelFor(status, short = compact)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.container)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.padding(end = 0.dp)
        )
        Text(
            text = label,
            color = colors.content,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun statusColorsFor(status: TripLimitStatus): StatusColors {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (status) {
        TripLimitStatus.WITHIN_LIMITS -> if (dark) StatusColors(StatusOkContainer, StatusOkText)
        else StatusColors(StatusOkContainerLight, StatusOkTextLight)

        TripLimitStatus.CLOSE_TO_LIMIT -> if (dark) StatusColors(StatusWarnContainer, StatusWarnText)
        else StatusColors(StatusWarnContainerLight, StatusWarnTextLight)

        TripLimitStatus.OVER_LIMIT -> if (dark) StatusColors(StatusErrorContainer, StatusErrorText)
        else StatusColors(StatusErrorContainerLight, StatusErrorTextLight)
    }
}

private fun statusIconFor(status: TripLimitStatus): ImageVector = when (status) {
    TripLimitStatus.WITHIN_LIMITS -> Icons.Filled.CheckCircle
    TripLimitStatus.CLOSE_TO_LIMIT -> Icons.Filled.Warning
    TripLimitStatus.OVER_LIMIT -> Icons.Filled.ErrorOutline
}

@ReadOnlyComposable
private fun statusLabelFor(status: TripLimitStatus, short: Boolean): String = when (status) {
    TripLimitStatus.WITHIN_LIMITS -> if (short) "OK" else "Within limits"
    TripLimitStatus.CLOSE_TO_LIMIT -> if (short) "Close" else "Close to limit"
    TripLimitStatus.OVER_LIMIT -> if (short) "Over" else "Over limit"
}

private data class StatusColors(val container: Color, val content: Color)

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
