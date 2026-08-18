package com.adzannotif.presentation.widget

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.background
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.adzannotif.MainActivity
import com.adzannotif.R
import com.adzannotif.core.prayer.Prayer
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale


internal object WidgetRenderer {
    private val navyHeader = dayNight(Color(0xFF0D3B66), Color(0xFF0A2540))
    private val timetableBg = dayNight(Color(0xFFFFFFFF), Color(0xFF161E26))
    private val timetableBorder = dayNight(Color(0xFFE2E8F0), Color(0xFF2D3748))
    private val primaryText = dayNight(Color(0xFF1A202C), Color(0xFFF7FAFC))
    private val secondaryText = dayNight(Color(0xFF4A5568), Color(0xFFA0AEC0))
    private val highlightText = dayNight(Color(0xFF0D3B66), Color(0xFF63B3ED))
    private val passedCheckColor = dayNight(Color(0xFF0D3B66), Color(0xFF63B3ED))

    private fun dayNight(day: Color, night: Color): ColorProvider = DayNightColorProvider(day, night)

    @Composable
    fun Content(snapshot: PrayerWidgetSnapshot) {
        val size = LocalSize.current
        val context = LocalContext.current
        val isNight = (LocalConfiguration.current.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val surfaceColor = if (isNight) Color(0xFF161E26) else Color.White
        val description = if (snapshot.availability == PrayerWidgetAvailability.AVAILABLE) {
            context.getString(
                R.string.widget_content_description,
                snapshot.locationName ?: context.getString(R.string.location_unavailable),
                prayerLabel(context, snapshot.nextPrayer),
                formatTarget(context, snapshot.nextTargetEpochMillis, snapshot.timeZoneId),
            )
        } else {
            context.getString(R.string.prayer_data_unavailable)
        }
        val rootModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(timetableBg)
            .cornerRadius(16.dp)
            .semantics { contentDescription = description }
            .clickable(actionStartActivity<MainActivity>())

        Box(
            modifier = rootModifier,
            contentAlignment = Alignment.Center,
        ) {
            if (snapshot.availability == PrayerWidgetAvailability.AVAILABLE) {
                // The timetable needs vertical room for all eight real entries.
                // A wide-but-short launcher slot (for example 4x2) must stay
                // compact instead of rendering rows that the host will clip.
                if (size.height >= 140.dp) {
                    Timetable3x4Content(snapshot)
                } else {
                    CompactContent(snapshot)
                }
            } else {
                UnavailableContent()
            }
        }
    }

    @Composable
    private fun Timetable3x4Content(snapshot: PrayerWidgetSnapshot) {
        val context = LocalContext.current
        val nextName = prayerLabel(context, snapshot.nextPrayer)

        Column(modifier = GlanceModifier.fillMaxSize()) {
            // 1. Navy Header Bar
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(navyHeader)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                val headerTitle = formatNextPrayer(context, nextName, snapshot.nextTargetEpochMillis)

                Text(
                    text = headerTitle,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = "⚙",
                    modifier = GlanceModifier
                        .padding(8.dp)
                        .semantics {
                            contentDescription = context.getString(R.string.widget_open_settings)
                        }
                        .clickable(actionStartActivityIntent(PrayerWidgetRoute.settingsIntent(context))),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                    ),
                )
            }

            // 2. Timetable Prayer Rows (All 8 Prayers)
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.Vertical.Top,
            ) {
                snapshot.timetableItems.forEach { item ->
                    TimetableRow(item, snapshot.timeZoneId)
                }
            }

            // Divider Line
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(timetableBorder)
            ) {}

            // 3. Hijri Date Footer
            val hijriText = snapshot.hijriDate?.let { "${String.format(Locale.ROOT, "%02d", it.day)} ${it.monthName} ${it.year} H" }
                ?: context.getString(R.string.hijri_unavailable)
            
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(timetableBg)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = hijriText,
                    style = TextStyle(
                        color = secondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
        }
    }

    @Composable
    private fun TimetableRow(item: PrayerTimetableItem, timeZoneId: String?) {
        val context = LocalContext.current
        val checkIcon = if (item.isPassed) "✓" else "○"
        val highlighted = item.isCurrent || item.isNext
        val rowColor = if (highlighted) highlightText else primaryText
        val fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
        val iconColor = if (item.isPassed) passedCheckColor else if (highlighted) highlightText else secondaryText

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = checkIcon,
                style = TextStyle(
                    color = iconColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.width(10.dp))
            Text(
                text = timetableEntryLabel(context, item.entry),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = rowColor,
                    fontSize = 12.sp,
                    fontWeight = fontWeight,
                ),
                maxLines = 1,
            )
            Text(
                text = formatTarget(context, item.timeEpochMillis, timeZoneId),
                style = TextStyle(
                    color = rowColor,
                    fontSize = 12.sp,
                    fontWeight = fontWeight,
                ),
            )
        }
    }

    private fun timetableEntryLabel(context: Context, entry: PrayerWidgetTimetableEntry): String = when (entry) {
        PrayerWidgetTimetableEntry.IMSAK -> context.getString(R.string.prayer_imsak)
        PrayerWidgetTimetableEntry.FAJR -> context.getString(R.string.prayer_fajr)
        PrayerWidgetTimetableEntry.SUNRISE -> context.getString(R.string.prayer_sunrise)
        PrayerWidgetTimetableEntry.DHUHA -> context.getString(R.string.prayer_dhuha)
        PrayerWidgetTimetableEntry.DHUHR -> context.getString(R.string.prayer_dhuhr)
        PrayerWidgetTimetableEntry.ASR -> context.getString(R.string.prayer_asr)
        PrayerWidgetTimetableEntry.MAGHRIB -> context.getString(R.string.prayer_maghrib)
        PrayerWidgetTimetableEntry.ISHA -> context.getString(R.string.prayer_isha)
    }

    @Composable
    private fun CompactContent(snapshot: PrayerWidgetSnapshot) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = snapshot.locationName.orEmpty(),
                style = TextStyle(color = secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = prayerLabel(context, snapshot.nextPrayer),
                style = TextStyle(color = highlightText, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                text = formatTarget(context, snapshot.nextTargetEpochMillis, snapshot.timeZoneId),
                style = TextStyle(color = primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Countdown(snapshot.nextTargetEpochMillis)
        }
    }

    @Composable
    private fun UnavailableContent() {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            Text(
                text = context.getString(R.string.prayer_data_unavailable),
                style = TextStyle(color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.prayer_data_unavailable_detail),
                style = TextStyle(color = secondaryText, fontSize = 10.sp),
                maxLines = 2,
            )
        }
    }

    @Composable
    private fun Countdown(targetEpochMillis: Long?) {
        if (targetEpochMillis == null) return
        val remainingMillis = targetEpochMillis - System.currentTimeMillis()
        if (remainingMillis <= 0) return

        val context = LocalContext.current
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
            setChronometer(
                R.id.chronometer,
                SystemClock.elapsedRealtime() + remainingMillis,
                "%s",
                true,
            )
            setTextColor(R.id.chronometer, countdownTextColor(context))
            setContentDescription(R.id.chronometer, context.getString(R.string.widget_countdown_description))
        }
        AndroidRemoteViews(remoteViews = remoteViews)
    }

    private fun prayerLabel(context: Context, prayer: Prayer?): String = when (prayer) {
        Prayer.IMSAK -> context.getString(R.string.prayer_imsak)
        Prayer.FAJR -> context.getString(R.string.prayer_fajr)
        Prayer.SUNRISE -> context.getString(R.string.prayer_sunrise)
        Prayer.DHUHR -> context.getString(R.string.prayer_dhuhr)
        Prayer.ASR -> context.getString(R.string.prayer_asr)
        Prayer.MAGHRIB -> context.getString(R.string.prayer_maghrib)
        Prayer.ISHA -> context.getString(R.string.prayer_isha)
        Prayer.MIDNIGHT -> context.getString(R.string.prayer_midnight)
        Prayer.TAHAJJUD -> context.getString(R.string.prayer_tahajjud)
        null -> context.getString(R.string.value_unavailable)
    }

    private fun formatTarget(context: Context, epochMillis: Long?, timeZoneId: String?): String {
        if (epochMillis == null || timeZoneId == null) return context.getString(R.string.value_unavailable)
        val timeZone = runCatching { TimeZone.of(timeZoneId) }.getOrNull()
            ?: return context.getString(R.string.value_unavailable)
        val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        return String.format(Locale.ROOT, "%02d:%02d", local.hour, local.minute)
    }

    private fun formatNextPrayer(context: Context, prayerName: String, targetEpochMillis: Long?): String {
        if (targetEpochMillis == null) return prayerName
        val diffMillis = targetEpochMillis - System.currentTimeMillis()
        if (diffMillis <= 0) return prayerName
        val totalMinutes = diffMillis / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> context.getString(
                R.string.widget_next_prayer_hours,
                prayerName,
                hours.toInt(),
            )
            else -> context.getString(
                R.string.widget_next_prayer_minutes,
                prayerName,
                minutes.toInt(),
            )
        }
    }

    private fun countdownTextColor(context: Context): Int {
        val isNight = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (isNight) 0xFF63B3ED.toInt() else 0xFF0D3B66.toInt()
    }
}
