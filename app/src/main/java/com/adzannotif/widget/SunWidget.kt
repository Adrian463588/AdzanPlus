package com.adzannotif.widget

import android.content.Context
import android.os.SystemClock
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

@Composable
fun SunWidgetContent(sunInfo: SunInfo?) {
    val context = LocalContext.current
    
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF0B1525))
            .padding(16.dp),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        if (sunInfo != null) {
            Text(
                text = "☀️ ${sunInfo.currentPhase}",
                style = TextStyle(
                    color = ColorProvider(Color.White), 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            // For SunWidget, let's just pick golden hour or next event.
            // If we are before morning golden hour, show that.
            val now = System.currentTimeMillis()
            val nextEventMillis = listOfNotNull(
                sunInfo.morningGoldenHourStartMillis,
                sunInfo.eveningGoldenHourStartMillis,
                sunInfo.riseMillis,
                sunInfo.setMillis
            ).filter { it > now }.minOrNull()
            
            val eventName = when (nextEventMillis) {
                sunInfo.morningGoldenHourStartMillis -> "Morning Golden Hour"
                sunInfo.eveningGoldenHourStartMillis -> "Evening Golden Hour"
                sunInfo.riseMillis -> "Sunrise"
                sunInfo.setMillis -> "Sunset"
                else -> "Next Event"
            }

            if (nextEventMillis != null) {
                Text(
                    text = eventName,
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp)
                )
                Text(
                    text = "Mulai dalam",
                    style = TextStyle(color = ColorProvider(Color(0xFFAAAAAA)), fontSize = 12.sp)
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
                    text = "No Upcoming Event Today",
                    style = TextStyle(color = ColorProvider(Color(0xFFAAAAAA)), fontSize = 14.sp)
                )
            }
        } else {
            Text(
                text = "☀️ --", 
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Next Event: --", 
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp)
            )
            Text(
                text = "Mulai dalam",
                style = TextStyle(color = ColorProvider(Color(0xFFAAAAAA)), fontSize = 12.sp)
            )
            Text(
                text = "--",
                style = TextStyle(color = ColorProvider(Color(0xFFFAC248)), fontSize = 20.sp)
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
        val epochMillis = System.currentTimeMillis()
        val sunInfo = astronomyRepository.getSunInfo(
            location.latitude,
            location.longitude,
            epochMillis
        ).first()

        provideContent {
            SunWidgetContent(sunInfo = sunInfo)
        }
    }
}
