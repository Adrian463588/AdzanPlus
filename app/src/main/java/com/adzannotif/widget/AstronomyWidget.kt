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
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.adzannotif.R
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.domain.repository.AstronomyRepository
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.presentation.common.Screen
import com.adzannotif.presentation.localization.astronomyEventLabel
import com.adzannotif.presentation.localization.moonPhaseLabel
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

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AstronomyWidgetEntryPoint {
    fun locationRepository(): LocationRepository
    fun astronomyRepository(): AstronomyRepository
}

data class AstronomyWidgetSnapshot(
    val sunInfo: SunInfo?,
    val nextDaySunInfo: SunInfo?,
    val moonInfo: MoonInfo?,
    val nextDayMoonInfo: MoonInfo?,
    val locationName: String,
    val timeZoneId: String?,
)

class AstronomyWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(200.dp, 110.dp),
            DpSize(250.dp, 130.dp),
            DpSize(220.dp, 200.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AstronomyWidgetEntryPoint::class.java,
        )
        val location = entryPoint.locationRepository().currentOrSelectedLocation.first()
        val snapshot = if (location != null) {
            val nowMillis = System.currentTimeMillis()
            val nextDayMillis = nowMillis + (24 * 3600 * 1000L)

            val sunInfo = entryPoint.astronomyRepository().getSunInfo(location, nowMillis).first()
            val nextDaySunInfo = entryPoint.astronomyRepository().getSunInfo(location, nextDayMillis).first()
            val moonInfo = entryPoint.astronomyRepository().getMoonInfo(location, nowMillis).first()
            val nextDayMoonInfo = entryPoint.astronomyRepository().getMoonInfo(location, nextDayMillis).first()

            AstronomyWidgetSnapshot(
                sunInfo = sunInfo,
                nextDaySunInfo = nextDaySunInfo,
                moonInfo = moonInfo,
                nextDayMoonInfo = nextDayMoonInfo,
                locationName = location.name,
                timeZoneId = location.timeZoneId,
            )
        } else {
            AstronomyWidgetSnapshot(
                sunInfo = null,
                nextDaySunInfo = null,
                moonInfo = null,
                nextDayMoonInfo = null,
                locationName = "",
                timeZoneId = null,
            )
        }

        provideContent {
            AstronomyWidgetContent(snapshot)
        }
    }
}

@Composable
fun AstronomyWidgetContent(snapshot: AstronomyWidgetSnapshot) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isWide = size.width >= 190.dp
    val isTall = size.height >= 180.dp

    val dashboardAction = actionStartActivity(
        CelestialWidgetRoute.intent(context, Screen.AstronomyDashboard.route),
    )
    val sunAction = actionStartActivity(
        CelestialWidgetRoute.intent(context, Screen.SunDetail.route),
    )
    val moonAction = actionStartActivity(
        CelestialWidgetRoute.intent(context, Screen.MoonDetail.route),
    )

    val widgetDescription = if (snapshot.sunInfo != null && snapshot.moonInfo != null && snapshot.timeZoneId != null) {
        context.getString(
            R.string.astronomy_widget_content_description,
            solarPhaseLabel(context, snapshot.sunInfo.currentPhase),
            moonPhaseLabel(context, snapshot.moonInfo.phaseName),
            formatPercent(snapshot.moonInfo.illuminationPercent),
            snapshot.locationName,
        )
    } else {
        context.getString(R.string.astronomy_widget_data_unavailable)
    }

    val rootModifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(AstronomyWidgetPalette.astronomySurface(context))
        .cornerRadius(16.dp)
        .semantics { contentDescription = widgetDescription }
        .clickable(dashboardAction)

    Box(
        modifier = rootModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (snapshot.sunInfo == null || snapshot.moonInfo == null || snapshot.timeZoneId == null) {
            UnavailableContent()
        } else {
            when {
                isTall -> ExpandedContent(snapshot, sunAction, moonAction)
                isWide -> WideContent(snapshot, sunAction, moonAction)
                else -> CompactContent(snapshot, sunAction, moonAction)
            }
        }
    }
}

@Composable
private fun CompactContent(
    snapshot: AstronomyWidgetSnapshot,
    sunAction: androidx.glance.action.Action,
    moonAction: androidx.glance.action.Action,
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val sunInfo = snapshot.sunInfo!!
    val moonInfo = snapshot.moonInfo!!
    val timeZoneId = snapshot.timeZoneId!!

    val sunInfos = listOfNotNull(sunInfo, snapshot.nextDaySunInfo)
    val goldenWindow = nextLightWindow(
        now = now,
        timeZoneId = timeZoneId,
        windows = sunInfos.flatMap {
            listOf(
                it.morningGoldenHourStartMillis to it.morningGoldenHourEndMillis,
                it.eveningGoldenHourStartMillis to it.eveningGoldenHourEndMillis,
            )
        },
    )
    val blueWindow = nextLightWindow(
        now = now,
        timeZoneId = timeZoneId,
        windows = sunInfos.flatMap {
            listOf(
                it.morningBlueHourStartMillis to it.morningBlueHourEndMillis,
                it.eveningBlueHourStartMillis to it.eveningBlueHourEndMillis,
            )
        },
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(10.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        // Sun Mini Card (Top)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(sunAction),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = sunGlyph(sunInfo.currentPhase),
                style = TextStyle(fontSize = 18.sp),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = solarPhaseLabel(context, sunInfo.currentPhase),
                    style = TextStyle(
                        color = AstronomyWidgetPalette.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                val lightText = goldenWindow?.let { "Golden: $it" }
                    ?: blueWindow?.let { "Blue: $it" }
                    ?: if (sunInfo.altitude > 0) "Siang" else "Malam"
                Text(
                    text = lightText,
                    style = TextStyle(
                        color = AstronomyWidgetPalette.goldenHourColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }

        // Horizontal Divider
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AstronomyWidgetPalette.dividerColor),
        ) {}

        // Moon Mini Card (Bottom)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(moonAction),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = phaseGlyph(moonInfo.phaseOrdinal),
                style = TextStyle(fontSize = 18.sp),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "${formatPercent(moonInfo.illuminationPercent)} • ${moonPhaseLabel(context, moonInfo.phaseName)}",
                    style = TextStyle(
                        color = AstronomyWidgetPalette.primaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                val moonEventText = moonInfo.riseMillis?.takeIf { it > now }?.let { "Terbit ${formatTime(it, timeZoneId)}" }
                    ?: moonInfo.setMillis?.takeIf { it > now }?.let { "Terbenam ${formatTime(it, timeZoneId)}" }
                    ?: "Bulan hari ini"
                Text(
                    text = moonEventText,
                    style = TextStyle(
                        color = AstronomyWidgetPalette.moonAccent,
                        fontSize = 10.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WideContent(
    snapshot: AstronomyWidgetSnapshot,
    sunAction: androidx.glance.action.Action,
    moonAction: androidx.glance.action.Action,
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val sunInfo = snapshot.sunInfo!!
    val moonInfo = snapshot.moonInfo!!
    val timeZoneId = snapshot.timeZoneId!!

    val sunInfos = listOfNotNull(sunInfo, snapshot.nextDaySunInfo)
    val goldenWindow = nextLightWindow(
        now = now,
        timeZoneId = timeZoneId,
        windows = sunInfos.flatMap {
            listOf(
                it.morningGoldenHourStartMillis to it.morningGoldenHourEndMillis,
                it.eveningGoldenHourStartMillis to it.eveningGoldenHourEndMillis,
            )
        },
    )
    val blueWindow = nextLightWindow(
        now = now,
        timeZoneId = timeZoneId,
        windows = sunInfos.flatMap {
            listOf(
                it.morningBlueHourStartMillis to it.morningBlueHourEndMillis,
                it.eveningBlueHourStartMillis to it.eveningBlueHourEndMillis,
            )
        },
    )

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        // Left Column: SUN
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .clickable(sunAction),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = sunGlyph(sunInfo.currentPhase),
                    style = TextStyle(fontSize = 18.sp),
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = solarPhaseLabel(context, sunInfo.currentPhase),
                    style = TextStyle(
                        color = AstronomyWidgetPalette.primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.height(3.dp))
            goldenWindow?.let {
                Text(
                    text = "● Golden: $it",
                    style = TextStyle(
                        color = AstronomyWidgetPalette.goldenHourColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
            blueWindow?.let {
                Text(
                    text = "● Blue: $it",
                    style = TextStyle(
                        color = AstronomyWidgetPalette.blueHourColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }

        // Vertical Divider
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .background(AstronomyWidgetPalette.dividerColor),
        ) {}

        Spacer(modifier = GlanceModifier.width(10.dp))

        // Right Column: MOON
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .clickable(moonAction),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = phaseGlyph(moonInfo.phaseOrdinal),
                    style = TextStyle(fontSize = 18.sp),
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = moonPhaseLabel(context, moonInfo.phaseName),
                    style = TextStyle(
                        color = AstronomyWidgetPalette.primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.height(3.dp))
            Text(
                text = "Iluminasi: ${formatPercent(moonInfo.illuminationPercent)}",
                style = TextStyle(
                    color = AstronomyWidgetPalette.moonAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            val moonEventText = moonInfo.riseMillis?.let { "Terbit: ${formatTime(it, timeZoneId)}" }
                ?: moonInfo.setMillis?.let { "Terbenam: ${formatTime(it, timeZoneId)}" }
                ?: ""
            if (moonEventText.isNotEmpty()) {
                Text(
                    text = moonEventText,
                    style = TextStyle(
                        color = AstronomyWidgetPalette.secondaryText,
                        fontSize = 10.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ExpandedContent(
    snapshot: AstronomyWidgetSnapshot,
    sunAction: androidx.glance.action.Action,
    moonAction: androidx.glance.action.Action,
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val sunInfo = snapshot.sunInfo!!
    val moonInfo = snapshot.moonInfo!!
    val timeZoneId = snapshot.timeZoneId!!

    val sunInfos = listOfNotNull(sunInfo, snapshot.nextDaySunInfo)
    val morningGolden = formatWindow(sunInfo.morningGoldenHourStartMillis, sunInfo.morningGoldenHourEndMillis, timeZoneId)
    val eveningGolden = formatWindow(sunInfo.eveningGoldenHourStartMillis, sunInfo.eveningGoldenHourEndMillis, timeZoneId)
    val morningBlue = formatWindow(sunInfo.morningBlueHourStartMillis, sunInfo.morningBlueHourEndMillis, timeZoneId)
    val eveningBlue = formatWindow(sunInfo.eveningBlueHourStartMillis, sunInfo.eveningBlueHourEndMillis, timeZoneId)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(14.dp),
    ) {
        // Header Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "🌌 ${snapshot.locationName}",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = AstronomyWidgetPalette.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Sun Card Block
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(sunAction),
        ) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(text = sunGlyph(sunInfo.currentPhase), style = TextStyle(fontSize = 16.sp))
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "${solarPhaseLabel(context, sunInfo.currentPhase)} • Alt: ${formatAltitude(context, sunInfo.altitude)}",
                    style = TextStyle(
                        color = AstronomyWidgetPalette.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Golden Hour: Pagi $morningGolden • Sore $eveningGolden",
                style = TextStyle(
                    color = AstronomyWidgetPalette.goldenHourColor,
                    fontSize = 10.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = "Blue Hour: Pagi $morningBlue • Sore $eveningBlue",
                style = TextStyle(
                    color = AstronomyWidgetPalette.blueHourColor,
                    fontSize = 10.sp,
                ),
                maxLines = 1,
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AstronomyWidgetPalette.dividerColor),
        ) {}
        Spacer(modifier = GlanceModifier.height(6.dp))

        // Moon Card Block
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(moonAction),
        ) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(text = phaseGlyph(moonInfo.phaseOrdinal), style = TextStyle(fontSize = 16.sp))
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "${moonPhaseLabel(context, moonInfo.phaseName)} (${formatPercent(moonInfo.illuminationPercent)})",
                    style = TextStyle(
                        color = AstronomyWidgetPalette.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            val riseText = moonInfo.riseMillis?.let { formatTime(it, timeZoneId) } ?: "—"
            val setText = moonInfo.setMillis?.let { formatTime(it, timeZoneId) } ?: "—"
            val status = when {
                moonInfo.isPerigee -> "Perigee"
                moonInfo.isApogee -> "Apogee"
                else -> "Orbit Rata-Rata"
            }
            Text(
                text = "Terbit: $riseText • Terbenam: $setText • $status",
                style = TextStyle(
                    color = AstronomyWidgetPalette.secondaryText,
                    fontSize = 10.sp,
                ),
                maxLines = 1,
            )
        }
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
            text = context.getString(R.string.astronomy_widget_data_unavailable),
            style = TextStyle(
                color = AstronomyWidgetPalette.primaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = context.getString(R.string.astronomy_widget_location_hint),
            style = TextStyle(
                color = AstronomyWidgetPalette.secondaryText,
                fontSize = 10.sp,
            ),
            maxLines = 2,
        )
    }
}

private fun phaseGlyph(phaseOrdinal: Int): String = when (phaseOrdinal) {
    0 -> "🌑"
    1 -> "🌒"
    2 -> "🌓"
    3 -> "🌔"
    4 -> "🌕"
    5 -> "🌖"
    6 -> "🌗"
    7 -> "🌘"
    else -> "🌙"
}

private fun sunGlyph(phase: String?): String = when {
    phase?.contains("Day", ignoreCase = true) == true || phase?.contains("Siang", ignoreCase = true) == true -> "☀️"
    phase?.contains("Golden", ignoreCase = true) == true -> "🌅"
    phase?.contains("Blue", ignoreCase = true) == true -> "🌌"
    phase?.contains("Civil", ignoreCase = true) == true -> "🌇"
    phase?.contains("Night", ignoreCase = true) == true || phase?.contains("Malam", ignoreCase = true) == true -> "🌙"
    else -> "☀️"
}

private fun formatPercent(value: Double): String =
    String.format(Locale.ROOT, "%.1f%%", value.coerceIn(0.0, 100.0))

private fun formatAltitude(context: Context, degrees: Double): String =
    if (degrees.isFinite()) String.format(Locale.ROOT, "%.1f°", degrees) else context.getString(R.string.value_unavailable)

private fun formatTime(epochMillis: Long?, timeZoneId: String?): String {
    if (epochMillis == null || timeZoneId == null) return "—"
    val timeZone = runCatching { TimeZone.of(timeZoneId) }.getOrNull() ?: return "—"
    val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
    return String.format(Locale.ROOT, "%02d:%02d", local.hour, local.minute)
}

private fun formatWindow(startMillis: Long?, endMillis: Long?, timeZoneId: String?): String {
    if (startMillis == null || endMillis == null || timeZoneId == null) return "—"
    return "${formatTime(startMillis, timeZoneId)}–${formatTime(endMillis, timeZoneId)}"
}

private fun nextLightWindow(
    now: Long,
    timeZoneId: String?,
    windows: List<Pair<Long?, Long?>>,
): String? {
    if (timeZoneId == null) return null
    val active = windows.firstOrNull { (start, end) ->
        start != null && end != null && now in start..end
    }
    if (active != null) {
        val (start, end) = active
        return "${formatTime(start, timeZoneId)}–${formatTime(end, timeZoneId)}"
    }
    val next = windows
        .filter { (start, _) -> start != null && start > now }
        .minByOrNull { it.first ?: Long.MAX_VALUE }
        ?: return null
    val (start, end) = next
    return "${formatTime(start, timeZoneId)}–${formatTime(end, timeZoneId)}"
}
