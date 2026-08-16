package com.adzannotif.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.adzannotif.MainActivity
import com.adzannotif.R
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object WidgetRenderer {

    fun buildCompactWidget(
        context: Context,
        location: LocationInfo,
        nextPrayerName: String,
        targetInstant: Instant,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)
        val tz = TimeZone.of(location.timeZoneId)
        val localTime = targetInstant.toLocalDateTime(tz)
        val timeFormatted = String.format("%02d:%02d WIB", localTime.hour, localTime.minute)

        views.setTextViewText(R.id.widget_location_title, location.name)
        views.setTextViewText(R.id.widget_next_prayer_name, nextPrayerName.uppercase())
        views.setTextViewText(R.id.widget_target_time, timeFormatted)

        // Set Realtime Chronometer countdown
        val baseChronometerMillis = SystemClock.elapsedRealtime() + (targetInstant.toEpochMilliseconds() - System.currentTimeMillis())
        views.setChronometer(R.id.widget_countdown_chronometer, baseChronometerMillis, null, true)

        // Click on widget opens MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_compact_root, pendingIntent)

        return views
    }

    fun buildDetailedWidget(
        context: Context,
        location: LocationInfo,
        nextPrayerName: String,
        targetInstant: Instant,
        record: PrayerTimeRecord,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_detailed)
        val tz = TimeZone.of(location.timeZoneId)
        val localTarget = targetInstant.toLocalDateTime(tz)
        val targetFormatted = String.format("%02d:%02d WIB", localTarget.hour, localTarget.minute)

        views.setTextViewText(R.id.widget_det_location, location.name)
        views.setTextViewText(R.id.widget_det_prayer_name, nextPrayerName.uppercase())
        views.setTextViewText(R.id.widget_det_prayer_time, targetFormatted)

        // Chronometer countdown
        val baseChronometerMillis = SystemClock.elapsedRealtime() + (targetInstant.toEpochMilliseconds() - System.currentTimeMillis())
        views.setChronometer(R.id.widget_det_countdown, baseChronometerMillis, null, true)

        // 5-Prayer timeline
        val fSubuh = record.fajr.toLocalDateTime(tz)
        val fDzuhur = record.dhuhr.toLocalDateTime(tz)
        val fAshar = record.asr.toLocalDateTime(tz)
        val fMaghrib = record.maghrib.toLocalDateTime(tz)
        val fIsya = record.isha.toLocalDateTime(tz)

        views.setTextViewText(R.id.widget_det_fajr, String.format("%02d:%02d", fSubuh.hour, fSubuh.minute))
        views.setTextViewText(R.id.widget_det_dhuhr, String.format("%02d:%02d", fDzuhur.hour, fDzuhur.minute))
        views.setTextViewText(R.id.widget_det_asr, String.format("%02d:%02d", fAshar.hour, fAshar.minute))
        views.setTextViewText(R.id.widget_det_maghrib, String.format("%02d:%02d", fMaghrib.hour, fMaghrib.minute))
        views.setTextViewText(R.id.widget_det_isha, String.format("%02d:%02d", fIsya.hour, fIsya.minute))

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_detailed_root, pendingIntent)

        return views
    }
}
