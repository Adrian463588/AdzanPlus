package com.adzannotif.presentation.astronomy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.presentation.theme.AstronomyBackgroundDeep
import com.adzannotif.presentation.theme.AstronomyBlueHour
import com.adzannotif.presentation.theme.AstronomyConstellationLine
import com.adzannotif.presentation.theme.AstronomyGoldenHour
import com.adzannotif.presentation.theme.AstronomySunAmber
import com.adzannotif.presentation.theme.AstronomyTwilightAstro
import com.adzannotif.presentation.theme.AstronomyTwilightCivil
import com.adzannotif.presentation.theme.AstronomyTwilightNautical
import java.util.Calendar
import java.util.TimeZone

/**
 * Renders the actual solar event windows for the selected local date.
 * Missing events leave the relevant segment unavailable instead of drawing a
 * substituted day shape.
 */
@Composable
fun SolarEventTimeline(
    sunInfo: SunInfo?,
    timeZoneId: String?,
    modifier: Modifier = Modifier,
) {
    if (sunInfo == null) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.adzannotif.R.string.astro_timeline_unavailable),
            color = AstronomyTwilightCivil,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
        return
    }

    val timeZone = timeZoneId?.let(TimeZone::getTimeZone)
    if (timeZone == null) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.adzannotif.R.string.astro_timeline_timezone_unavailable),
            color = AstronomyTwilightCivil,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
        return
    }
    val referenceMillis = sunInfo.calculationEpochMillis
        .takeIf { it != 0L }
        ?: sunInfo.noonMillis
        ?: sunInfo.riseMillis
        ?: sunInfo.setMillis
        ?: return
    val localMidnight = Calendar.getInstance(timeZone).apply {
        timeInMillis = referenceMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val nextLocalMidnight = Calendar.getInstance(timeZone).apply {
        timeInMillis = localMidnight
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
    val localDayDuration = (nextLocalMidnight - localMidnight).coerceAtLeast(1L)

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.adzannotif.R.string.astro_timeline_title),
            color = AstronomyTwilightCivil,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val timelineDescription = androidx.compose.ui.res.stringResource(com.adzannotif.R.string.astro_timeline_description)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .semantics {
                    contentDescription = timelineDescription
                },
        ) {
            val width = size.width
            val height = size.height

            drawRect(
                color = AstronomyBackgroundDeep,
                topLeft = Offset.Zero,
                size = Size(width, height),
            )

            fun x(epochMillis: Long): Float =
                ((epochMillis - localMidnight).toDouble() / localDayDuration)
                    .toFloat()
                    .coerceIn(0f, 1f)

            fun drawSegment(start: Long?, end: Long?, color: Color) {
                if (start == null || end == null || end <= start) return
                val left = x(start)
                val right = x(end)
                if (right > left) {
                    drawRect(
                        color = color,
                        topLeft = Offset(width * left, 0f),
                        size = Size(width * (right - left), height),
                    )
                }
            }

            // Base daylight band; the twilight and photography windows are
            // layered on top using the values computed by :core-astronomy.
            drawSegment(sunInfo.riseMillis, sunInfo.setMillis, AstronomySunAmber)
            drawSegment(sunInfo.astronomicalDawnMillis, sunInfo.nauticalDawnMillis, AstronomyTwilightAstro)
            drawSegment(sunInfo.nauticalDawnMillis, sunInfo.civilDawnMillis, AstronomyTwilightNautical)
            drawSegment(sunInfo.civilDawnMillis, sunInfo.morningBlueHourStartMillis, AstronomyTwilightCivil)
            drawSegment(sunInfo.morningBlueHourStartMillis, sunInfo.morningBlueHourEndMillis, AstronomyBlueHour)
            drawSegment(sunInfo.morningGoldenHourStartMillis, sunInfo.morningGoldenHourEndMillis, AstronomyGoldenHour)
            drawSegment(sunInfo.eveningGoldenHourStartMillis, sunInfo.eveningGoldenHourEndMillis, AstronomyGoldenHour)
            drawSegment(sunInfo.eveningBlueHourStartMillis, sunInfo.eveningBlueHourEndMillis, AstronomyBlueHour)
            drawSegment(sunInfo.eveningBlueHourEndMillis, sunInfo.civilDuskMillis, AstronomyTwilightCivil)
            drawSegment(sunInfo.civilDuskMillis, sunInfo.nauticalDuskMillis, AstronomyTwilightNautical)
            drawSegment(sunInfo.nauticalDuskMillis, sunInfo.astronomicalDuskMillis, AstronomyTwilightAstro)

            val nowX = x(System.currentTimeMillis()) * width
            drawLine(
                color = Color.White,
                start = Offset(nowX, 0f),
                end = Offset(nowX, height),
                strokeWidth = 2f,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TimelineLabel("00:00")
            TimelineLabel("06:00")
            TimelineLabel("12:00")
            TimelineLabel("18:00")
            TimelineLabel("24:00")
        }
    }
}

@Composable
private fun TimelineLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = AstronomyConstellationLine,
    )
}
