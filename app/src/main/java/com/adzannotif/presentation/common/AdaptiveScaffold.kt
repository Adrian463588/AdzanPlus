package com.adzannotif.presentation.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adzannotif.presentation.theme.AstronomyBackgroundDeep
import com.adzannotif.presentation.theme.AstronomyMoonGold
import com.adzannotif.presentation.theme.AstronomyStarWhite
import com.adzannotif.presentation.theme.AstronomyTwilightCivil

enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun AdaptiveScaffold(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    content: @Composable (WindowWidthSizeClass) -> Unit
) {
    val isAstronomyRoute = currentRoute == Screen.AstronomyDashboard.route ||
            currentRoute == Screen.MoonDetail.route ||
            currentRoute == Screen.SunDetail.route ||
            currentRoute == Screen.StarMap.route ||
            currentRoute == Screen.HijriCalendar.route

    val navBarContainerColor by animateColorAsState(
        targetValue = if (isAstronomyRoute) AstronomyBackgroundDeep else MaterialTheme.colorScheme.surface,
        animationSpec = if (rememberMotionAnimationsEnabled()) tween(300) else snap(),
        label = "navBarContainerColor"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isAstronomyRoute) AstronomyBackgroundDeep else MaterialTheme.colorScheme.background
            )
    ) {
        val widthClass = when {
            maxWidth < 600.dp -> WindowWidthSizeClass.COMPACT
            maxWidth < 840.dp -> WindowWidthSizeClass.MEDIUM
            else -> WindowWidthSizeClass.EXPANDED
        }

        if (widthClass == WindowWidthSizeClass.COMPACT) {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = navBarContainerColor,
                        contentColor = if (isAstronomyRoute) AstronomyStarWhite else MaterialTheme.colorScheme.onSurface,
                    ) {
                        Screen.items.forEach { screen ->
                            val selected = screen.matchesRoute(currentRoute)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onNavigate(screen) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                colors = if (isAstronomyRoute) {
                                    NavigationBarItemDefaults.colors(
                                        indicatorColor = AstronomyMoonGold.copy(alpha = 0.2f),
                                        selectedIconColor = AstronomyMoonGold,
                                        selectedTextColor = AstronomyMoonGold,
                                        unselectedIconColor = AstronomyTwilightCivil,
                                        unselectedTextColor = AstronomyTwilightCivil
                                    )
                                } else {
                                    NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    content(widthClass)
                }
            }
        } else {
            // Medium / Expanded: Navigation Rail for tablets, foldables, and landscape.
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = navBarContainerColor,
                    contentColor = if (isAstronomyRoute) AstronomyStarWhite else MaterialTheme.colorScheme.onSurface,
                ) {
                    Screen.items.forEach { screen ->
                        val selected = screen.matchesRoute(currentRoute)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onNavigate(screen) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            alwaysShowLabel = widthClass == WindowWidthSizeClass.EXPANDED,
                            colors = if (isAstronomyRoute) {
                                NavigationRailItemDefaults.colors(
                                    indicatorColor = AstronomyMoonGold.copy(alpha = 0.2f),
                                    selectedIconColor = AstronomyMoonGold,
                                    selectedTextColor = AstronomyMoonGold,
                                    unselectedIconColor = AstronomyTwilightCivil,
                                    unselectedTextColor = AstronomyTwilightCivil
                                )
                            } else {
                                NavigationRailItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    content(widthClass)
                }
            }
        }
    }
}
