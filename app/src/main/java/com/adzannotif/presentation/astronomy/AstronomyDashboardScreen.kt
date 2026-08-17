package com.adzannotif.presentation.astronomy

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.presentation.common.Screen
import com.adzannotif.presentation.common.WindowWidthSizeClass
import com.adzannotif.presentation.common.rememberMotionAnimationsEnabled
import com.adzannotif.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstronomyDashboardScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT,
    viewModel: AstronomyDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AstronomyBackgroundDeep,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Dashboard Astronomi",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = AstronomyStarWhite
                        )
                        Text(
                            "Pergerakan Matahari, Bulan & Bintang",
                            style = MaterialTheme.typography.bodySmall,
                            color = AstronomyTwilightCivil
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AstronomyBackgroundDeep)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AstronomyBackgroundDeep),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AstronomyMoonGold)
                    }
                }
            } else if (uiState.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                val sunInfo = uiState.sunInfo
                val moonInfo = uiState.moonInfo
                val loc = uiState.location

                if (loc != null) {
                    item {
                        Surface(
                            color = AstronomySurface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AstronomyConstellationLine.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Place,
                                        contentDescription = null,
                                        tint = AstronomyMoonGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                        text = "${loc.name}, ${loc.country}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AstronomyStarWhite
                                    )
                                }
                                Text(
                        text = "${String.format(Locale.ROOT, "%.2f°", loc.latitude)}, ${String.format(Locale.ROOT, "%.2f°", loc.longitude)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AstronomyTwilightCivil
                                )
                            }
                        }
                    }
                }

                item {
                    SolarPhaseBadge(sunInfo = sunInfo)
                }

                if (widthSizeClass != WindowWidthSizeClass.COMPACT) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                SunCard(
                                    sunInfo = sunInfo,
                                    timeZoneId = loc?.timeZoneId,
                                    onClick = { navController.navigate(Screen.SunDetail.route) },
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MoonCard(moonInfo, onClick = { navController.navigate(Screen.MoonDetail.route) })
                            }
                        }
                    }
                } else {
                    item {
                        SunCard(
                            sunInfo = sunInfo,
                            timeZoneId = loc?.timeZoneId,
                            onClick = { navController.navigate(Screen.SunDetail.route) },
                        )
                    }
                    item { MoonCard(moonInfo, onClick = { navController.navigate(Screen.MoonDetail.route) }) }
                }

                item {
                    GoldenBlueHourTimeline(sunInfo = sunInfo, timeZoneId = loc?.timeZoneId)
                }

                item {
                    Text(
                        "Jelajahi Langit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AstronomyStarWhite,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NavigationTile(
                            modifier = Modifier.weight(1f),
                            title = "Detail Bulan",
                            subtitle = "Fase & Iluminasi",
                            icon = Icons.Filled.NightsStay,
                            accentColor = AstronomyMoonGold
                        ) { navController.navigate(Screen.MoonDetail.route) }

                        NavigationTile(
                            modifier = Modifier.weight(1f),
                            title = "Detail Matahari",
                            subtitle = "Arc & Golden Hour",
                            icon = Icons.Filled.WbSunny,
                            accentColor = AstronomySunAmber
                        ) { navController.navigate(Screen.SunDetail.route) }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NavigationTile(
                            modifier = Modifier.weight(1f),
                            title = "Peta Bintang",
                            subtitle = "500 Bintang & Rasi",
                            icon = Icons.Filled.AutoAwesome,
                            accentColor = AstronomyBlueHour
                        ) { navController.navigate(Screen.StarMap.route) }

                        NavigationTile(
                            modifier = Modifier.weight(1f),
                            title = "Kalender Hijriah",
                            subtitle = "Masehi & Hijriah",
                            icon = Icons.Filled.CalendarMonth,
                            accentColor = AstronomyGoldenHour
                        ) { navController.navigate(Screen.HijriCalendar.route) }
                    }
                }

                item {
                    AstronomyWidgetPinningCard()
                }
            }
        }
    }
}

@Composable
fun AstronomyWidgetPinningCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appWidgetManager = remember { context.getSystemService(android.appwidget.AppWidgetManager::class.java) }
    val isPinSupported = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            appWidgetManager?.isRequestPinAppWidgetSupported == true
        } else false
    }

    if (isPinSupported && appWidgetManager != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AstronomySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AstronomyMoonGold.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Widgets,
                        contentDescription = null,
                        tint = AstronomyMoonGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Widget Astronomi di Layar Utama",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AstronomyStarWhite
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Pasang widget fase bulan atau jadwal matahari & golden hour langsung ke Home Screen Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstronomyTwilightCivil
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val provider = android.content.ComponentName(context, com.adzannotif.widget.MoonWidgetReceiver::class.java)
                                appWidgetManager.requestPinAppWidget(provider, null, null)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AstronomyMoonGold),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🌙 Widget Bulan", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val provider = android.content.ComponentName(context, com.adzannotif.widget.SunWidgetReceiver::class.java)
                                appWidgetManager.requestPinAppWidget(provider, null, null)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AstronomySunAmber),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("☀️ Widget Surya", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SolarPhaseBadge(sunInfo: SunInfo?) {
    val phaseName = sunInfo?.currentPhase ?: "Data belum tersedia"
    val phaseColor = when {
        phaseName.contains("Golden", ignoreCase = true) -> AstronomyGoldenHour
        phaseName.contains("Blue", ignoreCase = true) -> AstronomyBlueHour
        phaseName.contains("Day", ignoreCase = true) -> AstronomySunAmber
        phaseName.contains("Civil", ignoreCase = true) -> AstronomyTwilightCivil
        phaseName.contains("Nautical", ignoreCase = true) -> AstronomyTwilightNautical
        phaseName.contains("Astro", ignoreCase = true) -> AstronomyTwilightAstro
        else -> AstronomySurface
    }

    val pulseScale = if (rememberMotionAnimationsEnabled()) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseScale",
        )
        animatedScale
    } else {
        1f
    }

    Surface(
        color = AstronomySurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, phaseColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .scale(pulseScale)
                        .background(phaseColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Fase Matahari Saat Ini",
                        style = MaterialTheme.typography.labelSmall,
                        color = AstronomyTwilightCivil
                    )
                    Text(
                        phaseName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AstronomyStarWhite
                    )
                }
            }

            if (sunInfo != null) {
                val alt = sunInfo.altitude
                Text(
                    "Alt: ${String.format(Locale.ROOT, "%.1f°", alt)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = phaseColor
                )
            }
        }
    }
}

@Composable
fun SunCard(
    sunInfo: SunInfo?,
    timeZoneId: String? = null,
    onClick: () -> Unit,
) {
    fun formatTime(ms: Long?): String {
        if (ms == null || timeZoneId == null) return "—"
        return SimpleDateFormat("HH:mm", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone(timeZoneId)
        }.format(Date(ms))
    }

    fun formatWindow(start: Long?, end: Long?): String {
        if (start == null || end == null || timeZoneId == null) return "—"
        return "${formatTime(start)} - ${formatTime(end)}"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.WbSunny,
                        contentDescription = "Matahari",
                        tint = AstronomySunAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Matahari Hari Ini",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AstronomyStarWhite
                    )
                }
                Text(
                    "Detail ›",
                    style = MaterialTheme.typography.labelMedium,
                    color = AstronomySunAmber
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Terbit", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
                    Text(
                        formatTime(sunInfo?.riseMillis),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AstronomyStarWhite
                    )
                }
                Column {
                    Text("Puncak (Noon)", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
                    Text(
                        formatTime(sunInfo?.noonMillis),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AstronomyStarWhite
                    )
                }
                Column {
                    Text("Terbenam", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
                    Text(
                        formatTime(sunInfo?.setMillis),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AstronomyStarWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AstronomyConstellationLine.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            val goldenMorning = formatWindow(
                sunInfo?.morningGoldenHourStartMillis,
                sunInfo?.morningGoldenHourEndMillis,
            )
            val goldenEvening = formatWindow(
                sunInfo?.eveningGoldenHourStartMillis,
                sunInfo?.eveningGoldenHourEndMillis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(AstronomyGoldenHour, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Golden Hour", style = MaterialTheme.typography.labelSmall, color = AstronomyGoldenHour)
                }
                Text(
                    "Pagi: $goldenMorning • Sore: $goldenEvening",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstronomyStarWhite
                )
            }
        }
    }
}

@Composable
fun MoonCard(moonInfo: MoonInfo?, onClick: () -> Unit) {
    val emoji = when (moonInfo?.phaseOrdinal) {
        0 -> "🌑"
        1 -> "🌒"
        2 -> "🌓"
        3 -> "🌔"
        4 -> "🌕"
        5 -> "🌖"
        6 -> "🌗"
        7 -> "🌘"
        else -> "—"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Bulan Hari Ini",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AstronomyStarWhite
                    )
                }
                Text(
                    "Detail ›",
                    style = MaterialTheme.typography.labelMedium,
                    color = AstronomyMoonGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Fase", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
                    Text(
                        moonInfo?.phaseName ?: "Belum tersedia",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AstronomyMoonGold
                    )
                }
                Column {
                    Text("Iluminasi", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
                    Text(
                    moonInfo?.let { String.format(Locale.ROOT, "%.1f%%", it.illuminationPercent) } ?: "—",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AstronomyStarWhite
                    )
                }
                Column {
                    Text("Umur", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
                    Text(
                    moonInfo?.let { String.format(Locale.ROOT, "%.1f h", it.ageInDays) } ?: "—",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AstronomyStarWhite
                    )
                }
            }
        }
    }
}

@Composable
fun GoldenBlueHourTimeline(sunInfo: SunInfo?, timeZoneId: String? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SolarEventTimeline(
            sunInfo = sunInfo,
            timeZoneId = timeZoneId,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun NavigationTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                color = AstronomyStarWhite,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                color = AstronomyTwilightCivil,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
