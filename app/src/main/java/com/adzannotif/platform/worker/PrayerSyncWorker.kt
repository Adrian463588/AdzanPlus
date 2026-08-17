package com.adzannotif.platform.worker

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.platform.alarm.AlarmScheduler
import com.adzannotif.platform.alarm.CelestialAlarmScheduler
import com.adzannotif.widget.MoonWidget
import com.adzannotif.widget.SunWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker executing at midnight (00:05 AM) daily to compute/refresh
 * the 30-day prayer times offline cache and verify exact alarm schedules.
 */
@HiltWorker
class PrayerSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val celestialAlarmScheduler: CelestialAlarmScheduler,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting daily midnight prayer times sync and alarm reconciliation")
            val location = locationRepository.currentOrSelectedLocation.first()
                ?: return Result.success()
            val settings = settingsRepository.userSettings.first()
            val today = Clock.System.now().toLocalDateTime(TimeZone.of(location.timeZoneId)).date

            // Compute and cache next 30 days of prayer times
            prayerTimesRepository.computeAndCachePrayerTimes(
                startDate = today,
                daysCount = 30,
                location = location,
                settings = settings
            )

            // Reconcile prayer alarms only when exact alarm access is available.
            // A denied permission is actionable but not a transient worker failure.
            alarmScheduler.schedule().onFailure { error ->
                Log.w(TAG, "Prayer alarms were not scheduled during sync", error)
            }

            // Update Glance Widgets
            try {
                MoonWidget().updateAll(applicationContext)
                SunWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update glance widgets", e)
            }

            val epochMillis = System.currentTimeMillis()
            celestialAlarmScheduler.reconcile(
                location = location,
                fromMillis = epochMillis,
                days = CELESTIAL_RECONCILIATION_DAYS,
            ).onSuccess { scheduledCount ->
                Log.d(TAG, "Reconciled $scheduledCount celestial alarms")
            }.onFailure { error ->
                Log.w(TAG, "Celestial alarms were not scheduled during sync", error)
            }

            Log.d(TAG, "Daily prayer sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Daily prayer sync worker failed, scheduling retry", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "PrayerSyncDailyWorker"
        const val CELESTIAL_RECONCILIATION_DAYS = 7
        private const val TAG = "PrayerSyncWorker"

        fun enqueuePeriodicWork(context: Context) {
            val initialDelay = calculateInitialDelayToMidnight()
            Log.d(TAG, "Enqueuing periodic prayer sync with initial delay: ${initialDelay / 1000 / 60} minutes")

            val request = PeriodicWorkRequestBuilder<PrayerSyncWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        private fun calculateInitialDelayToMidnight(): Long {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If 00:05 has already passed for today, set to tomorrow's 00:05
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return calendar.timeInMillis - now
        }

    }
}
