package com.schengen.tracker.sync

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val createdAt: Long,
    val appearanceMode: String,
    val colorThemeId: String,
    val startWeekOnSunday: Boolean,
    val profiles: List<BackupProfile>
)

@Serializable
data class BackupProfile(
    val name: String,
    val passportNumber: String,
    val trips: List<BackupTrip>
)

@Serializable
data class BackupTrip(
    val entryDate: String,
    val exitDate: String?,
    val source: String,
    val note: String,
    val countries: List<String>
)
