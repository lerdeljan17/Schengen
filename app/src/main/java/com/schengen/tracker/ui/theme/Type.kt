package com.schengen.tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val baseTypography = Typography()

val Typography: Typography = baseTypography.copy(
    displayLarge = baseTypography.displayLarge.copy(
        fontWeight = FontWeight.SemiBold
    ),
    headlineLarge = baseTypography.headlineLarge.copy(
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = baseTypography.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = baseTypography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = baseTypography.titleLarge.copy(
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp
    ),
    titleMedium = baseTypography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold
    ),
    labelLarge = baseTypography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = baseTypography.bodyLarge.copy(
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
)

val DayNumberStyle: TextStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 20.sp
)

val DaySubNumberStyle: TextStyle = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 13.sp
)
