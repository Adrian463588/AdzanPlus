package com.adzannotif

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.adzannotif.presentation.theme.AdzanNotifTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAppPermissions()

        setContent {
            val userSettings by settingsRepository.userSettings.collectAsStateWithLifecycle(
                initialValue = com.adzannotif.domain.model.UserSettings()
            )

            AdzanNotifTheme(themeMode = userSettings.themeMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

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
            HomeScreen(widthSizeClass = widthSizeClass)
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
    }
}
