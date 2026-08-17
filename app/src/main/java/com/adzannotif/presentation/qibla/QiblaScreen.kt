package com.adzannotif.presentation.qibla

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adzannotif.presentation.common.WindowWidthSizeClass
import com.adzannotif.presentation.common.rememberMotionAnimationsEnabled
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: QiblaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val motionEnabled = rememberMotionAnimationsEnabled()
    val locationName = state.location?.name ?: "Lokasi belum tersedia"

    var continuousHeading by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.deviceHeading) {
        val currentMod = (continuousHeading % 360f + 360f) % 360f
        val diff = ((state.deviceHeading - currentMod + 180f) % 360f + 360f) % 360f - 180f
        continuousHeading += diff
    }

    LaunchedEffect(state.isFacingQibla) {
        if (state.isFacingQibla) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    val animatedDialRotation by animateFloatAsState(
        targetValue = -continuousHeading,
        animationSpec = if (motionEnabled) tween(durationMillis = 100) else snap(),
        label = "compassDialRotation"
    )

    val animatedStatusColor by animateColorAsState(
        targetValue = if (state.qiblaDirection == null || !state.isSensorAvailable) {
            MaterialTheme.colorScheme.outline
        } else if (state.isFacingQibla) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiary
        },
        animationSpec = if (motionEnabled) tween(durationMillis = 180) else snap(),
        label = "statusColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kompas Arah Kiblat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.EXPANDED) 48.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Top guidance banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isFacingQibla) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        contentColor = if (state.isFacingQibla) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (state.isFacingQibla) 4.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = animatedStatusColor,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (!state.isSensorAvailable) {
                                        Icons.Default.Warning
                                    } else if (state.isFacingQibla) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.Explore
                                    },
                                    contentDescription = null,
                                    tint = if (state.isFacingQibla) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.turnInstruction,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.qiblaBearing?.let { bearing ->
                                    "Kiblat: ${String.format(java.util.Locale.US, "%.1f°", bearing)} ($locationName)"
                                } ?: "Kiblat belum tersedia ($locationName)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Main Islamic Compass Dial Canvas
                val dialSize = if (widthSizeClass == WindowWidthSizeClass.EXPANDED) 340.dp else 290.dp

                Box(
                    modifier = Modifier.size(dialSize),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer static rim with Kaaba lock glow
                    if (state.isFacingQibla) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    // Rotating Dial (Needle + Dial Ticks)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(animatedDialRotation),
                        contentAlignment = Alignment.Center
                    ) {
                        val onSurface = MaterialTheme.colorScheme.onSurface
                        val outline = MaterialTheme.colorScheme.outline
                        val primary = MaterialTheme.colorScheme.primary
                        val tertiary = MaterialTheme.colorScheme.tertiary

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.minDimension / 2f - 14.dp.toPx()

                            // Outer dial ring
                            drawCircle(
                                color = outline.copy(alpha = 0.35f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Inner subtle decorative ring
                            drawCircle(
                                color = outline.copy(alpha = 0.15f),
                                radius = radius * 0.72f,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // Compass Ticks (every 5° and 15°)
                            for (degree in 0 until 360 step 5) {
                                val rad = Math.toRadians(degree.toDouble() - 90.0)
                                val isCardinal = degree % 90 == 0
                                val isMajor = degree % 30 == 0
                                val isMinor = degree % 15 == 0

                                val tickLength = when {
                                    isCardinal -> 18.dp.toPx()
                                    isMajor -> 12.dp.toPx()
                                    isMinor -> 8.dp.toPx()
                                    else -> 4.dp.toPx()
                                }
                                val strokeWidth = when {
                                    isCardinal -> 3.5.dp.toPx()
                                    isMajor -> 2.dp.toPx()
                                    else -> 1.dp.toPx()
                                }
                                val tickColor = when {
                                    degree == 0 -> Color(0xFFE53935) // North Red
                                    isCardinal -> onSurface
                                    else -> onSurface.copy(alpha = 0.4f)
                                }

                                val startX = (center.x + (radius - tickLength) * cos(rad)).toFloat()
                                val startY = (center.y + (radius - tickLength) * sin(rad)).toFloat()
                                val endX = (center.x + radius * cos(rad)).toFloat()
                                val endY = (center.y + radius * sin(rad)).toFloat()

                                drawLine(
                                    color = tickColor,
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }

                        // Kaaba Direction Marker (Gold Dial Marker at qiblaBearing)
                        state.qiblaBearing?.let { qiblaBearing ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(qiblaBearing.toFloat()),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = animatedStatusColor,
                                        modifier = Modifier.size(38.dp),
                                        shadowElevation = 4.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "🕋",
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Center Hub: North pointer
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(52.dp),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "U",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE53935),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    // Static Top Device Indicator (Center Top Arrow)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Arah Perangkat",
                            tint = if (state.isFacingQibla) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom Readout Cards: Heading, Bearing, Distance
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Arah HP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${state.deviceHeading.toInt()}°",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Arah Ka'bah", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = state.qiblaBearing?.let { "${it.toInt()}°" } ?: "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (state.qiblaDirection != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Jarak Ka'bah", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${state.qiblaDirection!!.distanceKm.toInt()} km",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
