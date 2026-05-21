package com.schengen.tracker.ui.screens.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.domain.TripStatus
import com.schengen.tracker.ui.AppViewModel
import com.schengen.tracker.ui.components.AppTopBar
import com.schengen.tracker.ui.components.TripCard
import com.schengen.tracker.ui.components.TripDialog

@Composable
fun TripsScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val calculator = viewModel.calculator
    val today = uiState.today
    val trips = uiState.trips

    var addingTrip by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }

    val sortedTrips = remember(trips) {
        trips.sortedByDescending { it.entryDate }
    }
    val ongoing = sortedTrips
        .filter { it.classify(today) == TripStatus.ONGOING }
        .sortedBy { it.entryDate }
    val upcoming = sortedTrips
        .filter { it.classify(today) == TripStatus.UPCOMING }
        .sortedBy { it.entryDate }
    val past = sortedTrips
        .filter { it.classify(today) == TripStatus.PAST }
        .sortedByDescending { it.exitDate ?: it.entryDate }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Trips")
            if (trips.isEmpty()) {
                EmptyTripsState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = contentPadding.calculateBottomPadding())
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = contentPadding.calculateBottomPadding() + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (ongoing.isNotEmpty()) {
                        item { SectionHeader("Ongoing") }
                        items(ongoing, key = { it.id }) { trip ->
                            TripCard(
                                trip = trip,
                                status = calculator.statusForTrip(trip, trips, today),
                                onClick = { editingTrip = trip }
                            )
                        }
                    }
                    if (upcoming.isNotEmpty()) {
                        item { SectionHeader("Upcoming") }
                        items(upcoming, key = { it.id }) { trip ->
                            TripCard(
                                trip = trip,
                                status = calculator.statusForTrip(trip, trips, today),
                                onClick = { editingTrip = trip }
                            )
                        }
                    }
                    if (past.isNotEmpty()) {
                        item { SectionHeader("Past") }
                        items(past, key = { it.id }) { trip ->
                            TripCard(
                                trip = trip,
                                status = calculator.statusForTrip(trip, trips, today),
                                onClick = { editingTrip = trip }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { addingTrip = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add trip") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 20.dp)
        )
    }

    if (addingTrip) {
        TripDialog(
            existingTrip = null,
            onDismiss = { addingTrip = false },
            onSave = { entryDate, exitDate, source, note, countries ->
                viewModel.addTrip(entryDate, exitDate, source, note, countries)
                addingTrip = false
            }
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

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun EmptyTripsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("No trips yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap \"Add trip\" to record your first stay.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
