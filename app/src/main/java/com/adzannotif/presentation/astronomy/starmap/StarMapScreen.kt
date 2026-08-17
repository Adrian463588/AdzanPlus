package com.adzannotif.presentation.astronomy.starmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.domain.model.astronomy.VisibleStar
import com.adzannotif.presentation.theme.*
import com.adzannotif.presentation.common.WindowWidthSizeClass
import com.adzannotif.presentation.common.Screen
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarMapScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT,
    viewModel: StarMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedStar by remember { mutableStateOf<VisibleStar?>(null) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = AstronomyBackgroundDeep,
        topBar = {
            TopAppBar(
                title = {
                    val loc = uiState.location
                    Column {
                        Text(
                            androidx.compose.ui.res.stringResource(com.adzannotif.R.string.star_map_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = AstronomyStarWhite
                        )
                        Text(
                            text = if (loc != null) "📍 ${loc.name} (${String.format(Locale.ROOT, "%.2f°", loc.latitude)}, ${String.format(Locale.ROOT, "%.2f°", loc.longitude)})" else androidx.compose.ui.res.stringResource(com.adzannotif.R.string.star_map_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = AstronomyTwilightCivil
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Screen.AstronomyDashboard.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(com.adzannotif.R.string.action_back),
                            tint = AstronomyStarWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        resetTrigger++
                        selectedStar = null
                    }) {
                        Icon(
                            Icons.Filled.CenterFocusStrong,
                            contentDescription = androidx.compose.ui.res.stringResource(com.adzannotif.R.string.action_reset_position),
                            tint = AstronomyStarWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AstronomyBackgroundDeep)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.COMPACT) 0.dp else 24.dp)
                .background(AstronomyBackgroundDeep)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = AstronomyMoonGold)
                    }
                } else if (uiState.starMapData == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            color = AstronomySurface,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                text = uiState.error ?: "Data peta langit belum tersedia untuk lokasi ini.",
                                color = AstronomyStarWhite,
                                modifier = Modifier.padding(20.dp),
                            )
                        }
                    }
                } else {
                    InteractiveSkyChart(
                        data = uiState.starMapData,
                        resetKey = resetTrigger,
                        onStarTapped = { selectedStar = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Selected Star Info Sheet / Card overlay
                if (selectedStar != null) {
                    val star = selectedStar!!
                    Surface(
                        color = AstronomySurface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AstronomyMoonGold.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                            .fillMaxWidth(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    star.name ?: "HIP ${star.hipId}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AstronomyMoonGold
                                )
                                Text(
                            "Magnitudo: ${String.format(Locale.ROOT, "%.2f", star.magnitude)} • HIP: ${star.hipId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AstronomyStarWhite
                                )
                                Text(
                            "Alt: ${String.format(Locale.ROOT, "%.1f°", star.altitude)} | Az: ${String.format(Locale.ROOT, "%.1f°", star.azimuth)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AstronomyTwilightCivil
                                )
                            }
                            IconButton(onClick = { selectedStar = null }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = androidx.compose.ui.res.stringResource(com.adzannotif.R.string.star_close_info),
                                    tint = AstronomyStarWhite,
                                )
                            }
                        }
                    }
                }

                // Legend Chip
                if (uiState.starMapData != null) {
                    Surface(
                        color = AstronomySurface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                    ) {
                        Text(
                            androidx.compose.ui.res.stringResource(com.adzannotif.R.string.star_gesture_hint),
                            color = AstronomyTwilightCivil,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            TimeSlider(
                currentMillis = uiState.observedMillis,
                onTimeChanged = { viewModel.setObservedTime(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun InteractiveSkyChart(
    data: StarMapData?,
    resetKey: Int,
    onStarTapped: (VisibleStar) -> Unit,
    modifier: Modifier
) {
    var scale by remember(resetKey) { mutableFloatStateOf(1f) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }

    // Screen projected star coordinates cache for hit detection
    val projectedStars = remember { mutableStateListOf<Pair<VisibleStar, Offset>>() }
    val mapDescription = androidx.compose.ui.res.stringResource(com.adzannotif.R.string.star_map_description)

    Box(
        modifier = modifier.semantics {
            contentDescription = mapDescription
        }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(AstronomyBackgroundDeep)
                .pointerInput(resetKey) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset += pan
                    }
                }
                .pointerInput(resetKey) {
                    detectTapGestures { tapOffset ->
                        val hit = projectedStars.minByOrNull { (_, pos) ->
                            hypot(tapOffset.x - pos.x, tapOffset.y - pos.y)
                        }
                        if (hit != null && hypot(tapOffset.x - hit.second.x, tapOffset.y - hit.second.y) < 40f) {
                            onStarTapped(hit.first)
                        }
                    }
                }
        ) {
        val center = Offset(size.width / 2 + offset.x, size.height / 2 + offset.y)
        val chartRadius = minOf(size.width, size.height) / 2 * scale

        // Draw horizon and cardinal marks
        drawCircle(AstronomyHorizon.copy(alpha = 0.4f), chartRadius, center, style = Stroke(2f))
        drawCircle(AstronomyConstellationLine.copy(alpha = 0.2f), chartRadius * 0.5f, center, style = Stroke(1f))

        fun project(azimuthDeg: Double, altitudeDeg: Double): Offset? {
            if (altitudeDeg < -5.0) return null
            val r = chartRadius * (1.0 - (altitudeDeg.coerceIn(0.0, 90.0) / 90.0))
            val angle = Math.toRadians(azimuthDeg - 90.0)
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            return Offset(x, y)
        }

        projectedStars.clear()

        if (data != null) {
            // 1. Constellation lines
            data.constellations.forEach { constellation ->
                constellation.lines.forEach { (fromHip, toHip) ->
                    val fromPos = data.visibleStars.find { it.hipId == fromHip }?.let { project(it.azimuth, it.altitude) }
                    val toPos = data.visibleStars.find { it.hipId == toHip }?.let { project(it.azimuth, it.altitude) }
                    if (fromPos != null && toPos != null) {
                        drawLine(
                            color = AstronomyConstellationLine.copy(alpha = 0.5f),
                            start = fromPos,
                            end = toPos,
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            // 2. Stars
            data.visibleStars.forEach { star ->
                val pos = project(star.azimuth, star.altitude)
                if (pos != null) {
                    projectedStars.add(star to pos)
                    val starRadius = (4.5 - star.magnitude).coerceIn(1.0, 6.0).toFloat() * (if (scale > 1.5f) 1.3f else 1.0f)
                    val starAlpha = (1.0 - (star.magnitude / 5.0)).coerceIn(0.3, 1.0).toFloat()

                    drawCircle(
                        color = AstronomyStarWhite.copy(alpha = starAlpha),
                        radius = starRadius,
                        center = pos
                    )

                    // Named stars halo
                    if (star.name != null && scale > 1.2f) {
                        drawCircle(
                            color = AstronomyMoonGold.copy(alpha = 0.3f),
                            radius = starRadius * 2f,
                            center = pos
                        )
                    }
                }
            }

            // 3. Sun marker
            project(data.sunAzimuth, data.sunAltitude)?.let { sunPos ->
                drawCircle(color = AstronomySunAmber.copy(alpha = 0.3f), radius = 16f, center = sunPos)
                drawCircle(color = AstronomySunAmber, radius = 9f, center = sunPos)
            }

            // 4. Moon marker
            project(data.moonAzimuth, data.moonAltitude)?.let { moonPos ->
                drawCircle(color = AstronomyMoonGold.copy(alpha = 0.3f), radius = 12f, center = moonPos)
                drawCircle(color = AstronomyMoonGold, radius = 7f, center = moonPos)
            }
        }
        }
        Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp), color = AstronomyTwilightCivil)
        Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), color = AstronomyTwilightCivil)
        Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), color = AstronomyTwilightCivil)
        Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp), color = AstronomyTwilightCivil)
    }
}

@Composable
fun TimeSlider(currentMillis: Long, onTimeChanged: (Long) -> Unit, modifier: Modifier) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(currentMillis) {
        sliderValue = ((currentMillis - System.currentTimeMillis()) / 3_600_000f).coerceIn(-12f, 12f)
    }

    Column(modifier = modifier) {
        val sign = if (sliderValue > 0) "+" else ""
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                androidx.compose.ui.res.stringResource(com.adzannotif.R.string.star_simulation_title),
                color = AstronomyStarWhite,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                androidx.compose.ui.res.stringResource(
                    com.adzannotif.R.string.star_simulation_offset,
                    sign,
                    sliderValue.toInt(),
                ),
                color = AstronomyMoonGold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                val newMillis = System.currentTimeMillis() + (sliderValue * 60 * 60 * 1000L).toLong()
                onTimeChanged(newMillis)
            },
            valueRange = -12f..12f,
            colors = SliderDefaults.colors(
                thumbColor = AstronomyMoonGold,
                activeTrackColor = AstronomyGoldenHour,
                inactiveTrackColor = AstronomySurface
            )
        )
    }
}
