package com.adzannotif.platform.alarm

import android.content.Intent

/** Android alarm boundary used by lifecycle hosts and receivers. */
interface AlarmScheduler {
    fun canScheduleExactAlarms(): Boolean
    fun exactAlarmPermissionIntent(): Intent?
    suspend fun schedule(): Result<Unit>
    fun rescheduleAllAlarms()
    suspend fun cancelAllAlarms()
}
