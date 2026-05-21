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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.ui.AppViewModel
import com.schengen.tracker.ui.components.AppTopBar
import com.schengen.tracker.ui.components.MonthBlock
import com.schengen.tracker.ui.components.TripDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

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

    var editingTrip by remember { mutableStateOf<Trip?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        listState.scrollToItem(initialIndex)
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
                    coroutineScope.launch { listState.animateScrollToItem(initialIndex) }
                }) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Today",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

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
            items(months, key = { it.toString() }) { month ->
                MonthBlock(
                    month = month,
                    trips = trips,
                    today = today,
                    startWeekOnSunday = themeState.startWeekOnSunday,
                    onDayClick = { date ->
                        val tripForDay = trips.firstOrNull { trip ->
                            val end = trip.exitDate ?: today
                            !date.isBefore(trip.entryDate) && !date.isAfter(end)
                        }
                        if (tripForDay != null) {
                            editingTrip = tripForDay
                        }
                    },
                    availableDaysProvider = availableProvider
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
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
