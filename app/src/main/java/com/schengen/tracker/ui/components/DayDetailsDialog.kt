package com.schengen.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schengen.tracker.domain.SchengenCalculator
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.ui.screens.TextHelpers
import java.time.LocalDate

@Composable
fun DayDetailsDialog(
    date: LocalDate,
    trips: List<Trip>,
    calculator: SchengenCalculator,
    today: LocalDate,
    onDismiss: () -> Unit
) {
    val windowStart = calculator.rollingWindowStart(date)
    val windowEnd = calculator.rollingWindowEnd(date)
    val usedDays = calculator.usedDaysOn(date, trips)
    val tripsInWindow = calculator.tripsInWindow(date, trips)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text(
                text = "Day details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "On ${date.format(TextHelpers.longDateFormatter)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Maximum continuous stay: ${TextHelpers.formatDayCount(usedDays)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Window: ${windowStart.format(TextHelpers.shortDateFormatter)} – ${windowEnd.format(TextHelpers.shortDateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (tripsInWindow.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Trips in this window",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    tripsInWindow.forEach { trip ->
                        DayDetailsTripRow(
                            trip = trip,
                            date = date,
                            today = today,
                            calculator = calculator
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DayDetailsTripRow(
    trip: Trip,
    date: LocalDate,
    today: LocalDate,
    calculator: SchengenCalculator
) {
    val exit = trip.exitDate ?: today
    val daysInWindow = calculator.daysCountedInWindow(date, trip)
    val flag = trip.countries.firstOrNull()?.let(::countryCodeToFlagEmoji) ?: "🗺"
    val label = TextHelpers.tripLabel(trip).lowercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = flag,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label, ${trip.entryDate.format(TextHelpers.shortDateFormatter)} – ${exit.format(TextHelpers.shortDateFormatter)} • ${TextHelpers.formatDayCount(daysInWindow)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
