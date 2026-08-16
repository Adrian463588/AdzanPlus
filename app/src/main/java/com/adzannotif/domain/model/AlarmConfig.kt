package com.adzannotif.domain.model

import com.adzannotif.core.prayer.Prayer
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

@Serializable
data class AllAlarmSettings(
    val fajr: AlarmConfig = AlarmConfig(Prayer.FAJR, adhanVoice = AdhanVoice.FAJR_SPECIAL),
    val sunrise: AlarmConfig = AlarmConfig(Prayer.SUNRISE, isEnabled = false, soundType = AdhanSoundType.BEEP_NOTIFICATION),
    val dhuhr: AlarmConfig = AlarmConfig(Prayer.DHUHR),
    val asr: AlarmConfig = AlarmConfig(Prayer.ASR),
    val maghrib: AlarmConfig = AlarmConfig(Prayer.MAGHRIB),
    val isha: AlarmConfig = AlarmConfig(Prayer.ISHA),
    val dndAutoSilenceMinutes: Int = 15,
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
