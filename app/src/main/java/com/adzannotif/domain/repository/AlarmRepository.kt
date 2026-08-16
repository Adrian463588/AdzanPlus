package com.adzannotif.domain.repository

import com.adzannotif.core.prayer.Prayer
import kotlinx.datetime.Instant

interface AlarmRepository {
    fun canScheduleExactAlarms(): Boolean
    suspend fun scheduleExactAlarm(prayer: Prayer, targetInstant: Instant, title: String, isPreReminder: Boolean = false)
    suspend fun cancelAlarm(prayer: Prayer)
    suspend fun cancelAllAlarms()
}
