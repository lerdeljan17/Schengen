package com.schengen.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schengen.tracker.location.SchengenCountryCatalog
import com.schengen.tracker.ui.screens.TextHelpers

@Composable
fun CountryPickerDialog(
    title: String,
    selectedCountryCodes: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var pendingSelection by remember(selectedCountryCodes) { mutableStateOf(selectedCountryCodes) }
    val filteredCountries = SchengenCountryCatalog.filter(searchQuery, pendingSelection.toSet())

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(pendingSelection) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search countries") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = "${pendingSelection.size} selected",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCountries, key = { it.code }) { country ->
                        val isSelected = country.code in pendingSelection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingSelection = TextHelpers.toggleCountrySelection(
                                        pendingSelection,
                                        country.code
                                    )
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(country.name)
                                Text(country.code, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(countryCodeToFlagEmoji(country.code))
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CountrySelectionSummary(
    selectedCountryCodes: List<String>,
    onPickCountries: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (selectedCountryCodes.isEmpty()) {
                "Countries: none selected"
            } else {
                "Countries: ${TextHelpers.formatCountryNames(selectedCountryCodes)}"
            },
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = onPickCountries) {
            Text(if (selectedCountryCodes.isEmpty()) "Select countries" else "Edit countries")
        }
    }
}
