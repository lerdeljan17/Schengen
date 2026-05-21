package com.schengen.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.ui.theme.DayNumberStyle
import com.schengen.tracker.ui.theme.DaySubNumberStyle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthBlock(
    month: YearMonth,
    trips: List<Trip>,
    today: LocalDate,
    startWeekOnSunday: Boolean,
    onDayClick: (LocalDate) -> Unit,
    availableDaysProvider: (LocalDate) -> Int,
    modifier: Modifier = Modifier
) {
    val weekDays = if (startWeekOnSunday) sundayFirst else mondayFirst
    val firstWeekday = weekDays.first()

    val tripDays: Set<LocalDate> = remember(month, trips, today) {
        val out = mutableSetOf<LocalDate>()
        val rangeStart = month.atDay(1).minusDays(7)
        val rangeEnd = month.atEndOfMonth().plusDays(7)
        trips.forEach { trip ->
            val effectiveEnd = trip.exitDate ?: today
            var cursor = maxOf(trip.entryDate, rangeStart)
            val finalEnd = minOf(effectiveEnd, rangeEnd)
            while (!cursor.isAfter(finalEnd)) {
                out.add(cursor)
                cursor = cursor.plusDays(1)
            }
        }
        out
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${month.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp, top = 4.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        val cells = remember(month, firstWeekday) { buildDayCells(month, firstWeekday) }
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 1.dp)
            ) {
                week.forEachIndexed { index, date ->
                    if (date == null) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val inTrip = date in tripDays
                        val prev = if (index > 0) week[index - 1] else null
                        val next = if (index < week.lastIndex) week[index + 1] else null
                        val leftConnects = inTrip && prev != null && prev in tripDays
                        val rightConnects = inTrip && next != null && next in tripDays
                        val topRadius = when {
                            !inTrip -> 14.dp
                            !leftConnects -> 16.dp
                            else -> 0.dp
                        }
                        val rightRadius = when {
                            !inTrip -> 14.dp
                            !rightConnects -> 16.dp
                            else -> 0.dp
                        }
                        DayCell(
                            modifier = Modifier.weight(1f),
                            date = date,
                            isToday = date == today,
                            isInTrip = inTrip,
                            leftRadius = topRadius,
                            rightRadius = rightRadius,
                            leftEdgePadding = if (leftConnects) 0.dp else 2.dp,
                            rightEdgePadding = if (rightConnects) 0.dp else 2.dp,
                            onClick = { onDayClick(date) },
                            availableDays = availableDaysProvider(date)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    modifier: Modifier,
    date: LocalDate,
    isToday: Boolean,
    isInTrip: Boolean,
    leftRadius: Dp,
    rightRadius: Dp,
    leftEdgePadding: Dp,
    rightEdgePadding: Dp,
    onClick: () -> Unit,
    availableDays: Int
) {
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val numberColor = MaterialTheme.colorScheme.onSurface
    val subColor = MaterialTheme.colorScheme.primary

    val shape = if (isInTrip) RoundedCornerShape(
        topStart = leftRadius,
        bottomStart = leftRadius,
        topEnd = rightRadius,
        bottomEnd = rightRadius
    ) else RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = leftEdgePadding, end = rightEdgePadding, top = 2.dp, bottom = 2.dp)
            .background(
                color = if (isInTrip) highlightColor else androidx.compose.ui.graphics.Color.Transparent,
                shape = shape
            )
            .then(
                if (isToday) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = shape
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = numberColor,
                style = DayNumberStyle
            )
            Text(
                text = availableDays.toString(),
                color = subColor,
                style = DaySubNumberStyle
            )
        }
    }
}

private val mondayFirst = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

private val sundayFirst = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
)

private fun buildDayCells(month: YearMonth, firstWeekday: DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val leadingEmpty = (first.dayOfWeek.value - firstWeekday.value + 7) % 7
    val totalDays = month.lengthOfMonth()

    val cells = mutableListOf<LocalDate?>()
    repeat(leadingEmpty) { cells.add(null) }
    for (day in 1..totalDays) {
        cells.add(month.atDay(day))
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}
