package com.adzannotif.widget

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.adzannotif.R
import com.adzannotif.domain.model.astronomy.SunInfo
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun SunWidgetContent(sunInfo: SunInfo?, locationName: String, timeZoneId: String?) {
    val context = LocalContext.current

    fun formatWindow(start: Long?, end: Long?): String {
        if (start == null || end == null || timeZoneId == null) return "Data tidak tersedia"
        val fmt = SimpleDateFormat("HH:mm", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone(timeZoneId)
        }
        return "${fmt.format(Date(start))} - ${fmt.format(Date(end))}"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF0B1525))
            .padding(14.dp),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        if (sunInfo != null && timeZoneId != null) {
            Text(
                text = "☀️ ${sunInfo.currentPhase}",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "📍 $locationName",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFF9A3C)),
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))

            // Golden Hour Window
            val goldenMorning = formatWindow(sunInfo.morningGoldenHourStartMillis, sunInfo.morningGoldenHourEndMillis)
            val goldenEvening = formatWindow(sunInfo.eveningGoldenHourStartMillis, sunInfo.eveningGoldenHourEndMillis)
            Text(
                text = "Golden: $goldenMorning | $goldenEvening",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFB347)),
                    fontSize = 11.sp
                )
            )

            // Blue Hour Window
            val blueMorning = formatWindow(sunInfo.morningBlueHourStartMillis, sunInfo.morningBlueHourEndMillis)
            val blueEvening = formatWindow(sunInfo.eveningBlueHourStartMillis, sunInfo.eveningBlueHourEndMillis)
            Text(
                text = "Blue: $blueMorning | $blueEvening",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF5B8FD4)),
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            val now = System.currentTimeMillis()
            val nextEventMillis = listOfNotNull(
                sunInfo.morningBlueHourStartMillis,
                sunInfo.morningGoldenHourStartMillis,
                sunInfo.riseMillis,
                sunInfo.eveningGoldenHourStartMillis,
                sunInfo.eveningBlueHourStartMillis,
                sunInfo.setMillis
            ).filter { it > now }.minOrNull()

            val eventName = when (nextEventMillis) {
                sunInfo.morningBlueHourStartMillis -> "Blue Hour Pagi"
                sunInfo.morningGoldenHourStartMillis -> "Golden Hour Pagi"
                sunInfo.riseMillis -> "Matahari Terbit"
                sunInfo.eveningGoldenHourStartMillis -> "Golden Hour Sore"
                sunInfo.eveningBlueHourStartMillis -> "Blue Hour Sore"
                sunInfo.setMillis -> "Matahari Terbenam"
                else -> "Acara surya tidak tersedia"
            }

            if (nextEventMillis != null) {
                Text(
                    text = "$eventName:",
                    style = TextStyle(color = ColorProvider(Color(0xFFE1E3DF)), fontSize = 12.sp)
                )
                val baseChronometerMillis = SystemClock.elapsedRealtime() + (nextEventMillis - System.currentTimeMillis())
                AndroidRemoteViews(
                    remoteViews = RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
                        setChronometer(
                            R.id.chronometer,
                            baseChronometerMillis,
                            "%s",
                            true
                        )
                    }
                )
            } else {
                Text(
                    text = "Acara surya berikutnya tidak tersedia",
                    style = TextStyle(color = ColorProvider(Color(0xFFAAAAAA)), fontSize = 12.sp)
                )
            }
        } else {
            Text(
                text = "☀️ Data surya tidak tersedia",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "📍 $locationName",
                style = TextStyle(color = ColorProvider(Color(0xFFFF9A3C)), fontSize = 11.sp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Golden Hour: Data tidak tersedia",
                style = TextStyle(color = ColorProvider(Color(0xFFFFB347)), fontSize = 11.sp)
            )
        }
    }
}

class SunWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val locationRepository = entryPoint.locationRepository()
        val astronomyRepository = entryPoint.astronomyRepository()

        val location = locationRepository.currentOrSelectedLocation.first()
        if (location == null) {
            provideContent {
                SunWidgetContent(
                    sunInfo = null,
                    locationName = "Lokasi belum tersedia",
                    timeZoneId = null,
                )
            }
            return
        }
        val epochMillis = System.currentTimeMillis()
        val sunInfo = runCatching {
            astronomyRepository.getSunInfo(
                location,
                epochMillis,
            ).first()
        }.onFailure { error ->
            Log.w("SunWidget", "Sun data is unavailable for widget update", error)
        }.getOrNull()

        provideContent {
        SunWidgetContent(
            sunInfo = sunInfo,
            locationName = location.name,
            timeZoneId = location.timeZoneId,
        )
        }
    }
}
