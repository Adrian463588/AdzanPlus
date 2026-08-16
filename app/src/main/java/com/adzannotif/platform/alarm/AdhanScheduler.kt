package com.adzannotif.platform.alarm

import com.adzannotif.domain.repository.AlarmRepository
import com.adzannotif.domain.usecase.SchedulePrayerAlarmsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level scheduler coordinating prayer alarm setup, exact alarm permission checks,
 * and background alarm synchronization.
 */
@Singleton
class AdhanScheduler @Inject constructor(
    private val schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase,
    private val alarmRepository: AlarmRepository,
) {
    /**
     * Checks if the app has exact alarm scheduling capability (Android 12+ SCHEDULE_EXACT_ALARM).
     */
    fun canScheduleExactAlarms(): Boolean {
        return alarmRepository.canScheduleExactAlarms()
    }

    /**
     * Suspend function to compute and schedule upcoming prayer alarms.
     */
    suspend fun schedule(): Result<Unit> {
        return try {
            schedulePrayerAlarmsUseCase()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Non-blocking trigger to reschedule all upcoming prayer alarms in a background scope.
     */
    fun rescheduleAllAlarms(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch {
            try {
                schedulePrayerAlarmsUseCase()
            } catch (e: Exception) {
                // Background exception handled gracefully
            }
        }
    }

    /**
     * Cancels all scheduled prayer and pre-reminder alarms.
     */
    suspend fun cancelAllAlarms() {
        alarmRepository.cancelAllAlarms()
    }
}

