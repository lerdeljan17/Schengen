package com.schengen.tracker.ui.screens

import com.schengen.tracker.domain.Stay
import com.schengen.tracker.location.SchengenCountryCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal data class UnlockPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val unlockedDays: Int
)

internal object SchengenScreenText {
    fun parseIsoDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.trim()) }.getOrNull()

    fun formatCountryNames(countryCodes: List<String>): String =
        SchengenCountryCatalog.displayNames(countryCodes).joinToString()

    fun toggleCountrySelection(selectedCountryCodes: List<String>, code: String): List<String> =
        if (code in selectedCountryCodes) {
            selectedCountryCodes.filterNot { it == code }
        } else {
            selectedCountryCodes + code
        }

    fun summarizeUnlockPeriods(unlockedDaysByDate: Map<LocalDate, Int>): List<UnlockPeriod> {
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

    fun formatUnlockPeriod(period: UnlockPeriod, formatter: DateTimeFormatter): String {
        val dateLabel = if (period.startDate == period.endDate) {
            period.startDate.format(formatter)
        } else {
            "${period.startDate.format(formatter)} - ${period.endDate.format(formatter)}"
        }
        return "$dateLabel: ${formatDayCount(period.unlockedDays)}"
    }

    fun stayDurationSummary(stay: Stay, todayDate: LocalDate = LocalDate.now()): String {
        return stayDurationSummary(
            entry = stay.entryDate,
            exit = stay.exitDate,
            hasExitDate = stay.exitDate != null,
            todayDate = todayDate
        ).orEmpty()
    }

    fun stayDurationSummary(
        entry: LocalDate?,
        exit: LocalDate?,
        hasExitDate: Boolean,
        todayDate: LocalDate = LocalDate.now()
    ): String? {
        if (entry == null) return null
        val effectiveExit = if (hasExitDate) exit ?: return null else todayDate
        if (effectiveExit.isBefore(entry)) return null
        val label = if (hasExitDate) "Duration" else "Days so far"
        return "$label: ${formatDayCount(inclusiveDayCount(entry, effectiveExit))}"
    }

    fun plannedTripDurationSummary(entry: LocalDate?, exit: LocalDate?): String? {
        if (entry == null || exit == null || exit.isBefore(entry)) return null
        return "Duration: ${formatDayCount(inclusiveDayCount(entry, exit))}"
    }

    fun inclusiveDayCount(start: LocalDate, end: LocalDate): Int {
        return ChronoUnit.DAYS.between(start, end).toInt() + 1
    }

    fun formatDayCount(value: Int): String {
        return if (value == 1) "1 day" else "$value days"
    }

    fun formatDelta(value: Int): String {
        return when {
            value > 0 -> "+$value days"
            value < 0 -> "$value days"
            else -> "0 days"
        }
    }
}
