package com.schengen.tracker.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.schengen.tracker.SchengenApp
import com.schengen.tracker.domain.SchengenCalculator
import java.time.LocalDate

class OverstayAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val calculator = SchengenCalculator()

    override suspend fun doWork(): Result {
        val app = applicationContext as SchengenApp
        val repository = app.repository
        val profile = repository.getCurrentProfile() ?: return Result.success()
        val (stays, plannedTrips) = repository.getSnapshotForActiveProfile()
        val available = calculator.availableDaysOn(LocalDate.now(), stays)
        val threshold = calculator.nextAlertThreshold(available) ?: return Result.success()

        val prefs = applicationContext.getSharedPreferences("schengen_prefs", Context.MODE_PRIVATE)
        val key = "alert_${profile.id}_${LocalDate.now()}_$threshold"
        if (prefs.getBoolean(key, false)) return Result.success()

        val overstayDate = calculator.firstPlannedOverstayDate(LocalDate.now(), stays, plannedTrips)
        val body = if (overstayDate != null) {
            "${profile.name}: $available days left. Planned trips first exceed the limit on $overstayDate."
        } else {
            "${profile.name}: $available days left in your current 180-day window."
        }

        NotificationHelper.showRiskNotification(
            context = applicationContext,
            title = "Schengen days running low",
            body = body,
            notificationId = (profile.id * 31 + threshold).toInt()
        )
        prefs.edit().putBoolean(key, true).apply()
        return Result.success()
    }
}
