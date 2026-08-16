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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.adzannotif.R
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.repository.AstronomyRepository
import com.adzannotif.domain.repository.LocationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@Composable
fun MoonWidgetContent(moonInfo: MoonInfo?, locationName: String = "Jakarta") {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF111D30))
            .padding(14.dp),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        if (moonInfo != null) {
            val emoji = when (moonInfo.phaseOrdinal) {
                0 -> "🌑"
                1 -> "🌒"
                2 -> "🌓"
                3 -> "🌔"
                4 -> "🌕"
                5 -> "🌖"
                6 -> "🌗"
                7 -> "🌘"
                else -> "🌑"
            }
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = "$emoji ${moonInfo.phaseName}",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "📍 $locationName",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFAC248)),
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            val dist = String.format("%.0f km", moonInfo.distanceKm)
            val illum = String.format("%.1f%%", moonInfo.illuminationPercent)
            Text(
                text = "Iluminasi: $illum • Jarak: $dist",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFAAAAAA)),
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(6.dp))

            val moonriseMillis = moonInfo.riseMillis
            if (moonriseMillis != null) {
                Text(
                    text = "Bulan Terbit (Moonrise):",
                    style = TextStyle(color = ColorProvider(Color(0xFFE1E3DF)), fontSize = 12.sp)
                )
                val baseChronometerMillis = SystemClock.elapsedRealtime() + (moonriseMillis - System.currentTimeMillis())
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
                    text = "Tidak Ada Terbit Hari Ini",
                    style = TextStyle(color = ColorProvider(Color(0xFFAAAAAA)), fontSize = 12.sp)
                )
            }
        } else {
            Text(
                text = "🌑 Memuat Bulan...",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "📍 $locationName",
                style = TextStyle(color = ColorProvider(Color(0xFFFAC248)), fontSize = 11.sp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Iluminasi: -- • Jarak: --",
                style = TextStyle(color = ColorProvider(Color(0xFFAAAAAA)), fontSize = 11.sp)
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun astronomyRepository(): AstronomyRepository
    fun locationRepository(): LocationRepository
}

class MoonWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val locationRepository = entryPoint.locationRepository()
        val astronomyRepository = entryPoint.astronomyRepository()

        val location = locationRepository.currentOrSelectedLocation.first()
        val epochMillis = System.currentTimeMillis()
        val moonInfo = runCatching {
            astronomyRepository.getMoonInfo(
                location.latitude,
                location.longitude,
                epochMillis,
            ).first()
        }.onFailure { error ->
            Log.w("MoonWidget", "Moon data is unavailable for widget update", error)
        }.getOrNull()

        provideContent {
        MoonWidgetContent(moonInfo = moonInfo, locationName = location.name)
        }
    }
}
