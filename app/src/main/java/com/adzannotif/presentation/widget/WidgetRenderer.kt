package com.adzannotif.presentation.widget

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.background
import androidx.glance.appwidget.cornerRadius
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
    private val navyHeader = ColorProvider(Color(0xFF072146))
    private val timetableBg = dayNightColor(Color(0xFFFFFFFF), Color(0xFF161E26))
    private val timetableBorder = dayNightColor(Color(0xFFE2E8F0), Color(0xFF2D3748))
    private val primaryText = dayNightColor(Color(0xFF1A202C), Color(0xFFF7FAFC))
    private val secondaryText = dayNightColor(Color(0xFF4A5568), Color(0xFFA0AEC0))
    private val highlightText = dayNightColor(Color(0xFF0E3A75), Color(0xFF63B3ED))
    private val accent = dayNightColor(Color(0xFF0F3E7D), Color(0xFF4299E1))

    @Composable
    fun Content(snapshot: PrayerWidgetSnapshot) {
        val size = LocalSize.current
        val context = LocalContext.current
        val description = if (snapshot.availability == PrayerWidgetAvailability.AVAILABLE) {
            context.getString(
                R.string.widget_content_description,
                snapshot.locationName ?: context.getString(R.string.location_unavailable),
                prayerLabel(context, snapshot.nextPrayer),
                formatTarget(snapshot.nextTargetEpochMillis, snapshot.timeZoneId),
            )
        } else {
            context.getString(R.string.prayer_data_unavailable)
        }
        val rootModifier = GlanceModifier
            .fillMaxSize()
            .background(timetableBg)
            .cornerRadius(16.dp)
            .semantics { contentDescription = description }
            .clickable(actionStartActivity<MainActivity>())

        Box(modifier = rootModifier, contentAlignment = Alignment.Center) {
            if (snapshot.availability == PrayerWidgetAvailability.AVAILABLE) {
                if (size.height >= 170.dp || size.width >= 180.dp) {
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
        val remainingText = formatRelativeRemaining(snapshot.nextTargetEpochMillis)
        val headerTitle = if (remainingText.isNotEmpty()) "$nextName $remainingText" else nextName

        Column(modifier = GlanceModifier.fillMaxSize()) {
            // 1. Navy Header Bar
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(navyHeader)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
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
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                    ),
                )
            }

            // 2. Timetable Prayer Rows
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                val items = if (snapshot.timetableItems.isNotEmpty()) {
                    snapshot.timetableItems
                } else {
                    snapshot.prayerTimes.map { pt ->
                        PrayerTimetableItem(
                            name = prayerLabel(context, pt.prayer),
                            timeEpochMillis = pt.timeEpochMillis,
                            isPassed = pt.timeEpochMillis <= System.currentTimeMillis(),
                            isNext = pt.isCurrent,
                        )
                    }
                }

                items.forEach { item ->
                    TimetableRow(item, snapshot.timeZoneId)
                }
            }

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
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
        }
    }

    @Composable
    private fun TimetableRow(item: PrayerTimetableItem, timeZoneId: String?) {
        val checkIcon = if (item.isPassed) "☑" else "○"
        val rowColor = if (item.isNext) highlightText else primaryText
        val fontWeight = if (item.isNext) FontWeight.Bold else FontWeight.Normal

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = checkIcon,
                style = TextStyle(
                    color = if (item.isPassed || item.isNext) accent else secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = item.name,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = rowColor,
                    fontSize = 12.sp,
                    fontWeight = fontWeight,
                ),
                maxLines = 1,
            )
            Text(
                text = formatTarget(item.timeEpochMillis, timeZoneId),
                style = TextStyle(
                    color = rowColor,
                    fontSize = 12.sp,
                    fontWeight = fontWeight,
                ),
            )
        }
    }

    @Composable
    private fun CompactContent(snapshot: PrayerWidgetSnapshot) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = snapshot.locationName.orEmpty(),
                style = TextStyle(color = secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                text = prayerLabel(context, snapshot.nextPrayer),
                style = TextStyle(color = highlightText, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                text = formatTarget(snapshot.nextTargetEpochMillis, snapshot.timeZoneId),
                style = TextStyle(color = secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
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
                style = TextStyle(color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.prayer_data_unavailable_detail),
                style = TextStyle(color = secondaryText, fontSize = 11.sp),
            )
        }
    }

    @Composable
    private fun Countdown(targetEpochMillis: Long?) {
        val context = LocalContext.current
        val target = targetEpochMillis ?: return
        val remainingMillis = target - System.currentTimeMillis()
        if (remainingMillis <= 0L) return
        val chronometer = RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
            setChronometer(
                R.id.chronometer,
                SystemClock.elapsedRealtime() + remainingMillis,
                "%s",
                true,
            )
            setTextColor(R.id.chronometer, countdownTextColor(context))
            setContentDescription(R.id.chronometer, context.getString(R.string.remaining_time))
        }
        AndroidRemoteViews(remoteViews = chronometer)
    }

    private fun prayerLabel(context: Context, prayer: Prayer?): String = when (prayer) {
        Prayer.IMSAK -> context.getString(R.string.prayer_imsak)
        Prayer.FAJR -> context.getString(R.string.prayer_fajr)
        Prayer.SUNRISE -> context.getString(R.string.prayer_sunrise)
        Prayer.DHUHR -> context.getString(R.string.prayer_dhuhr)
        Prayer.ASR -> context.getString(R.string.prayer_asr)
        Prayer.MAGHRIB -> context.getString(R.string.prayer_maghrib)
        Prayer.ISHA -> context.getString(R.string.prayer_isha)
        else -> context.getString(R.string.prayer_data_unavailable)
    }

    private fun formatTarget(epochMillis: Long?, timeZoneId: String?): String {
        if (epochMillis == null || timeZoneId == null) return "—"
        val timeZone = runCatching { TimeZone.of(timeZoneId) }.getOrNull() ?: return "—"
        val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        return String.format(Locale.ROOT, "%02d:%02d", local.hour, local.minute)
    }

    private fun formatRelativeRemaining(targetEpochMillis: Long?): String {
        if (targetEpochMillis == null) return ""
        val diffMillis = targetEpochMillis - System.currentTimeMillis()
        if (diffMillis <= 0) return ""
        val totalMinutes = diffMillis / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "±$hours jam lagi"
            else -> "±$minutes mnt lagi"
        }
    }

    private fun countdownTextColor(context: Context): Int {
        val isNight = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (isNight) 0xFF63B3ED.toInt() else 0xFF0E3A75.toInt()
    }

    private fun dayNightColor(day: Color, night: Color): ColorProvider = object : ColorProvider {
        override fun getColor(context: Context): Color {
            val isNight = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            return if (isNight) night else day
        }
    }
}
