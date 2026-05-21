package com.schengen.tracker.data

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One-time CSV backup of pre-v5 data (stays + planned_trips) written to
 * `<filesDir>/backups/pre-v5-<timestamp>.csv` before the Room 4 -> 5 migration runs.
 *
 * Gated by a SharedPreferences flag so it only happens once.
 */
object PreV5Backup {
    private const val PREFS_NAME = "schengen_prefs"
    private const val FLAG_KEY = "pre_v5_backup_completed"

    fun runIfNeeded(context: Context, databaseName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(FLAG_KEY, false)) return

        val dbFile = context.getDatabasePath(databaseName)
        if (!dbFile.exists()) {
            prefs.edit().putBoolean(FLAG_KEY, true).apply()
            return
        }

        runCatching { writeBackup(context, databaseName) }
            .onSuccess { prefs.edit().putBoolean(FLAG_KEY, true).apply() }
    }

    private fun writeBackup(context: Context, databaseName: String) {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(/* version = */ 4) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        helper.use { openHelper ->
            val db = openHelper.readableDatabase
            val currentVersion = db.version
            if (currentVersion >= 5) return@use
            if (!hasTable(db, "stays") && !hasTable(db, "planned_trips")) return@use

            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "pre-v5-$timestamp.csv")

            backupFile.bufferedWriter().use { writer ->
                writer.appendLine("type,profile_name,passport_number,entry_date,exit_date,source,note,countries")
                val profilesByID = if (hasTable(db, "profiles")) {
                    fetchProfiles(db)
                } else emptyMap()

                if (hasTable(db, "stays")) {
                    db.query("SELECT profileId, entryDate, exitDate, source, note, countries FROM stays").use { cursor ->
                        while (cursor.moveToNext()) {
                            val profileId = cursor.getLong(0)
                            val profile = profilesByID[profileId]
                            writer.appendLine(
                                encodeRow(
                                    listOf(
                                        "STAY",
                                        profile?.first.orEmpty(),
                                        profile?.second.orEmpty(),
                                        cursor.getStringOrEmpty(1),
                                        cursor.getStringOrEmpty(2),
                                        cursor.getStringOrEmpty(3),
                                        cursor.getStringOrEmpty(4),
                                        cursor.getStringOrEmpty(5)
                                    )
                                )
                            )
                        }
                    }
                }

                if (hasTable(db, "planned_trips")) {
                    db.query("SELECT profileId, entryDate, exitDate, note, countries FROM planned_trips").use { cursor ->
                        while (cursor.moveToNext()) {
                            val profileId = cursor.getLong(0)
                            val profile = profilesByID[profileId]
                            writer.appendLine(
                                encodeRow(
                                    listOf(
                                        "PLANNED",
                                        profile?.first.orEmpty(),
                                        profile?.second.orEmpty(),
                                        cursor.getStringOrEmpty(1),
                                        cursor.getStringOrEmpty(2),
                                        "",
                                        cursor.getStringOrEmpty(3),
                                        cursor.getStringOrEmpty(4)
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun fetchProfiles(
        db: androidx.sqlite.db.SupportSQLiteDatabase
    ): Map<Long, Pair<String, String>> {
        val out = mutableMapOf<Long, Pair<String, String>>()
        db.query("SELECT id, name, passportNumber FROM profiles").use { cursor ->
            while (cursor.moveToNext()) {
                out[cursor.getLong(0)] = cursor.getStringOrEmpty(1) to cursor.getStringOrEmpty(2)
            }
        }
        return out
    }

    private fun hasTable(db: androidx.sqlite.db.SupportSQLiteDatabase, name: String): Boolean {
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf<Any>(name)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun Cursor.getStringOrEmpty(index: Int): String =
        if (isNull(index)) "" else getString(index).orEmpty()

    private fun encodeRow(values: List<String>): String =
        values.joinToString(",") { value ->
            val escaped = value.replace("\"", "\"\"")
            "\"$escaped\""
        }
}
