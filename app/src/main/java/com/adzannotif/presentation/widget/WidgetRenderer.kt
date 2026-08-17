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
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
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
    private val background = dayNightColor(Color(0xFFF6F9F7), Color(0xFF131B18))
    private val surface = dayNightColor(Color(0xFFFFFFFF), Color(0xFF22302A))
    private val activeSurface = dayNightColor(Color(0xFFD6F5E8), Color(0xFF00513C))
    private val primaryText = dayNightColor(Color(0xFF191C1B), Color(0xFFE1E3DF))
    private val secondaryText = dayNightColor(Color(0xFF3F4945), Color(0xFFBFC9C3))
    private val accent = dayNightColor(Color(0xFF7A5900), Color(0xFFFAC248))

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
            .background(background)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .semantics { contentDescription = description }
            .clickable(actionStartActivity<MainActivity>())

        Box(modifier = rootModifier, contentAlignment = Alignment.Center) {
            if (snapshot.availability == PrayerWidgetAvailability.AVAILABLE) {
                if (size.width < 220.dp) CompactContent(snapshot) else DetailedContent(snapshot)
            } else {
                UnavailableContent()
            }
        }
    }

    @Composable
    private fun CompactContent(snapshot: PrayerWidgetSnapshot) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier.fillMaxSize(),
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
                style = TextStyle(color = primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold),
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
    private fun DetailedContent(snapshot: PrayerWidgetSnapshot) {
        val context = LocalContext.current
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = snapshot.locationName.orEmpty(),
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(
                    text = snapshot.timeZoneId.orEmpty(),
                    style = TextStyle(color = secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
            }
            Text(
                text = snapshot.hijriDate?.let { "${it.day} ${it.monthName} ${it.year} H" }
                    ?: context.getString(R.string.hijri_unavailable),
                style = TextStyle(color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(surface)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = prayerLabel(context, snapshot.nextPrayer),
                        style = TextStyle(color = primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Text(
                        text = formatTarget(snapshot.nextTargetEpochMillis, snapshot.timeZoneId),
                        style = TextStyle(color = secondaryText, fontSize = 10.sp),
                    )
                }
                Countdown(snapshot.nextTargetEpochMillis)
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                PrayerColumn(snapshot.prayerTimes.take(3), snapshot.timeZoneId)
                PrayerColumn(snapshot.prayerTimes.drop(3), snapshot.timeZoneId)
            }
        }
    }

    @Composable
    private fun RowScope.PrayerColumn(times: List<PrayerWidgetTime>, timeZoneId: String?) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            times.forEach { prayerTime -> PrayerRow(prayerTime, timeZoneId) }
        }
    }

    @Composable
    private fun PrayerRow(prayerTime: PrayerWidgetTime, timeZoneId: String?) {
        val context = LocalContext.current
        val rowModifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .then(
                if (prayerTime.isCurrent) {
                    GlanceModifier.background(activeSurface).cornerRadius(6.dp).padding(horizontal = 4.dp)
                } else {
                    GlanceModifier.padding(horizontal = 4.dp)
                },
            )
            .semantics {
                contentDescription = context.getString(
                    R.string.widget_prayer_time_description,
                    prayerLabel(context, prayerTime.prayer),
                    formatTarget(prayerTime.timeEpochMillis, timeZoneId),
                )
            }
        Row(modifier = rowModifier) {
            Text(
                text = prayerLabel(context, prayerTime.prayer),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = secondaryText, fontSize = 9.sp),
                maxLines = 1,
            )
            Text(
                text = formatTarget(prayerTime.timeEpochMillis, timeZoneId),
                style = TextStyle(
                    color = if (prayerTime.isCurrent) accent else primaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }

    @Composable
    private fun UnavailableContent() {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
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

    private fun countdownTextColor(context: Context): Int {
        val isNight = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (isNight) 0xFFFAC248.toInt() else 0xFF7A5900.toInt()
    }

    private fun dayNightColor(day: Color, night: Color): ColorProvider = object : ColorProvider {
        override fun getColor(context: Context): Color {
            val isNight = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            return if (isNight) night else day
        }
    }
}
