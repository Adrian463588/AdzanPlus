package com.adzannotif.platform.alarm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.adzannotif.domain.repository.AlarmRepository
import com.adzannotif.domain.usecase.SchedulePrayerAlarmsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level scheduler coordinating prayer alarm setup, exact alarm permission checks,
 * and background alarm synchronization.
 */
@Singleton
class AdhanScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase,
    private val alarmRepository: AlarmRepository,
) : AlarmScheduler {
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Checks if the app has exact alarm scheduling capability (Android 12+ SCHEDULE_EXACT_ALARM).
     */
    override fun canScheduleExactAlarms(): Boolean {
        return alarmRepository.canScheduleExactAlarms()
    }

    /**
     * Returns the system settings intent required to grant exact alarm access.
     *
     * A null result means that the permission is not required on this API level,
     * or that it has already been granted.
     */
    override fun exactAlarmPermissionIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms()) {
            return null
        }

        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}"),
        )
    }

    /**
     * Reconciles the complete prayer alarm set.
     *
     * The permission check intentionally happens before cancelling anything. When
     * exact-alarm access is unavailable, the app must report an actionable state and
     * must not silently downgrade a user-facing prayer alarm to an inexact alarm.
     */
    override suspend fun schedule(): Result<Unit> {
        if (!canScheduleExactAlarms()) {
            return Result.failure(ExactAlarmPermissionRequiredException())
        }

        return try {
            // Reconciliation also removes stale pre-reminder PendingIntents after a
            // settings change (for example, changing 10 minutes to disabled).
            alarmRepository.cancelAllAlarms()
            schedulePrayerAlarmsUseCase()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Non-blocking trigger to reschedule all upcoming prayer alarms in a background scope.
     */
    override fun rescheduleAllAlarms() {
        backgroundScope.launch {
            schedule().onFailure { error ->
                if (error is ExactAlarmPermissionRequiredException) {
                    Log.w(TAG, "Exact alarm permission is required; prayer alarms were not scheduled")
                } else {
                    Log.e(TAG, "Prayer alarm reconciliation failed", error)
                }
            }
        }
    }

    /**
     * Cancels all scheduled prayer and pre-reminder alarms.
     */
    override suspend fun cancelAllAlarms() {
        alarmRepository.cancelAllAlarms()
    }

    class ExactAlarmPermissionRequiredException : IllegalStateException(
        "SCHEDULE_EXACT_ALARM permission is required before scheduling prayer alarms",
    )

    private companion object {
        const val TAG = "AdhanScheduler"
    }
}
