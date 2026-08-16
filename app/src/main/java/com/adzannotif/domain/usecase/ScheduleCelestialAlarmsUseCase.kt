package com.adzannotif.domain.usecase

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.repository.AstronomyRepository
import com.adzannotif.platform.receiver.CelestialAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ScheduleCelestialAlarmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AstronomyRepository
) {
    suspend operator fun invoke(location: LocationInfo, fromMillis: Long, days: Int) {
        val events = repository.getUpcomingEvents(location.latitude, location.longitude, fromMillis, days)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        events.forEachIndexed { index, event ->
            if (event.epochMillis > System.currentTimeMillis()) {
                val intent = Intent(context, CelestialAlarmReceiver::class.java).apply {
                    action = CelestialAlarmReceiver.ACTION_CELESTIAL_ALARM
                    putExtra(CelestialAlarmReceiver.EXTRA_EVENT_TYPE, event.type.name)
                    putExtra(CelestialAlarmReceiver.EXTRA_EVENT_LABEL, event.label)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    index + 1000,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        event.epochMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    // Ignore or log if exact alarms permission not granted
                }
            }
        }
    }
}
