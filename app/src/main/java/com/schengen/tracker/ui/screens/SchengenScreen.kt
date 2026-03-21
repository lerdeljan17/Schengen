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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.schengen.tracker.data.EntrySource
import com.schengen.tracker.domain.PlannedTrip
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.domain.Stay
import com.schengen.tracker.ui.components.MonthCalendar
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class PickerMode { ENTRY, EXIT, PLANNED_ENTRY, PLANNED_EXIT, TARGET_DATE }
private enum class AppTab(val title: String) { MAIN("Main"), HISTORY("History"), PLANNED("Planned"), TOOLS("Tools") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SchengenScreen(vm: SchengenViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    val context = LocalContext.current

    var pickerMode by remember { mutableStateOf<PickerMode?>(null) }
    var pendingPlannedEntry by remember { mutableStateOf<LocalDate?>(null) }
    var pendingPlannedExit by remember { mutableStateOf<LocalDate?>(null) }
    var plannedNote by remember { mutableStateOf("") }
    var manualEntryNote by remember { mutableStateOf("") }

    var showAddProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newPassport by remember { mutableStateOf("") }
    var editingStay by remember { mutableStateOf<Stay?>(null) }
    var editingPlannedTrip by remember { mutableStateOf<PlannedTrip?>(null) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    val tabs = remember { AppTab.values() }
    val pagerState = rememberPagerState(
        initialPage = AppTab.MAIN.ordinal,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        vm.setLocationTracking(hasForegroundLocationPermission(context))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(tab.title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (tabs[page]) {
                AppTab.MAIN -> {
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
                            TargetDateCard(
                                targetDate = state.targetDate?.format(formatter),
                                availableDaysOnTargetDate = state.availableDaysOnTargetDate,
                                availableDaysOnTargetDateWithPlanned = state.availableDaysOnTargetDateWithPlanned,
                                onPickDate = { pickerMode = PickerMode.TARGET_DATE },
                                onClearDate = { vm.setTargetDate(null) }
                            )
                        }
                        item {
                            Card {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            state.selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${state.selectedMonth.year}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = vm::previousMonth) { Text("Prev") }
                                        TextButton(onClick = vm::goToCurrentMonth) { Text("Today") }
                                        TextButton(onClick = vm::nextMonth) { Text("Next") }
                                    }
                                    MonthCalendar(
                                        month = state.selectedMonth,
                                        occupiedDays = state.highlightedDays,
                                        plannedDays = state.plannedHighlightedDays,
                                        unlockedDays = state.unlockedHighlightedDays,
                                        todayDate = state.today
                                    )
                                    Text(
                                        "Green = confirmed stay days, gold = planned trip days, blue = days that unlock more availability",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    val unlockPeriods = summarizeUnlockPeriods(state.unlockedDaysByDate)
                                    val totalUnlockedDays = unlockPeriods.sumOf { it.unlockedDays }
                                    if (unlockPeriods.isEmpty()) {
                                        Text("No unlock days in this month.", style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        Text(
                                            "Total unlocked in this month: ${formatDayCount(totalUnlockedDays)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text("Breakdown:", style = MaterialTheme.typography.bodySmall)
                                        unlockPeriods.forEach { period ->
                                            Text(
                                                formatUnlockPeriod(period, formatter),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.padding(bottom = 20.dp)) }
                    }
                }
                AppTab.HISTORY -> {
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
                                    OutlinedTextField(
                                        value = manualEntryNote,
                                        onValueChange = { manualEntryNote = it },
                                        label = { Text("Note for next entry (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
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
                                        "Movement-triggered checks (geofence + location updates) auto-add entry/exit records from your location.",
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
                                                        if (
                                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                                            !hasBackgroundLocationPermission(context)
                                                        ) {
                                                            permissionLauncher.launch(
                                                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                                            )
                                                        } else {
                                                            vm.setLocationTracking(true)
                                                        }
                                                    } else {
                                                        val permissions = buildList {
                                                            add(Manifest.permission.ACCESS_FINE_LOCATION)
                                                            add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                                            }
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
                                    Button(
                                        onClick = vm::runLocationCheckNow,
                                        enabled = state.locationTrackingEnabled
                                    ) {
                                        Text("Check now")
                                    }
                                    state.locationStatusMessage?.let { message ->
                                        Text(
                                            text = message,
                                            color = if (state.locationStatusIsError) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        item { Text("Stay history", style = MaterialTheme.typography.titleMedium) }
                        items(state.stays, key = { it.id }) { stay ->
                            StayRow(
                                stay = stay,
                                formatter = formatter,
                                onClick = { editingStay = stay }
                            )
                        }
                        item { Spacer(modifier = Modifier.padding(bottom = 20.dp)) }
                    }
                }
                AppTab.PLANNED -> {
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
                            PlannedTripRow(
                                trip = trip,
                                formatter = formatter,
                                onClick = { editingPlannedTrip = trip }
                            )
                        }
                        item { Spacer(modifier = Modifier.padding(bottom = 20.dp)) }
                    }
                }
                AppTab.TOOLS -> {
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
                                onAddProfile = { showAddProfileDialog = true },
                                onEditProfile = { editingProfile = it }
                            )
                        }
                        item { Spacer(modifier = Modifier.padding(bottom = 20.dp)) }
                    }
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
                        PickerMode.ENTRY -> {
                            vm.addManualEntry(picked, manualEntryNote)
                            manualEntryNote = ""
                        }
                        PickerMode.EXIT -> vm.addManualExit(picked)
                        PickerMode.PLANNED_ENTRY -> pendingPlannedEntry = picked
                        PickerMode.PLANNED_EXIT -> pendingPlannedExit = picked
                        PickerMode.TARGET_DATE -> vm.setTargetDate(picked)
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

    editingStay?.let { stay ->
        EditStayDialog(
            stay = stay,
            onDismiss = { editingStay = null },
            onSave = { entryDate, exitDate, source, note ->
                vm.updateStay(stay.id, entryDate, exitDate, source, note)
                editingStay = null
            },
            onDelete = {
                vm.deleteStay(stay.id)
                editingStay = null
            }
        )
    }

    editingPlannedTrip?.let { trip ->
        EditPlannedTripDialog(
            trip = trip,
            onDismiss = { editingPlannedTrip = null },
            onSave = { entryDate, exitDate, note ->
                vm.updatePlannedTrip(trip.id, entryDate, exitDate, note)
                editingPlannedTrip = null
            },
            onDelete = {
                vm.deletePlannedTrip(trip.id)
                editingPlannedTrip = null
            },
            onConfirm = {
                vm.confirmPlannedTrip(trip.id)
                editingPlannedTrip = null
            }
        )
    }

    editingProfile?.let { profile ->
        EditProfileDialog(
            profile = profile,
            isActive = profile.id == state.activeProfileId,
            onDismiss = { editingProfile = null },
            onSave = { name, passportNumber ->
                vm.updateProfile(profile.id, name, passportNumber)
                editingProfile = null
            },
            onDelete = {
                vm.deleteProfile(profile.id)
                editingProfile = null
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profiles: List<Profile>,
    activeProfileId: Long?,
    onSelect: (Long) -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (Profile) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Passport profiles", style = MaterialTheme.typography.titleMedium)
            if (profiles.isEmpty()) {
                Text("No profiles yet")
            } else {
                profiles.forEach { profile ->
                    val selected = profile.id == activeProfileId
                    Card(
                        onClick = { onEditProfile(profile) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(profile.name)
                                if (profile.passportNumber.isNotBlank()) {
                                    Text(
                                        "Passport: ${profile.passportNumber}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onEditProfile(profile) }) { Text("Edit") }
                                TextButton(onClick = { onSelect(profile.id) }) {
                                    Text(if (selected) "Active" else "Use")
                                }
                            }
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
private fun TargetDateCard(
    targetDate: String?,
    availableDaysOnTargetDate: Int?,
    availableDaysOnTargetDateWithPlanned: Int?,
    onPickDate: () -> Unit,
    onClearDate: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Check days on specific date", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPickDate) {
                    Text(targetDate?.let { "Date: $it" } ?: "Pick date")
                }
                if (targetDate != null) {
                    TextButton(onClick = onClearDate) { Text("Clear") }
                }
            }
            if (targetDate != null && availableDaysOnTargetDate != null) {
                Text("Confirmed stays only: $availableDaysOnTargetDate days available")
                availableDaysOnTargetDateWithPlanned?.let { withPlanned ->
                    Text("With planned trips: $withPlanned days available")
                }
            }
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
private fun StayRow(stay: Stay, formatter: DateTimeFormatter, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Entry: ${stay.entryDate.format(formatter)}")
            Text("Exit: ${stay.exitDate?.format(formatter) ?: "Open"}")
            Text(stayDurationSummary(stay), fontWeight = FontWeight.SemiBold)
            Text("Source: ${stay.source.name}")
            if (stay.note.isNotBlank()) Text("Note: ${stay.note}")
            TextButton(onClick = onClick) { Text("Edit") }
        }
    }
}

@Composable
private fun PlannedTripRow(trip: PlannedTrip, formatter: DateTimeFormatter, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Entry: ${trip.entryDate.format(formatter)}")
            Text("Exit: ${trip.exitDate.format(formatter)}")
            plannedTripDurationSummary(trip.entryDate, trip.exitDate)?.let {
                Text(it, fontWeight = FontWeight.SemiBold)
            }
            if (trip.note.isNotBlank()) Text("Note: ${trip.note}")
            TextButton(onClick = onClick) { Text("Edit") }
        }
    }
}

@Composable
private fun EditStayDialog(
    stay: Stay,
    onDismiss: () -> Unit,
    onSave: (entryDate: LocalDate, exitDate: LocalDate?, source: EntrySource, note: String) -> Unit,
    onDelete: () -> Unit
) {
    var entryDateText by remember(stay.id) { mutableStateOf(stay.entryDate.toString()) }
    var hasExitDate by remember(stay.id) { mutableStateOf(stay.exitDate != null) }
    var exitDateText by remember(stay.id) { mutableStateOf(stay.exitDate?.toString().orEmpty()) }
    var source by remember(stay.id) { mutableStateOf(stay.source) }
    var noteText by remember(stay.id) { mutableStateOf(stay.note) }
    var errorMessage by remember(stay.id) { mutableStateOf<String?>(null) }
    val durationText = stayDurationSummary(
        entry = parseIsoDate(entryDateText),
        exit = if (hasExitDate) parseIsoDate(exitDateText) else null,
        hasExitDate = hasExitDate
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val entry = parseIsoDate(entryDateText)
                if (entry == null) {
                    errorMessage = "Entry date must use YYYY-MM-DD."
                    return@TextButton
                }
                val exit = if (!hasExitDate) null else {
                    val parsedExit = parseIsoDate(exitDateText)
                    if (parsedExit == null) {
                        errorMessage = "Exit date must use YYYY-MM-DD."
                        return@TextButton
                    }
                    if (parsedExit.isBefore(entry)) {
                        errorMessage = "Exit date cannot be before entry date."
                        return@TextButton
                    }
                    parsedExit
                }
                errorMessage = null
                onSave(entry, exit, source, noteText)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit stay history") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = entryDateText,
                    onValueChange = { entryDateText = it },
                    label = { Text("Entry date (YYYY-MM-DD)") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Has exit date")
                    Switch(
                        checked = hasExitDate,
                        onCheckedChange = { enabled ->
                            hasExitDate = enabled
                            if (!enabled) exitDateText = ""
                        }
                    )
                }
                if (hasExitDate) {
                    OutlinedTextField(
                        value = exitDateText,
                        onValueChange = { exitDateText = it },
                        label = { Text("Exit date (YYYY-MM-DD)") },
                        singleLine = true
                    )
                }
                durationText?.let {
                    Text(it, fontWeight = FontWeight.SemiBold)
                }
                Text("Source", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { source = EntrySource.MANUAL },
                        enabled = source != EntrySource.MANUAL
                    ) { Text("Manual") }
                    Button(
                        onClick = { source = EntrySource.AUTO },
                        enabled = source != EntrySource.AUTO
                    ) { Text("Auto") }
                }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onDelete) { Text("Delete stay") }
                errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}

@Composable
private fun EditPlannedTripDialog(
    trip: PlannedTrip,
    onDismiss: () -> Unit,
    onSave: (entryDate: LocalDate, exitDate: LocalDate, note: String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit
) {
    var entryDateText by remember(trip.id) { mutableStateOf(trip.entryDate.toString()) }
    var exitDateText by remember(trip.id) { mutableStateOf(trip.exitDate.toString()) }
    var noteText by remember(trip.id) { mutableStateOf(trip.note) }
    var errorMessage by remember(trip.id) { mutableStateOf<String?>(null) }
    val durationText = plannedTripDurationSummary(
        entry = parseIsoDate(entryDateText),
        exit = parseIsoDate(exitDateText)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val entry = parseIsoDate(entryDateText)
                val exit = parseIsoDate(exitDateText)
                when {
                    entry == null -> errorMessage = "Entry date must use YYYY-MM-DD."
                    exit == null -> errorMessage = "Exit date must use YYYY-MM-DD."
                    exit.isBefore(entry) -> errorMessage = "Exit date cannot be before entry date."
                    else -> {
                        errorMessage = null
                        onSave(entry, exit, noteText)
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit planned trip") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = entryDateText,
                    onValueChange = { entryDateText = it },
                    label = { Text("Entry date (YYYY-MM-DD)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = exitDateText,
                    onValueChange = { exitDateText = it },
                    label = { Text("Exit date (YYYY-MM-DD)") },
                    singleLine = true
                )
                durationText?.let {
                    Text(it, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onConfirm) { Text("Confirm as stay") }
                TextButton(onClick = onDelete) { Text("Delete planned trip") }
                errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}

@Composable
private fun EditProfileDialog(
    profile: Profile,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, passportNumber: String) -> Unit,
    onDelete: () -> Unit
) {
    var nameText by remember(profile.id) { mutableStateOf(profile.name) }
    var passportText by remember(profile.id) { mutableStateOf(profile.passportNumber) }
    var errorMessage by remember(profile.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (nameText.isBlank()) {
                    errorMessage = "Profile name is required."
                    return@TextButton
                }
                errorMessage = null
                onSave(nameText, passportText)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(if (isActive) "Edit passport profile (Active)" else "Edit passport profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Profile name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = passportText,
                    onValueChange = { passportText = it },
                    label = { Text("Passport number") },
                    singleLine = true
                )
                TextButton(onClick = onDelete) { Text("Delete profile") }
                errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}

private fun parseIsoDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim()) }.getOrNull()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private data class UnlockPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val unlockedDays: Int
)

private fun summarizeUnlockPeriods(unlockedDaysByDate: Map<LocalDate, Int>): List<UnlockPeriod> {
    if (unlockedDaysByDate.isEmpty()) return emptyList()
    val sortedEntries = unlockedDaysByDate.entries.sortedBy { it.key }

    val periods = mutableListOf<UnlockPeriod>()
    var periodStart = sortedEntries.first().key
    var periodEnd = periodStart
    var periodUnlockedDays = sortedEntries.first().value

    for ((date, unlockedDays) in sortedEntries.drop(1)) {
        if (date == periodEnd.plusDays(1)) {
            periodEnd = date
            periodUnlockedDays += unlockedDays
        } else {
            periods.add(
                UnlockPeriod(
                    startDate = periodStart,
                    endDate = periodEnd,
                    unlockedDays = periodUnlockedDays
                )
            )
            periodStart = date
            periodEnd = date
            periodUnlockedDays = unlockedDays
        }
    }

    periods.add(
        UnlockPeriod(
            startDate = periodStart,
            endDate = periodEnd,
            unlockedDays = periodUnlockedDays
        )
    )
    return periods
}

private fun formatUnlockPeriod(period: UnlockPeriod, formatter: DateTimeFormatter): String {
    val dateLabel = if (period.startDate == period.endDate) {
        period.startDate.format(formatter)
    } else {
        "${period.startDate.format(formatter)} - ${period.endDate.format(formatter)}"
    }
    return "$dateLabel: ${formatDayCount(period.unlockedDays)}"
}

private fun stayDurationSummary(stay: Stay): String {
    return stayDurationSummary(
        entry = stay.entryDate,
        exit = stay.exitDate,
        hasExitDate = stay.exitDate != null
    ).orEmpty()
}

private fun stayDurationSummary(entry: LocalDate?, exit: LocalDate?, hasExitDate: Boolean): String? {
    if (entry == null) return null
    val effectiveExit = if (hasExitDate) exit ?: return null else LocalDate.now()
    if (effectiveExit.isBefore(entry)) return null
    val label = if (hasExitDate) "Duration" else "Days so far"
    return "$label: ${formatDayCount(inclusiveDayCount(entry, effectiveExit))}"
}

private fun plannedTripDurationSummary(entry: LocalDate?, exit: LocalDate?): String? {
    if (entry == null || exit == null || exit.isBefore(entry)) return null
    return "Duration: ${formatDayCount(inclusiveDayCount(entry, exit))}"
}

private fun inclusiveDayCount(start: LocalDate, end: LocalDate): Int {
    return ChronoUnit.DAYS.between(start, end).toInt() + 1
}

private fun formatDayCount(value: Int): String {
    return if (value == 1) "1 day" else "$value days"
}

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

private fun hasBackgroundLocationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
