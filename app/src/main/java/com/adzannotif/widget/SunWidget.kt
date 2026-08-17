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
import androidx.glance.background
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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

@Composable
fun SunWidgetContent(sunInfo: SunInfo?, locationName: String, timeZoneId: String?) {
    val context = LocalContext.current
    val wide = LocalSize.current.width >= 220.dp
    val routeAction = actionStartActivity(
        CelestialWidgetRoute.intent(context, Screen.SunDetail.route),
    )
    val widgetDescription = if (sunInfo != null && timeZoneId != null) {
        context.getString(
            R.string.sun_widget_content_description,
            sunInfo.currentPhase,
            sunInfo.nextEventName ?: context.getString(R.string.sun_event_unavailable),
            locationName,
        )
    } else {
        context.getString(R.string.sun_widget_data_unavailable)
    }
    val modifier = GlanceModifier
        .fillMaxSize()
        .background(AstronomyWidgetPalette.sunBackground)
        .padding(if (wide) 12.dp else 10.dp)
        .semantics { contentDescription = widgetDescription }
        .clickable(routeAction)

    if (sunInfo == null || timeZoneId == null) {
        Column(modifier = modifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
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
        return
    }

    Column(modifier = modifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = context.getString(R.string.sun_widget_icon),
                style = TextStyle(fontSize = if (wide) 21.sp else 18.sp),
            )
            Spacer(GlanceModifier.width(if (wide) 8.dp else 5.dp))
            Text(
                text = sunInfo.currentPhase,
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
        val nextMillis = sunInfo.nextEventMillis?.takeIf { it > System.currentTimeMillis() }
        Text(
            text = if (nextMillis != null) {
                context.getString(
                    R.string.sun_event_time,
                    sunInfo.nextEventName ?: context.getString(R.string.sun_event_default),
                    formatTime(nextMillis, timeZoneId),
                )
            } else {
                context.getString(R.string.sun_event_unavailable)
            },
            style = TextStyle(AstronomyWidgetPalette.sunAccent, fontSize = if (wide) 11.sp else 10.sp),
            maxLines = 1,
        )
        if (nextMillis != null) Countdown(context, nextMillis)
    }
}

private fun formatAltitude(context: Context, value: Double): String =
    context.getString(R.string.sun_widget_altitude, value)

private fun formatTime(epochMillis: Long, timeZoneId: String): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.of(timeZoneId))
    return String.format(Locale.ROOT, "%02d:%02d", local.hour, local.minute)
}

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
        provideContent {
            SunWidgetContent(
                sunInfo = sunInfo,
                locationName = location?.name ?: context.getString(R.string.location_unavailable),
                timeZoneId = location?.timeZoneId,
            )
        }
    }
}
