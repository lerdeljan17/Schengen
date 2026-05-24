package com.schengen.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schengen.tracker.domain.Trip
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun YearCalendarView(
    months: List<YearMonth>,
    trips: List<Trip>,
    today: LocalDate,
    startWeekOnSunday: Boolean,
    onDayClick: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    onMonthClick: (YearMonth) -> Unit,
    contentPadding: PaddingValues,
    initialPageIndex: Int,
    scrollTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val pages = remember(months) { months.chunked(12) }
    val listState = rememberLazyListState()

    LaunchedEffect(initialPageIndex, scrollTrigger) {
        val pageIndex = initialPageIndex / 12
        if (pageIndex in pages.indices) {
            listState.scrollToItem(pageIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(pages.size, key = { pages[it].first().toString() }) { pageIndex ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(pages[pageIndex], key = { it.toString() }) { month ->
                    MiniMonthBlock(
                        month = month,
                        trips = trips,
                        today = today,
                        startWeekOnSunday = startWeekOnSunday,
                        onDayClick = onDayClick,
                        onDayLongPress = onDayLongPress,
                        onMonthClick = { onMonthClick(month) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniMonthBlock(
    month: YearMonth,
    trips: List<Trip>,
    today: LocalDate,
    startWeekOnSunday: Boolean,
    onDayClick: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    onMonthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDays = if (startWeekOnSunday) sundayFirstWeekdays else mondayFirstWeekdays
    val firstWeekday = weekDays.first()

    val tripDays = remember(month, trips, today) {
        val rangeStart = month.atDay(1).minusDays(7)
        val rangeEnd = month.atEndOfMonth().plusDays(7)
        tripDaysInRange(trips, today, rangeStart, rangeEnd)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onMonthClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${month.year}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .height(22.dp)
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
                            MiniDayCell(
                                modifier = Modifier.weight(1f),
                                date = date,
                                isToday = date == today,
                                isInTrip = inTrip,
                                leftRadius = if (!inTrip || leftConnects) 0.dp else 4.dp,
                                rightRadius = if (!inTrip || rightConnects) 0.dp else 4.dp,
                                leftEdgePadding = if (leftConnects) 0.dp else 1.dp,
                                rightEdgePadding = if (rightConnects) 0.dp else 1.dp,
                                onClick = { onDayClick(date) },
                                onLongClick = { onDayLongPress(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiniDayCell(
    modifier: Modifier,
    date: LocalDate,
    isToday: Boolean,
    isInTrip: Boolean,
    leftRadius: Dp,
    rightRadius: Dp,
    leftEdgePadding: Dp,
    rightEdgePadding: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val shape = if (isInTrip) {
        RoundedCornerShape(
            topStart = leftRadius,
            bottomStart = leftRadius,
            topEnd = rightRadius,
            bottomEnd = rightRadius
        )
    } else {
        RoundedCornerShape(4.dp)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = leftEdgePadding, end = rightEdgePadding)
            .background(
                color = if (isInTrip) highlightColor else androidx.compose.ui.graphics.Color.Transparent,
                shape = shape
            )
            .then(
                if (isToday) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
