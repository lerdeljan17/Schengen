package com.schengen.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schengen.tracker.ui.theme.UnlockBlue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthCalendar(
    month: YearMonth,
    occupiedDays: Set<LocalDate>,
    plannedDays: Set<LocalDate> = emptySet(),
    unlockedDays: Set<LocalDate> = emptySet(),
    todayDate: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier
) {
    val weekDays = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    val days = buildDayCells(month)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            items(days) { date ->
                if (date == null) {
                    Box(modifier = Modifier.size(36.dp))
                } else {
                    val occupied = date in occupiedDays
                    val planned = date in plannedDays
                    val unlocked = date in unlockedDays
                    val isToday = date == todayDate
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                when {
                                    unlocked -> UnlockBlue.copy(alpha = 0.55f)
                                    occupied -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
                                    planned -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(
                                width = if (isToday) 2.dp else 1.dp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (occupied || planned || unlocked) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private fun buildDayCells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val leadingEmpty = first.dayOfWeek.value - 1
    val totalDays = month.lengthOfMonth()

    val cells = mutableListOf<LocalDate?>()
    repeat(leadingEmpty) { cells.add(null) }
    for (day in 1..totalDays) {
        cells.add(month.atDay(day))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }
    return cells
}
