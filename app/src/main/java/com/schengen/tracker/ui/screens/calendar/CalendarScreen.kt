package com.schengen.tracker.ui.screens.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.ui.AppViewModel
import com.schengen.tracker.ui.components.AppTopBar
import com.schengen.tracker.ui.components.DayDetailsDialog
import com.schengen.tracker.ui.components.MonthBlock
import com.schengen.tracker.ui.components.TripDialog
import com.schengen.tracker.ui.components.YearCalendarView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private enum class CalendarViewMode { Month, Year }

@Composable
fun CalendarScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState by viewModel.themeState.collectAsState()
    val trips = uiState.trips
    val today = uiState.today

    val calculator = viewModel.calculator

    val months = remember(today) {
        val current = YearMonth.from(today)
        (-PAST_MONTHS..FUTURE_MONTHS).map { offset -> current.plusMonths(offset.toLong()) }
    }
    val initialIndex = PAST_MONTHS

    val availableCache = remember(trips) { mutableMapOf<LocalDate, Int>() }
    val availableProvider: (LocalDate) -> Int = { date ->
        availableCache.getOrPut(date) {
            calculator.availableDaysOn(date, trips)
        }
    }

    var viewMode by remember { mutableStateOf(CalendarViewMode.Month) }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }
    var dayDetailsDate by remember { mutableStateOf<LocalDate?>(null) }
    var yearViewInitialPage by remember { mutableIntStateOf(initialIndex) }
    var yearScrollTrigger by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        listState.scrollToItem(initialIndex)
    }

    fun scrollToMonth(month: YearMonth) {
        val index = months.indexOf(month)
        if (index >= 0) {
            coroutineScope.launch { listState.animateScrollToItem(index) }
        }
    }

    fun handleDayClick(date: LocalDate) {
        val tripForDay = trips.firstOrNull { trip ->
            val end = trip.exitDate ?: today
            !date.isBefore(trip.entryDate) && !date.isAfter(end)
        }
        if (tripForDay != null) {
            editingTrip = tripForDay
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        AppTopBar(
            title = "Calendar",
            actions = {
                IconButton(onClick = {
                    if (viewMode == CalendarViewMode.Month) {
                        coroutineScope.launch { listState.animateScrollToItem(initialIndex) }
                    } else {
                        yearViewInitialPage = initialIndex
                        yearScrollTrigger++
                    }
                }) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Today",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = {
                    if (viewMode == CalendarViewMode.Month) {
                        yearViewInitialPage = listState.firstVisibleItemIndex
                        viewMode = CalendarViewMode.Year
                    } else {
                        viewMode = CalendarViewMode.Month
                    }
                }) {
                    Icon(
                        imageVector = if (viewMode == CalendarViewMode.Month) {
                            Icons.Outlined.ZoomOut
                        } else {
                            Icons.Outlined.ZoomIn
                        },
                        contentDescription = if (viewMode == CalendarViewMode.Month) {
                            "Year view"
                        } else {
                            "Month view"
                        },
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

        when (viewMode) {
            CalendarViewMode.Month -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(months, key = { _, month -> month.toString() }) { _, month ->
                        MonthBlock(
                            month = month,
                            trips = trips,
                            today = today,
                            startWeekOnSunday = themeState.startWeekOnSunday,
                            onDayClick = ::handleDayClick,
                            onDayLongPress = { dayDetailsDate = it },
                            availableDaysProvider = availableProvider
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            CalendarViewMode.Year -> {
                YearCalendarView(
                    months = months,
                    trips = trips,
                    today = today,
                    startWeekOnSunday = themeState.startWeekOnSunday,
                    onDayClick = ::handleDayClick,
                    onDayLongPress = { dayDetailsDate = it },
                    onMonthClick = { month ->
                        viewMode = CalendarViewMode.Month
                        scrollToMonth(month)
                    },
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp
                    ),
                    initialPageIndex = yearViewInitialPage,
                    scrollTrigger = yearScrollTrigger,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    dayDetailsDate?.let { date ->
        DayDetailsDialog(
            date = date,
            trips = trips,
            calculator = calculator,
            today = today,
            onDismiss = { dayDetailsDate = null }
        )
    }

    editingTrip?.let { trip ->
        TripDialog(
            existingTrip = trip,
            onDismiss = { editingTrip = null },
            onSave = { entryDate, exitDate, source, note, countries ->
                viewModel.updateTrip(trip.id, entryDate, exitDate, source, note, countries)
                editingTrip = null
            },
            onDelete = {
                viewModel.deleteTrip(trip.id)
                editingTrip = null
            }
        )
    }
}

private const val PAST_MONTHS = 6
private const val FUTURE_MONTHS = 18
