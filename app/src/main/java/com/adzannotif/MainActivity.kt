package com.adzannotif

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.presentation.common.AdaptiveScaffold
import com.adzannotif.presentation.common.Screen
import com.adzannotif.presentation.common.WindowWidthSizeClass
import com.adzannotif.presentation.home.HomeScreen
import com.adzannotif.presentation.qibla.QiblaScreen
import com.adzannotif.presentation.schedule.ScheduleScreen
import com.adzannotif.presentation.settings.SettingsScreen
import com.adzannotif.presentation.astronomy.AstronomyDashboardScreen
import com.adzannotif.presentation.astronomy.sun.SunDetailScreen
import com.adzannotif.presentation.astronomy.moon.MoonDetailScreen
import com.adzannotif.presentation.astronomy.starmap.StarMapScreen
import com.adzannotif.presentation.astronomy.calendar.HijriCalendarScreen
import com.adzannotif.presentation.theme.AstronomyBackgroundDeep
import com.adzannotif.platform.alarm.AlarmScheduler
import com.adzannotif.presentation.theme.AdzanNotifTheme
import com.adzannotif.presentation.widget.PrayerTimesWidgetReceiver
import com.adzannotif.widget.AstronomyWidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        alarmScheduler.rescheduleAllAlarms()
        refreshWidgets()
    }

    private var exactAlarmPromptShown = false
    private val widgetRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetRoute.value = routeFromIntent(intent)
        requestAppPermissions()

        setContent {
            val userSettings by settingsRepository.userSettings.collectAsStateWithLifecycle(
                initialValue = com.adzannotif.domain.model.UserSettings()
            )

            AdzanNotifTheme(themeMode = userSettings.themeMode) {
                val navController = rememberNavController()
                val requestedRoute by widgetRoute.collectAsStateWithLifecycle()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
                LaunchedEffect(requestedRoute) {
                    requestedRoute?.let { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        widgetRoute.value = null
                    }
                }
                val astronomyRoute = currentRoute == Screen.AstronomyDashboard.route ||
                    currentRoute == Screen.MoonDetail.route ||
                    currentRoute == Screen.SunDetail.route ||
                    currentRoute == Screen.StarMap.route ||
                    currentRoute == Screen.HijriCalendar.route
                val darkTheme = when (userSettings.themeMode) {
                    com.adzannotif.domain.model.ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    com.adzannotif.domain.model.ThemeMode.LIGHT -> false
                    com.adzannotif.domain.model.ThemeMode.DARK -> true
                }
                val systemBarColor = if (astronomyRoute) {
                    AstronomyBackgroundDeep
                } else {
                    MaterialTheme.colorScheme.background
                }
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = this@MainActivity.window
                        val useLightIcons = !astronomyRoute && !darkTheme
                        window.statusBarColor = systemBarColor.toArgb()
                        window.navigationBarColor = systemBarColor.toArgb()
                        val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                        controller.isAppearanceLightStatusBars = useLightIcons
                        controller.isAppearanceLightNavigationBars = useLightIcons
                    }
                }

                AdaptiveScaffold(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) { widthSizeClass ->
                    AppNavHost(
                        navController = navController,
                        widthSizeClass = widthSizeClass
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!exactAlarmPromptShown) {
            alarmScheduler.exactAlarmPermissionIntent()?.let { intent ->
                exactAlarmPromptShown = true
                startActivity(intent)
            }
        }
        alarmScheduler.rescheduleAllAlarms()
        refreshWidgets()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeFromIntent(intent)?.let {
            widgetRoute.value = it
        }
    }

    private fun routeFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val extraRoute = intent.getStringExtra("com.adzannotif.extra.ASTRONOMY_ROUTE")
        if (extraRoute != null && extraRoute in VALID_ROUTES) return extraRoute
        val data = intent.data ?: return null
        if (data.scheme != WIDGET_ROUTE_SCHEME) return null
        val route = data.host?.takeIf { it.isNotBlank() } ?: data.path?.trim('/')
        return route?.takeIf { it in VALID_ROUTES }
    }

    private fun refreshWidgets() {
        PrayerTimesWidgetReceiver.updateAll(this)
        lifecycleScope.launch {
            AstronomyWidgetUpdater.updateAll(this@MainActivity)
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private companion object {
        const val WIDGET_ROUTE_SCHEME = "adzannotif"
        val VALID_ROUTES = setOf(
            Screen.Home.route,
            Screen.Schedule.route,
            Screen.Qibla.route,
            Screen.Settings.route,
            Screen.AstronomyDashboard.route,
            Screen.MoonDetail.route,
            Screen.SunDetail.route,
            Screen.StarMap.route,
            Screen.HijriCalendar.route,
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                widthSizeClass = widthSizeClass,
                onNavigateToAstronomy = { navController.navigate(Screen.AstronomyDashboard.route) }
            )
        }
        composable(Screen.Schedule.route) {
            ScheduleScreen(widthSizeClass = widthSizeClass)
        }
        composable(Screen.Qibla.route) {
            QiblaScreen(widthSizeClass = widthSizeClass)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(widthSizeClass = widthSizeClass)
        }
        composable(Screen.AstronomyDashboard.route) {
            AstronomyDashboardScreen(navController = navController, widthSizeClass = widthSizeClass)
        }
        composable(Screen.SunDetail.route) {
            SunDetailScreen(navController = navController, widthSizeClass = widthSizeClass)
        }
        composable(Screen.MoonDetail.route) {
            MoonDetailScreen(navController = navController, widthSizeClass = widthSizeClass)
        }
        composable(Screen.StarMap.route) {
            StarMapScreen(navController = navController, widthSizeClass = widthSizeClass)
        }
        composable(Screen.HijriCalendar.route) {
            HijriCalendarScreen(navController = navController, widthSizeClass = widthSizeClass)
        }
    }
}
