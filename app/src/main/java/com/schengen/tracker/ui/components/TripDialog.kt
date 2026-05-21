package com.schengen.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schengen.tracker.data.EntrySource
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.ui.screens.TextHelpers
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unified add/edit dialog for a Trip. When [existingTrip] is null, this acts as
 * an "Add trip" dialog. When non-null, it edits the existing trip and supports delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDialog(
    existingTrip: Trip?,
    onDismiss: () -> Unit,
    onSave: (
        entryDate: LocalDate,
        exitDate: LocalDate?,
        source: EntrySource,
        note: String,
        countries: List<String>
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val today = LocalDate.now()
    var entryDate by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.entryDate ?: today)
    }
    var hasExitDate by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.exitDate != null || existingTrip == null)
    }
    var exitDate by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.exitDate ?: today)
    }
    var source by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.source ?: EntrySource.MANUAL)
    }
    var note by remember(existingTrip?.id) { mutableStateOf(existingTrip?.note.orEmpty()) }
    var countries by remember(existingTrip?.id) {
        mutableStateOf(existingTrip?.countries ?: emptyList())
    }
    var showCountryPicker by remember { mutableStateOf(false) }
    var showEntryPicker by remember { mutableStateOf(false) }
    var showExitPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val formatter = TextHelpers.dateFormatter

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val finalExit = if (hasExitDate) exitDate else null
                if (finalExit != null && finalExit.isBefore(entryDate)) {
                    error = "Exit date cannot be before entry date."
                    return@TextButton
                }
                error = null
                onSave(entryDate, finalExit, source, note, countries)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(if (existingTrip == null) "Add trip" else "Edit trip") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showEntryPicker = true }
                    ) {
                        Text("Entry: ${entryDate.format(formatter)}")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showExitPicker = true },
                        enabled = hasExitDate
                    ) {
                        Text(
                            if (hasExitDate) "Exit: ${exitDate.format(formatter)}"
                            else "Open"
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Has exit date")
                    Switch(
                        checked = hasExitDate,
                        onCheckedChange = { hasExitDate = it }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Trip name or note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                CountrySelectionSummary(
                    selectedCountryCodes = countries,
                    onPickCountries = { showCountryPicker = true }
                )
                if (existingTrip != null) {
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
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("Delete trip", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )

    if (showCountryPicker) {
        CountryPickerDialog(
            title = "Countries visited on this trip",
            selectedCountryCodes = countries,
            onDismiss = { showCountryPicker = false },
            onConfirm = {
                countries = it
                showCountryPicker = false
            }
        )
    }

    if (showEntryPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = entryDate.atStartOfDayUtcMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEntryPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        entryDate = it.toLocalDate()
                    }
                    showEntryPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEntryPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showExitPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = exitDate.atStartOfDayUtcMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showExitPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        exitDate = it.toLocalDate()
                    }
                    showExitPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showExitPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun LocalDate.atStartOfDayUtcMillis(): Long =
    atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate()
