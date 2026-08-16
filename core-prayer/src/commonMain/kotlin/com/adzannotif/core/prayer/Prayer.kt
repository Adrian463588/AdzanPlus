package com.adzannotif.core.prayer

import kotlinx.serialization.Serializable

/**
 * Enumeration of Islamic prayers and significant solar/sharia times.
 */
@Serializable
enum class Prayer(val displayNameEn: String, val displayNameId: String, val isFardPrayer: Boolean) {
    IMSAK("Imsak", "Imsak", false),
    FAJR("Fajr", "Subuh", true),
    SUNRISE("Sunrise", "Terbit / Syuruq", false),
    DHUHR("Dhuhr", "Dzuhur", true),
    ASR("Asr", "Ashar", true),
    MAGHRIB("Maghrib", "Maghrib", true),
    ISHA("Isha", "Isya", true),
    MIDNIGHT("Midnight", "Tengah Malam", false),
    TAHAJJUD("Tahajjud", "Tahajjud / Sepertiga Malam", false);

    companion object {
        /**
         * The 5 mandatory daily prayers in chronological order.
         */
        val FARD_PRAYERS = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)

        /**
         * All standard display timeline prayers including Sunrise.
         */
        val STANDARD_TIMELINE = listOf(FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA)
    }
}
