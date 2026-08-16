package com.adzannotif.presentation.qibla

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adzannotif.presentation.common.WindowWidthSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: QiblaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    var continuousHeading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(state.deviceHeading) {
        val diff = (state.deviceHeading - (continuousHeading % 360f + 360f) % 360f)
        val shortestDiff = ((diff + 180f) % 360f + 360f) % 360f - 180f
        continuousHeading += shortestDiff
    }

    androidx.compose.runtime.LaunchedEffect(state.isFacingQibla) {
        if (state.isFacingQibla) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    val animatedRotation by animateFloatAsState(
        targetValue = -continuousHeading,
        animationSpec = tween(durationMillis = 150),
        label = "compassRotation"
    )

    val animatedQiblaColor by animateColorAsState(
        targetValue = if (state.isFacingQibla) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        label = "qiblaStatusColor"
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isFacingQibla) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (state.isFacingQibla) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (state.isFacingQibla) Icons.Default.CheckCircle else Icons.Default.Explore,
                        contentDescription = "Status",
                        tint = animatedQiblaColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = if (state.isFacingQibla) "Menghadap Tepat ke Ka'bah!" else "Arahkan perangkat ke jarum emas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Derajat Kiblat: ${String.format("%.1f°", state.qiblaBearing)} (${state.location.name})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Compass Dial Canvas
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .rotate(animatedRotation),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outline
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 - 12.dp.toPx()

                    // Outer dial circle
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.3f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Cardinal tick marks
                    for (i in 0 until 360 step 15) {
                        val angleRad = Math.toRadians(i.toDouble() - 90.0)
                        val tickLength = if (i % 90 == 0) 16.dp.toPx() else if (i % 45 == 0) 10.dp.toPx() else 6.dp.toPx()
                        val strokeW = if (i % 90 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                        val tickColor = if (i == 0) Color.Red else onSurfaceColor.copy(alpha = 0.5f)

                        val startX = (center.x + (radius - tickLength) * Math.cos(angleRad)).toFloat()
                        val startY = (center.y + (radius - tickLength) * Math.sin(angleRad)).toFloat()
                        val endX = (center.x + radius * Math.cos(angleRad)).toFloat()
                        val endY = (center.y + radius * Math.sin(angleRad)).toFloat()

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeW
                        )
                    }
                }

                // Kaaba direction pointer (Gold / Emerald Arrow)
                val qiblaAngle = state.qiblaBearing.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(qiblaAngle),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = animatedQiblaColor,
                            modifier = Modifier.size(36.dp)
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

                // Center Compass Hub
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "N",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Degree Info Display
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Arah HP", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${state.deviceHeading.toInt()}°",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Arah Ka'bah", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${state.qiblaBearing.toInt()}°",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (state.qiblaDirection != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Jarak", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${state.qiblaDirection!!.distanceKm.toInt()} km",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
