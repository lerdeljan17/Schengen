package com.schengen.tracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.domain.TripStatus
import com.schengen.tracker.ui.AppViewModel
import com.schengen.tracker.ui.components.AppTopBar
import com.schengen.tracker.ui.components.CountryFlagAvatar
import com.schengen.tracker.ui.screens.TextHelpers
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val calculator = viewModel.calculator
    val today = uiState.today
    val trips = uiState.trips

    var targetDate by remember { mutableStateOf<LocalDate?>(null) }
    var showTargetPicker by remember { mutableStateOf(false) }

    val availableNow = calculator.availableDaysOnConfirmed(today, trips, today)
    val usedNow = 90 - availableNow
    val latestPlannedExit = calculator.latestPlannedExit(today, trips)
    val availableAfterPlanned = latestPlannedExit?.let { calculator.availableDaysOn(it, trips) }
    val nextRecovery = calculator.nextDateWithMoreAvailability(today, trips)
    val overstay = calculator.firstOverstayDate(today, trips)

    val highlightedTrip = trips
        .filter { it.classify(today) != TripStatus.PAST }
        .minByOrNull { it.entryDate }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        AppTopBar(title = "Home")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AvailableDaysHero(
                    availableDays = availableNow,
                    afterPlannedDays = availableAfterPlanned,
                    afterPlannedDate = latestPlannedExit,
                    usedDays = usedNow
                )
            }
            if (overstay != null) {
                item {
                    OverstayWarningCard(overstayDate = overstay)
                }
            }
            item {
                NextRecoveryCard(date = nextRecovery)
            }
            if (highlightedTrip != null) {
                item {
                    NextTripCard(trip = highlightedTrip, today = today)
                }
            }
            item {
                TargetDateCard(
                    date = targetDate,
                    confirmedAvailable = targetDate?.let {
                        calculator.availableDaysOnConfirmed(it, trips, today)
                    },
                    withPlannedAvailable = targetDate?.let {
                        calculator.availableDaysOn(it, trips)
                    },
                    showWithPlanned = latestPlannedExit != null,
                    onPick = { showTargetPicker = true },
                    onClear = { targetDate = null }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showTargetPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (targetDate ?: today).atStartOfDayUtcMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showTargetPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { targetDate = it.toLocalDate() }
                    showTargetPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTargetPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun AvailableDaysHero(
    availableDays: Int,
    afterPlannedDays: Int?,
    afterPlannedDate: LocalDate?,
    usedDays: Int
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Days available",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = availableDays.toString(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 88.sp
            )
            Text(
                text = "of 90 in last 180 days",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Used",
                    value = "$usedDays days"
                )
                if (afterPlannedDays != null && afterPlannedDate != null) {
                    StatChip(
                        modifier = Modifier.weight(1f),
                        label = "After planned trips",
                        value = "$afterPlannedDays left"
                    )
                } else {
                    StatChip(
                        modifier = Modifier.weight(1f),
                        label = "Window",
                        value = "180 days"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(modifier: Modifier = Modifier, label: String, value: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OverstayWarningCard(overstayDate: LocalDate) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Planned overstay",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Your trips first exceed the 90/180 limit on ${overstayDate.format(TextHelpers.shortDateFormatter)}.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun NextRecoveryCard(date: LocalDate?) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Next day with more days", style = MaterialTheme.typography.titleMedium)
                Text(
                    date?.format(TextHelpers.shortDateFormatter) ?: "No recovery in forecast",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NextTripCard(trip: Trip, today: LocalDate) {
    val status = trip.classify(today)
    val isOngoing = status == TripStatus.ONGOING
    val sectionLabel = if (isOngoing) "Current trip" else "Next trip"
    val sideText = when {
        isOngoing -> {
            val end = trip.exitDate
            if (end != null) {
                val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, end)
                when {
                    daysLeft <= 0L -> "Ends today"
                    daysLeft == 1L -> "1d left"
                    else -> "${daysLeft}d left"
                }
            } else "Ongoing"
        }
        else -> {
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, trip.entryDate)
            when {
                daysUntil == 0L -> "Today"
                daysUntil == 1L -> "in 1d"
                else -> "in ${daysUntil}d"
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CountryFlagAvatar(countryCodes = trip.countries)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sectionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    TextHelpers.tripLabel(trip),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${trip.entryDate.format(TextHelpers.shortDateFormatter)}" +
                        (trip.exitDate?.let { " – ${it.format(TextHelpers.shortDateFormatter)}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    sideText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TargetDateCard(
    date: LocalDate?,
    confirmedAvailable: Int?,
    withPlannedAvailable: Int?,
    showWithPlanned: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(start = 8.dp))
                Text("Check a specific date", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "See how many days you can stay starting any future date.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPick, modifier = Modifier.weight(1f)) {
                    Text(date?.format(TextHelpers.shortDateFormatter) ?: "Pick date")
                }
                if (date != null) {
                    OutlinedButton(onClick = onClear) { Text("Clear") }
                }
            }
            if (date != null && confirmedAvailable != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "$confirmedAvailable days available on ${date.format(TextHelpers.shortDateFormatter)}",
                            fontWeight = FontWeight.SemiBold
                        )
                        if (showWithPlanned && withPlannedAvailable != null) {
                            Text(
                                "With your future trips: $withPlannedAvailable days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LocalDate.atStartOfDayUtcMillis(): Long =
    atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate()
