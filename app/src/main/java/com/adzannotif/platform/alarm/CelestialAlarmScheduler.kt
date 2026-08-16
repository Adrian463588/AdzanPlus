package com.adzannotif.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.SkyEvent
import com.adzannotif.domain.model.astronomy.SkyEventType
import com.adzannotif.domain.repository.AstronomyRepository
import com.adzannotif.platform.receiver.CelestialAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reconciles the next seven days of computed celestial events with exact alarms.
 * No inexact fallback is used because an event notification is only truthful when
 * the platform can honour its requested time.
 */
@Singleton
class CelestialAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val astronomyRepository: AstronomyRepository,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun reconcile(
        location: LocationInfo,
        fromMillis: Long = System.currentTimeMillis(),
        days: Int = DEFAULT_DAYS,
    ): Result<Int> {
        if (!canScheduleExactAlarms()) {
            return Result.failure(AdhanScheduler.ExactAlarmPermissionRequiredException())
        }

        val events = astronomyRepository
            .getUpcomingEvents(location.latitude, location.longitude, fromMillis, days)
            .asSequence()
            .filter { it.epochMillis > System.currentTimeMillis() }
            .distinctBy { event -> event.type to event.epochMillis }
            .toList()

        cancelReconciledWindow(fromMillis, days)

        return try {
            events.forEach { event -> scheduleExact(event) }
            Result.success(events.size)
        } catch (error: SecurityException) {
            Log.e(TAG, "Exact celestial alarm permission was revoked during reconciliation", error)
            Result.failure(error)
        }
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun scheduleExact(event: SkyEvent) {
        val pendingIntent = pendingIntent(event, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                event.epochMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, event.epochMillis, pendingIntent)
        }
    }

    private fun cancelReconciledWindow(fromMillis: Long, days: Int) {
        val firstDay = Math.floorDiv(fromMillis, DAY_MILLIS) - 1
        val lastDay = firstDay + days + 2
        for (day in firstDay..lastDay) {
            SkyEventType.entries.forEach { type ->
                val pendingIntent = existingPendingIntent(
                    eventType = type,
                    eventEpochMillis = day * DAY_MILLIS,
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        }

        // Remove request codes emitted by the previous index-based scheduler once.
        for (legacyIndex in 0 until LEGACY_EVENT_REQUEST_COUNT) {
            val intent = Intent(context, CelestialAlarmReceiver::class.java).apply {
                action = CelestialAlarmReceiver.ACTION_CELESTIAL_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                LEGACY_REQUEST_BASE + legacyIndex,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun pendingIntent(event: SkyEvent, flags: Int): PendingIntent {
        val day = Math.floorDiv(event.epochMillis, DAY_MILLIS)
        val intent = Intent(context, CelestialAlarmReceiver::class.java).apply {
            action = CelestialAlarmReceiver.ACTION_CELESTIAL_ALARM
            data = Uri.parse("adzannotif://celestial/$day/${event.type.name}")
            putExtra(CelestialAlarmReceiver.EXTRA_EVENT_TYPE, event.type.name)
            putExtra(CelestialAlarmReceiver.EXTRA_EVENT_LABEL, event.label)
        }
        val requestCode = REQUEST_CODE_BASE +
            (day.mod(REQUEST_DAY_BUCKETS) * SkyEventType.entries.size).toInt() + event.type.ordinal
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun existingPendingIntent(
        eventType: SkyEventType,
        eventEpochMillis: Long,
    ): PendingIntent? {
        val day = Math.floorDiv(eventEpochMillis, DAY_MILLIS)
        val intent = Intent(context, CelestialAlarmReceiver::class.java).apply {
            action = CelestialAlarmReceiver.ACTION_CELESTIAL_ALARM
            data = Uri.parse("adzannotif://celestial/$day/${eventType.name}")
        }
        val requestCode = REQUEST_CODE_BASE + (day.mod(REQUEST_DAY_BUCKETS) * SkyEventType.entries.size).toInt() + eventType.ordinal
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntent(
        eventType: SkyEventType,
        eventEpochMillis: Long,
        flags: Int,
    ): PendingIntent {
        val day = Math.floorDiv(eventEpochMillis, DAY_MILLIS)
        val intent = Intent(context, CelestialAlarmReceiver::class.java).apply {
            action = CelestialAlarmReceiver.ACTION_CELESTIAL_ALARM
            data = Uri.parse("adzannotif://celestial/$day/${eventType.name}")
            putExtra(CelestialAlarmReceiver.EXTRA_EVENT_TYPE, eventType.name)
        }
        val requestCode = REQUEST_CODE_BASE + (day.mod(REQUEST_DAY_BUCKETS) * SkyEventType.entries.size).toInt() + eventType.ordinal
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "CelestialAlarmScheduler"
        const val DEFAULT_DAYS = 7
        const val DAY_MILLIS = 86_400_000L
        const val REQUEST_CODE_BASE = 20_000
        const val REQUEST_DAY_BUCKETS = 100_000L
        const val LEGACY_REQUEST_BASE = 1_000
        const val LEGACY_EVENT_REQUEST_COUNT = 256
    }
}
