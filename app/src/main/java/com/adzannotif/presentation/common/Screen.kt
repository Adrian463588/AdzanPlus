package com.adzannotif.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    fun matchesRoute(currentRoute: String): Boolean = when (this) {
        AstronomyDashboard, MoonDetail, SunDetail, StarMap, HijriCalendar -> {
            currentRoute == AstronomyDashboard.route ||
                currentRoute == MoonDetail.route ||
                currentRoute == SunDetail.route ||
                currentRoute == StarMap.route ||
                currentRoute == HijriCalendar.route
        }

        else -> currentRoute == route
    }

    data object Home : Screen(
        route = "home",
        title = "Beranda",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object Schedule : Screen(
        route = "schedule",
        title = "Jadwal",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
    )

    data object Qibla : Screen(
        route = "qibla",
        title = "Kiblat",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
    )

    data object Settings : Screen(
        route = "settings",
        title = "Pengaturan",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    )

    data object AstronomyDashboard : Screen(
        route = "astronomy_dashboard",
        title = "Astronomi",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
    )

    data object MoonDetail : Screen(
        route = "moon_detail",
        title = "Bulan",
        selectedIcon = Icons.Filled.NightsStay,
        unselectedIcon = Icons.Outlined.NightsStay,
    )

    data object SunDetail : Screen(
        route = "sun_detail",
        title = "Matahari",
        selectedIcon = Icons.Filled.WbSunny,
        unselectedIcon = Icons.Outlined.WbSunny,
    )

    data object StarMap : Screen(
        route = "star_map",
        title = "Peta Bintang",
        selectedIcon = Icons.Filled.Stars,
        unselectedIcon = Icons.Outlined.Stars,
    )

    data object HijriCalendar : Screen(
        route = "hijri_calendar",
        title = "Kalender Hijriah",
        selectedIcon = Icons.Filled.Event,
        unselectedIcon = Icons.Outlined.Event,
    )

    companion object {
        val items: List<Screen>
            get() = listOf(Home, Schedule, Qibla, AstronomyDashboard, Settings)
    }
}
