package com.adzannotif.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.repository.AlarmRepository
import com.adzannotif.platform.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmRepository {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override suspend fun scheduleExactAlarm(
        prayer: Prayer,
        targetInstant: Instant,
        title: String,
        isPreReminder: Boolean
    ) {
        val requestCode = getRequestCode(prayer, isPreReminder)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_FIRE
            putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
            putExtra(AlarmReceiver.EXTRA_PRAYER_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_IS_PRE_REMINDER, isPreReminder)
            putExtra(AlarmReceiver.EXTRA_TARGET_EPOCH_MS, targetInstant.toEpochMilliseconds())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = targetInstant.toEpochMilliseconds()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(
                "AlarmRepositoryImpl",
                "SCHEDULE_EXACT_ALARM denied on Android 12+, falling back to setAndAllowWhileIdle",
                e
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    override suspend fun cancelAlarm(prayer: Prayer) {
        cancelSingleAlarm(prayer, false)
        cancelSingleAlarm(prayer, true)
    }

    private fun cancelSingleAlarm(prayer: Prayer, isPreReminder: Boolean) {
        val requestCode = getRequestCode(prayer, isPreReminder)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_FIRE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override suspend fun cancelAllAlarms() {
        for (prayer in Prayer.entries) {
            cancelAlarm(prayer)
        }
    }

    private fun getRequestCode(prayer: Prayer, isPreReminder: Boolean): Int {
        val baseCode = prayer.ordinal * 10
        return if (isPreReminder) baseCode + 1 else baseCode
    }
}
