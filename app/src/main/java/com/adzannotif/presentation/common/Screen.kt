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
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.adzannotif.R
import com.adzannotif.shared.SharedRoute

sealed class Screen(
    val route: String,
    val title: String,
    @StringRes val titleRes: Int,
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
        route = SharedRoute.HOME.id,
        title = "Beranda",
        titleRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object Schedule : Screen(
        route = SharedRoute.SCHEDULE.id,
        title = "Jadwal",
        titleRes = R.string.nav_schedule,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
    )

    data object Qibla : Screen(
        route = SharedRoute.QIBLA.id,
        title = "Kiblat",
        titleRes = R.string.nav_qibla,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
    )

    data object Settings : Screen(
        route = SharedRoute.SETTINGS.id,
        title = "Pengaturan",
        titleRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    )

    data object AstronomyDashboard : Screen(
        route = SharedRoute.ASTRONOMY.id,
        title = "Astronomi",
        titleRes = R.string.nav_astronomy,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
    )

    data object MoonDetail : Screen(
        route = "moon_detail",
        title = "Bulan",
        titleRes = R.string.moon_detail_title,
        selectedIcon = Icons.Filled.NightsStay,
        unselectedIcon = Icons.Outlined.NightsStay,
    )

    data object SunDetail : Screen(
        route = "sun_detail",
        title = "Matahari",
        titleRes = R.string.sun_detail_title,
        selectedIcon = Icons.Filled.WbSunny,
        unselectedIcon = Icons.Outlined.WbSunny,
    )

    data object StarMap : Screen(
        route = "star_map",
        title = "Peta Bintang",
        titleRes = R.string.star_map_title,
        selectedIcon = Icons.Filled.Stars,
        unselectedIcon = Icons.Outlined.Stars,
    )

    data object HijriCalendar : Screen(
        route = "hijri_calendar",
        title = "Kalender Hijriah",
        titleRes = R.string.hijri_calendar_title,
        selectedIcon = Icons.Filled.Event,
        unselectedIcon = Icons.Outlined.Event,
    )

    companion object {
        val items: List<Screen>
            get() = listOf(Home, Schedule, Qibla, AstronomyDashboard, Settings)
    }
}

/**
 * Switches to a top-level dashboard without restoring a previously saved
 * detail destination. Top-level navigation is a root switch, not a detail
 * back-stack restore operation.
 */
fun NavController.navigateToTopLevel(screen: Screen) {
    navigate(screen.route) {
        popUpTo(Screen.Home.route) {
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}

/**
 * Returns from an astronomy detail screen to its dashboard. Widget deep links
 * may open a detail route without a dashboard entry, so navigation has a
 * deterministic dashboard fallback.
 */
fun NavController.navigateToAstronomyDashboard() {
    if (!popBackStack(Screen.AstronomyDashboard.route, inclusive = false)) {
        navigateToTopLevel(Screen.AstronomyDashboard)
    }
}

@Composable
fun AstronomyDetailBackHandler(navController: NavController) {
    BackHandler {
        navController.navigateToAstronomyDashboard()
    }
}
