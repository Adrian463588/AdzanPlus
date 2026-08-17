package com.adzannotif.widget

import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
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
import androidx.glance.text.TextStyle
import com.adzannotif.R
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.domain.repository.AstronomyRepository
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.presentation.common.Screen
import com.adzannotif.presentation.localization.astronomyEventLabel
import com.adzannotif.presentation.localization.solarPhaseLabel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar
import java.util.Locale

import androidx.glance.appwidget.components.Scaffold

@Composable
fun SunWidgetContent(
    sunInfo: SunInfo?,
    nextDaySunInfo: SunInfo?,
    locationName: String,
    timeZoneId: String?,
) {
    val context = LocalContext.current
    val wide = LocalSize.current.width >= 220.dp
    val routeAction = actionStartActivity(
        CelestialWidgetRoute.intent(context, Screen.SunDetail.route),
    )
    val widgetDescription = if (sunInfo != null && timeZoneId != null) {
        context.getString(
            R.string.sun_widget_content_description,
            solarPhaseLabel(context, sunInfo.currentPhase),
            astronomyEventLabel(context, sunInfo.nextEventName),
            locationName,
        )
    } else {
        context.getString(R.string.sun_widget_data_unavailable)
    }
    val modifier = GlanceModifier
        .fillMaxSize()
        .semantics { contentDescription = widgetDescription }
        .clickable(routeAction)
    val contentModifier = GlanceModifier
        .fillMaxSize()
        .padding(if (wide) 14.dp else 12.dp)

    Scaffold(
        modifier = modifier,
        backgroundColor = AstronomyWidgetPalette.sunSurface(context),
        horizontalPadding = 0.dp,
    ) {
        if (sunInfo == null || timeZoneId == null) {
            Column(modifier = contentModifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = context.getString(R.string.sun_widget_data_unavailable),
                    style = TextStyle(AstronomyWidgetPalette.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = context.getString(R.string.sun_widget_location_hint),
                    style = TextStyle(AstronomyWidgetPalette.secondaryText, fontSize = 10.sp),
                    maxLines = 2,
                )
            }
        } else {
            Column(modifier = contentModifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = context.getString(R.string.sun_widget_icon),
                style = TextStyle(fontSize = if (wide) 21.sp else 18.sp),
            )
            Spacer(GlanceModifier.width(if (wide) 8.dp else 5.dp))
            Text(
                text = solarPhaseLabel(context, sunInfo.currentPhase),
                style = TextStyle(
                    AstronomyWidgetPalette.primaryText,
                    fontSize = if (wide) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        if (wide) {
            Spacer(GlanceModifier.height(3.dp))
            Text(
                text = context.getString(
                    R.string.sun_widget_location_altitude,
                    locationName,
                    formatAltitude(context, sunInfo.altitude),
                ),
                style = TextStyle(AstronomyWidgetPalette.secondaryText, fontSize = 10.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(if (wide) 5.dp else 4.dp))
        val now = System.currentTimeMillis()
        val nextMillis = sunInfo.nextEventMillis?.takeIf { it > now }
        
        val sunInfos = listOfNotNull(sunInfo, nextDaySunInfo)
        val goldenWindow = nextLightWindow(
            now = now,
            timeZoneId = timeZoneId,
            windows = sunInfos.flatMap { info ->
                listOf(
                    info.morningGoldenHourStartMillis to info.morningGoldenHourEndMillis,
                    info.eveningGoldenHourStartMillis to info.eveningGoldenHourEndMillis,
                )
            },
        )
        val blueWindow = nextLightWindow(
            now = now,
            timeZoneId = timeZoneId,
            windows = sunInfos.flatMap { info ->
                listOf(
                    info.morningBlueHourStartMillis to info.morningBlueHourEndMillis,
                    info.eveningBlueHourStartMillis to info.eveningBlueHourEndMillis,
                )
            },
        )

        Text(
            text = if (nextMillis != null) {
                context.getString(
                    R.string.sun_event_time,
                    astronomyEventLabel(context, sunInfo.nextEventName),
                    formatTime(nextMillis, timeZoneId),
                )
            } else {
                context.getString(R.string.sun_event_unavailable)
            },
            style = TextStyle(AstronomyWidgetPalette.sunAccent, fontSize = if (wide) 11.sp else 10.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )

        if (wide) {
            goldenWindow?.let { window ->
                Text(
                    text = context.getString(R.string.sun_widget_golden_hour, window),
                    style = TextStyle(AstronomyWidgetPalette.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
            }
            blueWindow?.let { window ->
                Text(
                    text = context.getString(R.string.sun_widget_blue_hour, window),
                    style = TextStyle(AstronomyWidgetPalette.secondaryText, fontSize = 9.sp),
                    maxLines = 1,
                )
            }
        } else {
            val compactWindow = blueWindow ?: goldenWindow
            Text(
                text = compactWindow?.let { window ->
                    context.getString(
                        if (blueWindow != null) R.string.sun_widget_blue_hour else R.string.sun_widget_golden_hour,
                        window,
                    )
                } ?: context.getString(R.string.sun_widget_light_unavailable),
                style = TextStyle(AstronomyWidgetPalette.secondaryText, fontSize = 9.sp),
                maxLines = 1,
            )
        }

        if (nextMillis != null) Countdown(context, nextMillis)
        }
    }
}
}

private fun formatAltitude(context: Context, value: Double): String =
    context.getString(R.string.sun_widget_altitude, value)

private fun formatTime(epochMillis: Long, timeZoneId: String): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.of(timeZoneId))
    return String.format(Locale.ROOT, "%02d:%02d", local.hour, local.minute)
}

private fun formatWindow(startMillis: Long?, endMillis: Long?, timeZoneId: String): String? {
    if (startMillis == null || endMillis == null || endMillis <= startMillis) return null
    return runCatching {
        "${formatTime(startMillis, timeZoneId)}–${formatTime(endMillis, timeZoneId)}"
    }.getOrNull()
}

private fun nextLightWindow(
    now: Long,
    timeZoneId: String,
    windows: List<Pair<Long?, Long?>>,
): String? = windows
    .mapNotNull { (start, end) ->
        if (start == null || end == null || end <= start || end <= now) {
            null
        } else {
            start to end
        }
    }
    .minByOrNull { (start, _) -> start }
    ?.let { (start, end) -> formatWindow(start, end, timeZoneId) }

@Composable
private fun Countdown(context: Context, targetMillis: Long) {
    val remaining = targetMillis - System.currentTimeMillis()
    if (remaining <= 0L) return
    val views = RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
        setChronometer(R.id.chronometer, SystemClock.elapsedRealtime() + remaining, "%s", true)
        setContentDescription(R.id.chronometer, context.getString(R.string.sun_event_countdown_description))
    }
    AndroidRemoteViews(remoteViews = views)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SunWidgetEntryPoint {
    fun astronomyRepository(): AstronomyRepository
    fun locationRepository(): LocationRepository
}

class SunWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SunWidgetEntryPoint::class.java,
        )
        val location = entryPoint.locationRepository().currentOrSelectedLocation.first()
        val sunInfo = location?.let {
            runCatching {
                entryPoint.astronomyRepository().getSunInfo(it, System.currentTimeMillis()).first()
            }.getOrNull()
        }
        val nextDaySunInfo = location?.let { selectedLocation ->
            runCatching {
                val nextDayStart = Calendar.getInstance(
                    java.util.TimeZone.getTimeZone(selectedLocation.timeZoneId),
                ).apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
                entryPoint.astronomyRepository().getSunInfo(selectedLocation, nextDayStart).first()
            }.getOrNull()
        }
        provideContent {
            SunWidgetContent(
                sunInfo = sunInfo,
                nextDaySunInfo = nextDaySunInfo,
                locationName = location?.name ?: context.getString(R.string.location_unavailable),
                timeZoneId = location?.timeZoneId,
            )
        }
    }
}
