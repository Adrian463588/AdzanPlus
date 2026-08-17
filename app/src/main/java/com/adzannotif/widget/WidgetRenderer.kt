package com.adzannotif.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.adzannotif.MainActivity
import com.adzannotif.R
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

/** Builds RemoteViews exclusively from a computed prayer snapshot. */
object WidgetRenderer {

    fun buildCompactWidget(
        context: Context,
        location: LocationInfo,
        nextPrayerName: String,
        targetInstant: Instant,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)
        val localTarget = targetInstant.toLocalDateTime(TimeZone.of(location.timeZoneId))

        views.setTextViewText(R.id.widget_location_title, location.name)
        views.setTextViewText(R.id.widget_next_prayer_name, nextPrayerName.uppercase(Locale.ROOT))
        views.setTextViewText(R.id.widget_target_time, formatTime(localTarget.hour, localTarget.minute, location.timeZoneId))
        setCountdown(
            views = views,
            chronometerId = R.id.widget_countdown_chronometer,
            targetEpochMillis = targetInstant.toEpochMilliseconds(),
        )
        setOpenAppAction(context, views, R.id.widget_compact_root)
        return views
    }

    fun buildDetailedWidget(
        context: Context,
        location: LocationInfo,
        nextPrayerName: String,
        targetInstant: Instant,
        record: PrayerTimeRecord,
        hijriDateFormatted: String? = null,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_detailed)
        val timeZone = TimeZone.of(location.timeZoneId)
        val localTarget = targetInstant.toLocalDateTime(timeZone)

        views.setTextViewText(R.id.widget_det_location, location.name)
        views.setTextViewText(R.id.widget_det_prayer_name, nextPrayerName.uppercase(Locale.ROOT))
        views.setTextViewText(
            R.id.widget_det_prayer_time,
            formatTime(localTarget.hour, localTarget.minute, location.timeZoneId),
        )
        if (hijriDateFormatted != null) {
            views.setTextViewText(R.id.widget_det_hijri_date, hijriDateFormatted)
            views.setViewVisibility(R.id.widget_det_hijri_date, View.VISIBLE)
        }
        setCountdown(
            views = views,
            chronometerId = R.id.widget_det_countdown,
            targetEpochMillis = targetInstant.toEpochMilliseconds(),
        )

        views.setTextViewText(R.id.widget_det_fajr, formatPrayerTime(record.fajr, timeZone))
        views.setTextViewText(R.id.widget_det_dhuhr, formatPrayerTime(record.dhuhr, timeZone))
        views.setTextViewText(R.id.widget_det_asr, formatPrayerTime(record.asr, timeZone))
        views.setTextViewText(R.id.widget_det_maghrib, formatPrayerTime(record.maghrib, timeZone))
        views.setTextViewText(R.id.widget_det_isha, formatPrayerTime(record.isha, timeZone))

        setOpenAppAction(context, views, R.id.widget_detailed_root)
        return views
    }

    /**
     * Renders an explicit unavailable state when the repository cannot provide a
     * snapshot. This prevents static preview text in the XML from reaching the
     * user's launcher as if it were current data.
     */
    fun buildUnavailableWidget(context: Context, detailed: Boolean): RemoteViews {
        val views = RemoteViews(
            context.packageName,
            if (detailed) R.layout.widget_detailed else R.layout.widget_compact,
        )

        if (detailed) {
            views.setTextViewText(R.id.widget_det_location, "Jadwal belum tersedia")
            views.setTextViewText(R.id.widget_det_prayer_name, "Data belum tersedia")
            views.setTextViewText(R.id.widget_det_prayer_time, "Data belum tersedia")
            views.setTextViewText(R.id.widget_det_fajr, "Data belum tersedia")
            views.setTextViewText(R.id.widget_det_dhuhr, "Data belum tersedia")
            views.setTextViewText(R.id.widget_det_asr, "Data belum tersedia")
            views.setTextViewText(R.id.widget_det_maghrib, "Data belum tersedia")
            views.setTextViewText(R.id.widget_det_isha, "Data belum tersedia")
            views.setViewVisibility(R.id.widget_det_countdown, View.GONE)
            setOpenAppAction(context, views, R.id.widget_detailed_root)
        } else {
            views.setTextViewText(R.id.widget_location_title, "Jadwal belum tersedia")
            views.setTextViewText(R.id.widget_next_prayer_name, "Data belum tersedia")
            views.setTextViewText(R.id.widget_target_time, "Data belum tersedia")
            views.setViewVisibility(R.id.widget_countdown_chronometer, View.GONE)
            setOpenAppAction(context, views, R.id.widget_compact_root)
        }

        return views
    }

    private fun setCountdown(
        views: RemoteViews,
        chronometerId: Int,
        targetEpochMillis: Long,
    ) {
        val remainingMillis = targetEpochMillis - System.currentTimeMillis()
        if (remainingMillis <= 0L) {
            views.setViewVisibility(chronometerId, View.GONE)
            return
        }

        val baseChronometerMillis = SystemClock.elapsedRealtime() + remainingMillis
        views.setChronometer(chronometerId, baseChronometerMillis, null, true)
        views.setViewVisibility(chronometerId, View.VISIBLE)
    }

    private fun setOpenAppAction(context: Context, views: RemoteViews, rootId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(rootId, pendingIntent)
    }

    private fun formatPrayerTime(instant: Instant, timeZone: TimeZone): String {
        val localTime = instant.toLocalDateTime(timeZone)
        return String.format(Locale.ROOT, "%02d:%02d", localTime.hour, localTime.minute)
    }

    private fun formatTime(hour: Int, minute: Int, timeZoneId: String): String {
        val suffix = when (timeZoneId) {
            "Asia/Jakarta", "Asia/Pontianak" -> "WIB"
            "Asia/Makassar", "Asia/Ujung_Pandang", "Asia/Bali" -> "WITA"
            "Asia/Jayapura" -> "WIT"
            "Asia/Riyadh" -> "AST"
            else -> ""
        }
        val time = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
        return if (suffix.isEmpty()) time else "$time $suffix"
    }
}
