package com.adzannotif.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared visual tokens used by Android and iOS Compose hosts. */
object SharedUiTokens {
    val brandPrimary = Color(0xFF1B8064)
    val astronomySurface = Color(0xFF111D30)
    val astronomyBackground = Color(0xFF050A14)
    val astronomyAccent = Color(0xFFFAC248)
    val minimumTouchTarget = 48.dp
}

enum class SharedWindowWidth {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

sealed interface SharedUiAction {
    data object RequestLocation : SharedUiAction
    data object ScheduleNotifications : SharedUiAction
}

/**
 * Small, platform-neutral adaptive shell. Hosts provide real snapshot data and
 * actions; this layer never invents a prayer time or location.
 */
@Composable
fun SharedPrayerShell(
    snapshot: PrayerUiSnapshot,
    currentRoute: SharedRoute,
    strings: SharedUiStrings,
    onRouteSelected: (SharedRoute) -> Unit,
    onAction: (SharedUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val width = when {
                maxWidth < 600.dp -> SharedWindowWidth.COMPACT
                maxWidth < 840.dp -> SharedWindowWidth.MEDIUM
                else -> SharedWindowWidth.EXPANDED
            }
            val content: @Composable () -> Unit = {
                SharedSnapshotCard(snapshot = snapshot, strings = strings, onAction = onAction)
            }

            if (width == SharedWindowWidth.COMPACT) {
                androidx.compose.material3.Scaffold(
                    bottomBar = {
                        NavigationBar {
                            SharedRoute.primary.forEach { route ->
                                NavigationBarItem(
                                    selected = currentRoute == route,
                                    onClick = { onRouteSelected(route) },
                                    icon = { Text(route.id.take(1).uppercase()) },
                                    label = { Text(strings.routeLabels.getValue(route)) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) { content() }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        SharedRoute.primary.forEach { route ->
                            NavigationRailItem(
                                selected = currentRoute == route,
                                onClick = { onRouteSelected(route) },
                                icon = { Text(route.id.take(1).uppercase()) },
                                label = { Text(strings.routeLabels.getValue(route)) },
                                alwaysShowLabel = width == SharedWindowWidth.EXPANDED,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedSnapshotCard(
    snapshot: PrayerUiSnapshot,
    strings: SharedUiStrings,
    onAction: (SharedUiAction) -> Unit,
) {
    val isAvailable = snapshot.availability == SnapshotAvailability.AVAILABLE &&
        snapshot.nextPrayerId != null && snapshot.targetEpochMillis != null

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = snapshot.locationName ?: strings.locationUnavailable,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isAvailable) {
                Text(
                    text = snapshot.nextPrayerId!!,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = snapshot.targetInstant()?.toString() ?: strings.timeUnavailable,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = strings.prayerDataUnavailable,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = strings.locationPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onAction(SharedUiAction.RequestLocation) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(strings.locationAction)
                }
                Button(
                    onClick = { onAction(SharedUiAction.ScheduleNotifications) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(strings.notificationsAction)
                }
            }
        }
    }
}
