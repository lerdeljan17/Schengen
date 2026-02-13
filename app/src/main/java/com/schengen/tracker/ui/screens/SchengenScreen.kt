package com.schengen.tracker.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.schengen.tracker.domain.PlannedTrip
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.domain.Stay
import com.schengen.tracker.ui.components.MonthCalendar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class PickerMode { ENTRY, EXIT, PLANNED_ENTRY, PLANNED_EXIT }
private enum class AppTab { MAIN, HISTORY, PLANNED, TOOLS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchengenScreen(vm: SchengenViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    val context = LocalContext.current

    var pickerMode by remember { mutableStateOf<PickerMode?>(null) }
    var pendingPlannedEntry by remember { mutableStateOf<LocalDate?>(null) }
    var pendingPlannedExit by remember { mutableStateOf<LocalDate?>(null) }
    var plannedNote by remember { mutableStateOf("") }

    var showAddProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newPassport by remember { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.MAIN.ordinal) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        vm.setLocationTracking(fine || coarse)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) vm.exportCsv(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importCsv(uri)
    }

    val pickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == AppTab.MAIN.ordinal, onClick = { selectedTab = AppTab.MAIN.ordinal }, text = { Text("Main") })
            Tab(selected = selectedTab == AppTab.HISTORY.ordinal, onClick = { selectedTab = AppTab.HISTORY.ordinal }, text = { Text("History") })
            Tab(selected = selectedTab == AppTab.PLANNED.ordinal, onClick = { selectedTab = AppTab.PLANNED.ordinal }, text = { Text("Planned") })
            Tab(selected = selectedTab == AppTab.TOOLS.ordinal, onClick = { selectedTab = AppTab.TOOLS.ordinal }, text = { Text("Tools") })
        }

        when (selectedTab) {
            AppTab.MAIN.ordinal -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        MetricsCard(
                            usedDays = state.usedDays,
                            availableDays = state.availableDays,
                            projectedUsedDaysToday = state.projectedUsedDaysToday,
                            projectedAvailableDaysToday = state.projectedAvailableDaysToday,
                            projectedDeltaDaysToday = state.projectedDeltaDaysToday,
                            simulatedUsedDaysAtPlanEnd = state.simulatedUsedDaysAtPlanEnd,
                            simulatedAvailableDaysAtPlanEnd = state.simulatedAvailableDaysAtPlanEnd,
                            simulatedDeltaDaysAtPlanEnd = state.simulatedDeltaDaysAtPlanEnd,
                            simulatedAsOfDate = state.simulatedAsOfDate?.format(formatter),
                            nextRecoveryDate = state.nextRecoveryDate?.format(formatter) ?: "No recovery in forecast",
                            overstayDate = state.firstPlannedOverstayDate?.format(formatter)
                        )
                    }
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = vm::previousMonth) { Text("Prev") }
                                    Text(
                                        state.selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${state.selectedMonth.year}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    TextButton(onClick = vm::nextMonth) { Text("Next") }
                                }
                                MonthCalendar(
                                    month = state.selectedMonth,
                                    occupiedDays = state.highlightedDays,
                                    plannedDays = state.plannedHighlightedDays
                                )
                                Text(
                                    "Green = confirmed stay days, gold = planned trip days",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.padding(bottom = 20.dp)) }
                }
            }
            AppTab.HISTORY.ordinal -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Manual stays", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { pickerMode = PickerMode.ENTRY }) { Text("Add entry") }
                                    Button(onClick = { pickerMode = PickerMode.EXIT }) { Text("Add exit") }
                                }
                                state.validationMessage?.let {
                                    Text(text = it, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Automatic detection", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Background checks auto-add entry/exit records from your location.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Location tracking", fontWeight = FontWeight.SemiBold)
                                    Switch(
                                        checked = state.locationTrackingEnabled,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                if (hasForegroundLocationPermission(context)) {
                                                    vm.setLocationTracking(true)
                                                } else {
                                                    val permissions = buildList {
                                                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                                                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                            add(Manifest.permission.POST_NOTIFICATIONS)
                                                        }
                                                    }
                                                    permissionLauncher.launch(permissions.toTypedArray())
                                                }
                                            } else {
                                                vm.setLocationTracking(false)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item { Text("Stay history", style = MaterialTheme.typography.titleMedium) }
                    items(state.stays, key = { it.id }) { stay ->
                        StayRow(stay = stay, formatter = formatter, onDelete = { vm.deleteStay(stay.id) })
                    }
                    item { Spacer(modifier = Modifier.padding(bottom = 20.dp)) }
                }
            }
            AppTab.PLANNED.ordinal -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PlannedTripsCard(
                            plannedEntry = pendingPlannedEntry,
                            plannedExit = pendingPlannedExit,
                            note = plannedNote,
                            onPickEntry = { pickerMode = PickerMode.PLANNED_ENTRY },
                            onPickExit = { pickerMode = PickerMode.PLANNED_EXIT },
                            onNoteChange = { plannedNote = it },
                            onAdd = {
                                val entry = pendingPlannedEntry
                                val exit = pendingPlannedExit
                                if (entry != null && exit != null) {
                                    vm.addPlannedTrip(entry, exit, plannedNote)
                                    pendingPlannedEntry = null
                                    pendingPlannedExit = null
                                    plannedNote = ""
                                }
                            }
                        )
                    }
                    item { Text("Planned trips", style = MaterialTheme.typography.titleMedium) }
                    items(state.plannedTrips, key = { it.id }) { trip ->
                        PlannedTripRow(trip = trip, formatter = formatter, onDelete = { vm.deletePlannedTrip(trip.id) })
                    }
                    item { Spacer(modifier = Modifier.padding(bottom = 20.dp)) }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DataToolsCard(
                            message = state.importExportMessage,
                            onExport = {
                                val date = LocalDate.now()
                                exportLauncher.launch("schengen-data-${date}.csv")
                            },
                            onImport = { importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv")) }
                        )
                    }
                    item {
                        ProfileCard(
                            profiles = state.profiles,
                            activeProfileId = state.activeProfileId,
                            onSelect = vm::selectProfile,
                            onAddProfile = { showAddProfileDialog = true }
                        )
                    }
                    item { Spacer(modifier = Modifier.padding(bottom = 20.dp)) }
                }
            }
        }
    }

    if (pickerMode != null) {
        DatePickerDialog(
            onDismissRequest = { pickerMode = null },
            confirmButton = {
                TextButton(onClick = {
                    val picked = pickerState.selectedDateMillis?.toLocalDate() ?: LocalDate.now()
                    when (pickerMode) {
                        PickerMode.ENTRY -> vm.addManualEntry(picked)
                        PickerMode.EXIT -> vm.addManualExit(picked)
                        PickerMode.PLANNED_ENTRY -> pendingPlannedEntry = picked
                        PickerMode.PLANNED_EXIT -> pendingPlannedExit = picked
                        null -> Unit
                    }
                    pickerMode = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { pickerMode = null }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showAddProfileDialog) {
        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.addProfile(newProfileName, newPassport)
                    newProfileName = ""
                    newPassport = ""
                    showAddProfileDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfileDialog = false }) { Text("Cancel") }
            },
            title = { Text("Add Passport Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPassport,
                        onValueChange = { newPassport = it },
                        label = { Text("Passport number") },
                        singleLine = true
                    )
                }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profiles: List<Profile>,
    activeProfileId: Long?,
    onSelect: (Long) -> Unit,
    onAddProfile: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Passport profiles", style = MaterialTheme.typography.titleMedium)
            if (profiles.isEmpty()) {
                Text("No profiles yet")
            } else {
                profiles.forEach { profile ->
                    val selected = profile.id == activeProfileId
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${profile.name} ${if (profile.passportNumber.isNotBlank()) "(${profile.passportNumber})" else ""}")
                        TextButton(onClick = { onSelect(profile.id) }) {
                            Text(if (selected) "Active" else "Use")
                        }
                    }
                }
            }
            Button(onClick = onAddProfile) { Text("Add profile") }
        }
    }
}

@Composable
private fun MetricsCard(
    usedDays: Int,
    availableDays: Int,
    projectedUsedDaysToday: Int?,
    projectedAvailableDaysToday: Int?,
    projectedDeltaDaysToday: Int?,
    simulatedUsedDaysAtPlanEnd: Int?,
    simulatedAvailableDaysAtPlanEnd: Int?,
    simulatedDeltaDaysAtPlanEnd: Int?,
    simulatedAsOfDate: String?,
    nextRecoveryDate: String,
    overstayDate: String?
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("90/180 status", style = MaterialTheme.typography.titleMedium)
            Text("Days used in current 180-day window: $usedDays")
            Text("Days available now: $availableDays", fontWeight = FontWeight.SemiBold)
            if (projectedAvailableDaysToday != null && projectedUsedDaysToday != null) {
                HorizontalDivider()
                Text("With planned trips included (today projection):", fontWeight = FontWeight.SemiBold)
                Text("Used days: $projectedUsedDaysToday")
                Text("Days left: $projectedAvailableDaysToday")
                projectedDeltaDaysToday?.let {
                    Text(
                        "Impact vs confirmed-only: ${formatDelta(it)}",
                        color = if (it <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (
                simulatedAvailableDaysAtPlanEnd != null &&
                simulatedUsedDaysAtPlanEnd != null &&
                simulatedAsOfDate != null
            ) {
                Text("At latest planned exit ($simulatedAsOfDate):", fontWeight = FontWeight.SemiBold)
                Text("Used days: $simulatedUsedDaysAtPlanEnd")
                Text("Days left: $simulatedAvailableDaysAtPlanEnd")
                simulatedDeltaDaysAtPlanEnd?.let {
                    Text(
                        "Impact at plan end: ${formatDelta(it)}",
                        color = if (it <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
            HorizontalDivider()
            Text("First next date more days are available: $nextRecoveryDate")
            Text(
                if (overstayDate == null) "Planned trips fit within current simulation."
                else "Planned trips first exceed the limit on: $overstayDate",
                color = if (overstayDate == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DataToolsCard(message: String?, onExport: () -> Unit, onImport: () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Data tools (local CSV)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onExport) { Text("Export CSV") }
                Button(onClick = onImport) { Text("Import CSV") }
            }
            message?.let { Text(it) }
        }
    }
}

@Composable
private fun PlannedTripsCard(
    plannedEntry: LocalDate?,
    plannedExit: LocalDate?,
    note: String,
    onPickEntry: () -> Unit,
    onPickExit: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Planned trips simulation", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPickEntry) {
                    Text(
                        plannedEntry?.let { "Entry ${it.format(formatter)}" } ?: "Pick entry"
                    )
                }
                Button(onClick = onPickExit) {
                    Text(
                        plannedExit?.let { "Exit ${it.format(formatter)}" } ?: "Pick exit"
                    )
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onAdd, enabled = plannedEntry != null && plannedExit != null) {
                Text("Add planned trip")
            }
        }
    }
}

@Composable
private fun StayRow(stay: Stay, formatter: DateTimeFormatter, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Entry: ${stay.entryDate.format(formatter)}")
            Text("Exit: ${stay.exitDate?.format(formatter) ?: "Open"}")
            Text("Source: ${stay.source.name}")
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
private fun PlannedTripRow(trip: PlannedTrip, formatter: DateTimeFormatter, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Entry: ${trip.entryDate.format(formatter)}")
            Text("Exit: ${trip.exitDate.format(formatter)}")
            if (trip.note.isNotBlank()) Text("Note: ${trip.note}")
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun formatDelta(value: Int): String {
    return when {
        value > 0 -> "+$value days"
        value < 0 -> "$value days"
        else -> "0 days"
    }
}

private fun hasForegroundLocationPermission(context: Context): Boolean {
    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return hasFine || hasCoarse
}
