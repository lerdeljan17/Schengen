package com.schengen.tracker

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.schengen.tracker.data.AppDatabase
import com.schengen.tracker.data.PreV5Backup
import com.schengen.tracker.data.TripRepository
import com.schengen.tracker.sync.DriveBackupService
import com.schengen.tracker.sync.GoogleSignInManager
import com.schengen.tracker.ui.theme.ThemePreferences

class SchengenApp : Application() {
    lateinit var repository: TripRepository
        private set

    lateinit var themePreferences: ThemePreferences
        private set

    lateinit var googleSignInManager: GoogleSignInManager
        private set

    lateinit var driveBackupService: DriveBackupService
        private set

    override fun onCreate() {
        super.onCreate()

        PreV5Backup.runIfNeeded(this, DATABASE_NAME)

        val db = Room.databaseBuilder(this, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .build()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        repository = TripRepository(db.tripDao(), prefs)
        themePreferences = ThemePreferences(prefs)
        googleSignInManager = GoogleSignInManager(this)
        driveBackupService = DriveBackupService(this, repository, themePreferences, prefs)
    }

    companion object {
        private const val DATABASE_NAME = "schengen.db"
        private const val PREFS_NAME = "schengen_prefs"
    }
}
