package com.adzannotif.domain.model

import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.astronomy.SkyEventType
import kotlinx.serialization.Serializable

enum class AdhanSoundType {
    FULL_ADHAN,
    SHORT_TAKBEER,
    BEEP_NOTIFICATION,
    SILENT
}

enum class AdhanVoice(val title: String, val rawResName: String) {
    MAKKAH("Adzan Makkah", "adhan_makkah"),
    MADINAH("Adzan Madinah", "adhan_madinah"),
    AL_AQSA("Adzan Al-Aqsa", "adhan_alaqsa"),
    EGYPT("Adzan Mesir", "adhan_egypt"),
    KUWAIT("Adzan Kuwait (Misyari)", "adhan_kuwait"),
    FAJR_SPECIAL("Adzan Subuh Khusus", "adhan_fajr"),
    SYSTEM_DEFAULT("Nada Dering Sistem", "system_default")
}

/**
 * Domain model representing notification and alarm configuration for a specific prayer.
 */
@Serializable
data class AlarmConfig(
    val prayer: Prayer,
    val isEnabled: Boolean = true,
    val soundType: AdhanSoundType = AdhanSoundType.FULL_ADHAN,
    val adhanVoice: AdhanVoice = if (prayer == Prayer.FAJR) AdhanVoice.FAJR_SPECIAL else AdhanVoice.MAKKAH,
    val customSoundUri: String? = null,
    val isVibrate: Boolean = true,
    val preReminderMinutes: Int = 0, // 0 = off, 5, 10, 15
)

enum class CelestialAlertType {
    GOLDEN_HOUR_START,
    BLUE_HOUR_START,
    MOONRISE,
    MOONSET,
    FULL_MOON,
    NEW_MOON,
}

@Serializable
data class CelestialAlertSettings(
    val goldenHourStart: Boolean = false,
    val blueHourStart: Boolean = false,
    val moonrise: Boolean = false,
    val moonset: Boolean = false,
    val fullMoon: Boolean = false,
    val newMoon: Boolean = false,
    val minutesBefore: Int = 0,
) {
    fun isEnabled(eventType: SkyEventType): Boolean = when (eventType) {
        SkyEventType.GOLDEN_HOUR_MORNING_START,
        SkyEventType.GOLDEN_HOUR_EVENING_START -> goldenHourStart
        SkyEventType.BLUE_HOUR_MORNING_START,
        SkyEventType.BLUE_HOUR_EVENING_START -> blueHourStart
        SkyEventType.MOONRISE -> moonrise
        SkyEventType.MOONSET -> moonset
        SkyEventType.FULL_MOON -> fullMoon
        SkyEventType.NEW_MOON -> newMoon
        else -> false
    }

    fun isEnabled(alertType: CelestialAlertType): Boolean = when (alertType) {
        CelestialAlertType.GOLDEN_HOUR_START -> goldenHourStart
        CelestialAlertType.BLUE_HOUR_START -> blueHourStart
        CelestialAlertType.MOONRISE -> moonrise
        CelestialAlertType.MOONSET -> moonset
        CelestialAlertType.FULL_MOON -> fullMoon
        CelestialAlertType.NEW_MOON -> newMoon
    }

    fun withEnabled(alertType: CelestialAlertType, enabled: Boolean): CelestialAlertSettings = when (alertType) {
        CelestialAlertType.GOLDEN_HOUR_START -> copy(goldenHourStart = enabled)
        CelestialAlertType.BLUE_HOUR_START -> copy(blueHourStart = enabled)
        CelestialAlertType.MOONRISE -> copy(moonrise = enabled)
        CelestialAlertType.MOONSET -> copy(moonset = enabled)
        CelestialAlertType.FULL_MOON -> copy(fullMoon = enabled)
        CelestialAlertType.NEW_MOON -> copy(newMoon = enabled)
    }
}

@Serializable
data class AllAlarmSettings(
    val fajr: AlarmConfig = AlarmConfig(Prayer.FAJR, adhanVoice = AdhanVoice.FAJR_SPECIAL),
    val sunrise: AlarmConfig = AlarmConfig(Prayer.SUNRISE, isEnabled = false, soundType = AdhanSoundType.BEEP_NOTIFICATION),
    val dhuhr: AlarmConfig = AlarmConfig(Prayer.DHUHR),
    val asr: AlarmConfig = AlarmConfig(Prayer.ASR),
    val maghrib: AlarmConfig = AlarmConfig(Prayer.MAGHRIB),
    val isha: AlarmConfig = AlarmConfig(Prayer.ISHA),
    val dndAutoSilenceMinutes: Int = 15,
    val celestialAlerts: CelestialAlertSettings = CelestialAlertSettings(),
) {
    fun getConfigForPrayer(prayer: Prayer): AlarmConfig = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
        Prayer.IMSAK, Prayer.MIDNIGHT, Prayer.TAHAJJUD -> fajr
    }

    fun updateConfig(config: AlarmConfig): AllAlarmSettings = when (config.prayer) {
        Prayer.FAJR -> copy(fajr = config)
        Prayer.SUNRISE -> copy(sunrise = config)
        Prayer.DHUHR -> copy(dhuhr = config)
        Prayer.ASR -> copy(asr = config)
        Prayer.MAGHRIB -> copy(maghrib = config)
        Prayer.ISHA -> copy(isha = config)
        Prayer.IMSAK, Prayer.MIDNIGHT, Prayer.TAHAJJUD -> this
    }
}
