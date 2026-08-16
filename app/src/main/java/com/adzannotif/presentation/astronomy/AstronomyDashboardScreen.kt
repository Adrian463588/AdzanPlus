package com.adzannotif.presentation.astronomy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.MoonInfo // will be provided by domain agent
import com.adzannotif.domain.model.astronomy.SunInfo // will be provided by domain agent
import com.adzannotif.presentation.common.Screen
import com.adzannotif.presentation.common.WindowWidthSizeClass
import com.adzannotif.presentation.theme.*

@Composable
fun AstronomyDashboardScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT,
    viewModel: AstronomyDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AstronomyBackgroundDeep),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Astronomi", style = MaterialTheme.typography.headlineMedium, color = AstronomyStarWhite)
        }

        if (uiState.isLoading) {
            item { CircularProgressIndicator(color = AstronomyStarWhite) }
        } else if (uiState.error != null) {
            item { Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error) }
        } else {
            val sunInfo = uiState.sunInfo
            val moonInfo = uiState.moonInfo

            item {
                SolarPhaseBadge()
            }

            if (widthSizeClass == WindowWidthSizeClass.EXPANDED) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SunCard(sunInfo, onClick = { navController.navigate(Screen.SunDetail.route) })
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            MoonCard(moonInfo, onClick = { navController.navigate(Screen.MoonDetail.route) })
                        }
                    }
                }
            } else {
                item { SunCard(sunInfo, onClick = { navController.navigate(Screen.SunDetail.route) }) }
                item { MoonCard(moonInfo, onClick = { navController.navigate(Screen.MoonDetail.route) }) }
            }

            item {
                GoldenBlueHourTimeline()
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NavigationTile(modifier = Modifier.weight(1f), title = "Bulan", icon = Icons.Filled.NightsStay) { navController.navigate(Screen.MoonDetail.route) }
                    NavigationTile(modifier = Modifier.weight(1f), title = "Matahari", icon = Icons.Filled.WbSunny) { navController.navigate(Screen.SunDetail.route) }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NavigationTile(modifier = Modifier.weight(1f), title = "Peta Bintang", icon = Icons.Filled.AutoAwesome) { navController.navigate(Screen.StarMap.route) }
                    NavigationTile(modifier = Modifier.weight(1f), title = "Kalender", icon = Icons.Filled.CalendarMonth) { navController.navigate(Screen.HijriCalendar.route) }
                }
            }
        }
    }
}

@Composable
fun SolarPhaseBadge() {
    Surface(
        color = AstronomySurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).background(AstronomySunAmber, RoundedCornerShape(50)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Day", color = AstronomyStarWhite, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SunCard(sunInfo: SunInfo?, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Matahari", style = MaterialTheme.typography.titleLarge, color = AstronomyStarWhite)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Terbit: --:--", color = AstronomyStarWhite)
            Text("Puncak: --:--", color = AstronomyStarWhite)
            Text("Terbenam: --:--", color = AstronomyStarWhite)
        }
    }
}

@Composable
fun MoonCard(moonInfo: MoonInfo?, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bulan", style = MaterialTheme.typography.titleLarge, color = AstronomyStarWhite)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Fase: --", color = AstronomyStarWhite)
            Text("Iluminasi: --%", color = AstronomyStarWhite)
        }
    }
}

@Composable
fun GoldenBlueHourTimeline() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text("Timeline Hari Ini", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            // Draw a simple bar
            drawRect(
                color = AstronomyTwilightAstro,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height)
            )
            drawRect(
                color = AstronomySunAmber,
                topLeft = Offset(size.width * 0.25f, 0f),
                size = Size(size.width * 0.5f, size.height)
            )
        }
    }
}

@Composable
fun NavigationTile(modifier: Modifier = Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        modifier = modifier.clickable(onClick = onClick).aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = AstronomyStarWhite, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = AstronomyStarWhite)
        }
    }
}
