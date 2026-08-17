package com.adzannotif.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.adzannotif.core.astronomy.AstronomyEngine
import com.adzannotif.core.astronomy.ObserverLocation
import com.adzannotif.core.prayer.CalculationMethod
import com.adzannotif.core.prayer.Coordinates
import com.adzannotif.core.prayer.DateComponents
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.core.prayer.PrayerTimes
import com.adzannotif.core.prayer.PrayerTimesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlin.math.max

private const val APP_GROUP_ID = "group.com.adzannotif.app"
private const val PRAYER_SNAPSHOT_KEY = "prayer_snapshot"
private const val MOON_SNAPSHOT_KEY = "moon_snapshot"
private const val SUN_SNAPSHOT_KEY = "sun_snapshot"
private const val CALCULATION_METHOD_KEY = "calculation_method"
private const val PRAYER_CACHE_FILE = "prayer_snapshot.json"

private val iosJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private class IosLocationDelegate(
    private val onLocation: (CLLocation) -> Unit,
    private val reportFailure: (String) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        if (location == null || location.horizontalAccuracy < 0.0) {
            reportFailure("Koordinat lokasi tidak valid")
            return
        }
        manager.stopUpdatingLocation()
        onLocation(location)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        notifyFailure(didFailWithError.localizedDescription)
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> manager.requestLocation()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> reportFailure("Izin lokasi tidak tersedia")
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
        }
    }

    private fun notifyFailure(errorMessage: String?) {
        reportFailure(errorMessage?.takeIf(String::isNotBlank) ?: "Lokasi perangkat tidak tersedia")
    }
}

/**
 * iOS adapter for the shared prayer snapshot. It starts in an unavailable state,
 * reads only previously persisted JSON, and replaces that state after CoreLocation
 * returns a real coordinate. No coordinate, timezone, or prayer timestamp is
 * invented by this adapter.
 */
private class IosSharedPlatformServices : SharedPlatformServices {
    private val defaults = NSUserDefaults(suiteName = APP_GROUP_ID)
        ?: NSUserDefaults.standardUserDefaults
    private val locationManager = CLLocationManager()
    private val _prayerSnapshot = MutableStateFlow(loadPrayerSnapshot() ?: PrayerUiSnapshot())
    private val _moonSnapshot = MutableStateFlow(loadSnapshot<CelestialUiSnapshot>(MOON_SNAPSHOT_KEY))
    private val _sunSnapshot = MutableStateFlow(loadSnapshot<CelestialUiSnapshot>(SUN_SNAPSHOT_KEY))
    private val locationDelegate = IosLocationDelegate(
        onLocation = ::updateFromLocation,
        reportFailure = { _ ->
            _prayerSnapshot.value = PrayerUiSnapshot()
        },
    )

    override val prayerSnapshot: StateFlow<PrayerUiSnapshot> = _prayerSnapshot
    val moonSnapshot: StateFlow<CelestialUiSnapshot?> = _moonSnapshot
    val sunSnapshot: StateFlow<CelestialUiSnapshot?> = _sunSnapshot

    init {
        locationManager.delegate = locationDelegate
    }

    override suspend fun requestLocation() {
        when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> locationManager.requestLocation()
            kCLAuthorizationStatusNotDetermined -> locationManager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> _prayerSnapshot.value = PrayerUiSnapshot()
        }
    }

    override suspend fun schedulePrayerNotifications() {
        val snapshot = _prayerSnapshot.value
        val targetEpochMillis = snapshot.targetEpochMillis ?: return
        if (snapshot.availability != SnapshotAvailability.AVAILABLE) return

        val delaySeconds = max(
            1L,
            (targetEpochMillis - Clock.System.now().toEpochMilliseconds()) / 1_000L,
        )
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { granted, _ ->
            if (!granted) return@requestAuthorizationWithOptions
            val content = UNMutableNotificationContent().apply {
                title = snapshot.nextPrayerId ?: return@apply
                body = snapshot.locationName ?: "Jadwal sholat"
                sound = platform.UserNotifications.UNNotificationSound.defaultSound
            }
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = delaySeconds.toDouble(),
                repeats = false,
            )
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "prayer-$targetEpochMillis",
                content = content,
                trigger = trigger,
            )
            center.addNotificationRequest(request, withCompletionHandler = null)
        }
    }

    private fun updateFromLocation(location: CLLocation) {
        val latitude = location.coordinate.latitude
        val longitude = location.coordinate.longitude
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        val method = calculationMethod()
        val coordinates = Coordinates(latitude = latitude, longitude = longitude)
        val parameters = method.createParameters()

        val todayTimes = PrayerTimes.calculate(
            coordinates = coordinates,
            dateComponents = DateComponents.from(today),
            calculationParameters = parameters,
        )
        val snapshot = when (todayTimes) {
            is PrayerTimesResult.Unavailable -> PrayerUiSnapshot()
            is PrayerTimesResult.Available -> {
                val todayNext = nextFard(todayTimes.value, now)
                if (todayNext != null) {
                    PrayerUiSnapshot(
                        nextPrayerId = todayNext.first.displayNameId,
                        targetEpochMillis = todayNext.second.toEpochMilliseconds(),
                        locationName = "$latitude, $longitude",
                        availability = SnapshotAvailability.AVAILABLE,
                    )
                } else {
                    val tomorrow = today.plus(DatePeriod(days = 1))
                    val tomorrowTimes = PrayerTimes.calculate(
                        coordinates = coordinates,
                        dateComponents = DateComponents.from(tomorrow),
                        calculationParameters = parameters,
                    )
                    when (tomorrowTimes) {
                        is PrayerTimesResult.Unavailable -> PrayerUiSnapshot()
                        is PrayerTimesResult.Available -> nextFard(tomorrowTimes.value, now)?.let { next ->
                            PrayerUiSnapshot(
                                nextPrayerId = next.first.displayNameId,
                                targetEpochMillis = next.second.toEpochMilliseconds(),
                                locationName = "$latitude, $longitude",
                                availability = SnapshotAvailability.AVAILABLE,
                            )
                        } ?: PrayerUiSnapshot()
                    }
                }
            }
        }

        _prayerSnapshot.value = snapshot
        persistPrayerSnapshot(snapshot)

        if (location.verticalAccuracy >= 0.0) {
            val observer = ObserverLocation(
                latitude = latitude,
                longitude = longitude,
                elevationMeters = location.altitude,
                timeZoneId = timeZone.id,
            )
            persistCelestialSnapshots(observer, now.toEpochMilliseconds())
        } else {
            _moonSnapshot.value = null
            _sunSnapshot.value = null
        }
    }

    private fun nextFard(
        prayerTimes: com.adzannotif.core.prayer.PrayerTimes,
        now: kotlinx.datetime.Instant,
    ): Pair<Prayer, kotlinx.datetime.Instant>? {
        return Prayer.FARD_PRAYERS
            .map { prayer -> prayer to prayerTimes.timeForPrayer(prayer) }
            .firstOrNull { (_, instant) -> instant > now }
    }

    private fun calculationMethod(): CalculationMethod {
        val stored = defaults.stringForKey(CALCULATION_METHOD_KEY)
        return stored?.let { name ->
            runCatching { CalculationMethod.valueOf(name) }.getOrNull()
        } ?: CalculationMethod.KEMENAG_RI
    }

    private fun persistCelestialSnapshots(observer: ObserverLocation, epochMillis: Long) {
        val engine = AstronomyEngine()
        val sun = engine.getSunState(observer, epochMillis)
        val moon = engine.getMoonState(observer, epochMillis)
        val now = epochMillis
        val nextSunEvent = listOfNotNull(
            "Sunrise" to sun.riseMillis,
            "Golden Hour" to sun.goldenBlueHour.morningGoldenHour?.startMillis,
            "Sunset" to sun.setMillis,
            "Golden Hour" to sun.goldenBlueHour.eveningGoldenHour?.startMillis,
        ).filter { (_, target) -> target > now }.minByOrNull { (_, target) -> target }
        val nextMoonEvent = listOfNotNull(
            "Moonrise" to moon.riseMillis,
            "Moonset" to moon.setMillis,
        ).filter { (_, target) -> target > now }.minByOrNull { (_, target) -> target }

        val sunSnapshot = CelestialUiSnapshot(
            kind = CelestialSnapshotKind.SUN,
            phaseName = sun.currentPhase.displayName,
            nextEventTitle = nextSunEvent?.first,
            nextEventEpochMillis = nextSunEvent?.second,
            availability = SnapshotAvailability.AVAILABLE,
        )
        val moonSnapshot = CelestialUiSnapshot(
            kind = CelestialSnapshotKind.MOON,
            phaseName = moon.phase.displayName,
            illuminationPercent = moon.illuminationFraction * 100.0,
            nextEventTitle = nextMoonEvent?.first,
            nextEventEpochMillis = nextMoonEvent?.second,
            availability = SnapshotAvailability.AVAILABLE,
        )
        _sunSnapshot.value = sunSnapshot
        _moonSnapshot.value = moonSnapshot
        persistSnapshot(SUN_SNAPSHOT_KEY, sunSnapshot)
        persistSnapshot(MOON_SNAPSHOT_KEY, moonSnapshot)
    }

    private fun persistPrayerSnapshot(snapshot: PrayerUiSnapshot) {
        val raw = iosJson.encodeToString(snapshot)
        defaults.setObject(raw, forKey = PRAYER_SNAPSHOT_KEY)
        snapshotFileUrl()?.let { url ->
            NSString.create(string = raw).writeToURL(
                url = url,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
        }
    }

    private fun persistSnapshot(key: String, snapshot: CelestialUiSnapshot) {
        val raw = iosJson.encodeToString(snapshot)
        defaults.setObject(raw, forKey = key)
        snapshotFileUrl("$key.json")?.let { url ->
            NSString.create(string = raw).writeToURL(
                url = url,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
        }
    }

    private fun loadPrayerSnapshot(): PrayerUiSnapshot? {
        val raw = defaults.stringForKey(PRAYER_SNAPSHOT_KEY) ?: snapshotFileUrl()?.let { url ->
            NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
        } ?: return null
        return runCatching { iosJson.decodeFromString<PrayerUiSnapshot>(raw) }.getOrNull()
    }

    private inline fun <reified T> loadSnapshot(key: String): T? {
        val raw = defaults.stringForKey(key) ?: snapshotFileUrl("$key.json")?.let { url ->
            NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
        } ?: return null
        return runCatching { iosJson.decodeFromString<T>(raw) }.getOrNull()
    }

    private fun snapshotFileUrl(fileName: String = PRAYER_CACHE_FILE): NSURL? {
        val urls = NSFileManager.defaultManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask,
        )
        return (urls.firstOrNull() as? NSURL)?.URLByAppendingPathComponent(fileName)
    }
}

private val iosUiStrings = SharedUiStrings(
    routeLabels = mapOf(
        SharedRoute.HOME to "Beranda",
        SharedRoute.SCHEDULE to "Jadwal",
        SharedRoute.QIBLA to "Kiblat",
        SharedRoute.ASTRONOMY to "Astronomi",
        SharedRoute.SETTINGS to "Pengaturan",
    ),
    locationAction = "Lokasi",
    notificationsAction = "Notifikasi",
    locationUnavailable = "Lokasi belum tersedia",
    prayerDataUnavailable = "Data waktu sholat belum tersedia",
    locationPrompt = "Pilih lokasi atau izinkan akses lokasi untuk menghitung waktu nyata.",
    timeUnavailable = "Waktu belum tersedia",
)

@Suppress("unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
    val services = remember { IosSharedPlatformServices() }
    val snapshot by services.prayerSnapshot.collectAsState()
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf(SharedRoute.HOME) }

    SharedPrayerShell(
        snapshot = snapshot,
        currentRoute = currentRoute,
        strings = iosUiStrings,
        onRouteSelected = { currentRoute = it },
        onAction = { action ->
            scope.launch {
                when (action) {
                    SharedUiAction.RequestLocation -> services.requestLocation()
                    SharedUiAction.ScheduleNotifications -> services.schedulePrayerNotifications()
                }
            }
        },
    )
}
