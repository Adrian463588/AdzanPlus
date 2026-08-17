package com.adzannotif.shared

import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Stable route identifiers shared by Android and iOS hosts. */
enum class SharedRoute(val id: String) {
    HOME("home"),
    SCHEDULE("schedule"),
    QIBLA("qibla"),
    ASTRONOMY("astronomy_dashboard"),
    SETTINGS("settings");

    companion object {
        val primary: List<SharedRoute> = entries
    }
}

/** Product states are explicit; unavailable data is never represented as an invented default. */
sealed interface AvailabilityState {
    data object Loading : AvailabilityState
    data object Available : AvailabilityState
    data class Unavailable(val reason: UnavailableReason) : AvailabilityState
}

enum class UnavailableReason {
    LOCATION_REQUIRED,
    CALCULATION_NOT_DEFINED,
    SENSOR_NOT_AVAILABLE,
    PERMISSION_REQUIRED,
    PLATFORM_UNSUPPORTED,
}

@Serializable
data class PrayerUiSnapshot(
    val nextPrayerId: String? = null,
    val targetEpochMillis: Long? = null,
    val locationName: String? = null,
    val availability: SnapshotAvailability = SnapshotAvailability.UNAVAILABLE,
) {
    fun targetInstant(): Instant? = targetEpochMillis?.let(Instant::fromEpochMilliseconds)
}

@Serializable
enum class SnapshotAvailability {
    AVAILABLE,
    UNAVAILABLE,
}

@Serializable
enum class CelestialSnapshotKind {
    MOON,
    SUN,
}

@Serializable
data class CelestialUiSnapshot(
    val kind: CelestialSnapshotKind,
    val phaseName: String? = null,
    val illuminationPercent: Double? = null,
    val nextEventTitle: String? = null,
    val nextEventEpochMillis: Long? = null,
    val availability: SnapshotAvailability = SnapshotAvailability.UNAVAILABLE,
)

/** Platform services are injected by Android/iOS hosts; the shared layer owns no SDK object. */
interface SharedPlatformServices {
    val prayerSnapshot: StateFlow<PrayerUiSnapshot>
    suspend fun requestLocation()
    suspend fun schedulePrayerNotifications()
}

data class SharedUiStrings(
    val routeLabels: Map<SharedRoute, String>,
    val locationAction: String,
    val notificationsAction: String,
    val locationUnavailable: String,
    val prayerDataUnavailable: String,
    val locationPrompt: String,
    val timeUnavailable: String,
)
