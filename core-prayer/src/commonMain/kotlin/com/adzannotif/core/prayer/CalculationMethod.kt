package com.adzannotif.core.prayer

import kotlinx.serialization.Serializable

/**
 * Pre-configured calculation methods established by recognized Islamic authorities worldwide.
 */
@Serializable
enum class CalculationMethod(
    val organizationName: String,
    val region: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaInterval: Int = 0,
    val maghribAngle: Double = 0.0
) {
    /**
     * Kementerian Agama Republik Indonesia (Kemenag RI).
     * Standard hisab for Indonesia: Fajr 20.0°, Isha 18.0°.
     */
    KEMENAG_RI(
        organizationName = "Kementerian Agama RI (Indonesia)",
        region = "Indonesia",
        fajrAngle = 20.0,
        ishaAngle = 18.0
    ),

    /**
     * Muslim World League (MWL).
     * Standard: Fajr 18.0°, Isha 17.0°. Used in Europe, Far East, and parts of USA.
     */
    MUSLIM_WORLD_LEAGUE(
        organizationName = "Muslim World League (MWL)",
        region = "Europe, Far East, Global",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),

    /**
     * Egyptian General Authority of Survey.
     * Standard: Fajr 19.5°, Isha 17.5°. Used in Egypt, Africa, Syria, Lebanon, Malaysia.
     */
    EGYPTIAN(
        organizationName = "Egyptian General Authority of Survey",
        region = "Egypt, Africa, Middle East",
        fajrAngle = 19.5,
        ishaAngle = 17.5
    ),

    /**
     * Umm Al-Qura University, Makkah.
     * Standard: Fajr 18.5°, Isha is 90 minutes after Maghrib (120 minutes in Ramadan).
     */
    UMM_AL_QURA(
        organizationName = "Umm Al-Qura University, Makkah",
        region = "Saudi Arabia, Arabian Peninsula",
        fajrAngle = 18.5,
        ishaAngle = 0.0,
        ishaInterval = 90
    ),

    /**
     * Gulf 90 Minute Method.
     * Standard: Fajr 19.5°, Isha 90 minutes after Maghrib.
     */
    GULF(
        organizationName = "Gulf Region Standard",
        region = "UAE, Bahrain, Oman",
        fajrAngle = 19.5,
        ishaAngle = 0.0,
        ishaInterval = 90
    ),

    /**
     * Ministry of Awqaf and Islamic Affairs, Kuwait.
     * Standard: Fajr 18.0°, Isha 17.5°.
     */
    KUWAIT(
        organizationName = "Ministry of Awqaf, Kuwait",
        region = "Kuwait",
        fajrAngle = 18.0,
        ishaAngle = 17.5
    ),

    /**
     * Ministry of Awqaf and Islamic Affairs, Qatar.
     * Standard: Fajr 18.0°, Isha 90 minutes after Maghrib.
     */
    QATAR(
        organizationName = "Ministry of Awqaf, Qatar",
        region = "Qatar",
        fajrAngle = 18.0,
        ishaAngle = 0.0,
        ishaInterval = 90
    ),

    /**
     * Majlis Ugama Islam Singapura (MUIS).
     * Standard: Fajr 20.0°, Isha 18.0°.
     */
    SINGAPORE_MUIS(
        organizationName = "Majlis Ugama Islam Singapura (MUIS)",
        region = "Singapore, Southeast Asia",
        fajrAngle = 20.0,
        ishaAngle = 18.0
    ),

    /**
     * University of Islamic Sciences, Karachi.
     * Standard: Fajr 18.0°, Isha 18.0°. Used in Pakistan, India, Bangladesh, Afghanistan.
     */
    KARACHI(
        organizationName = "University of Islamic Sciences, Karachi",
        region = "Pakistan, India, Bangladesh",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),

    /**
     * Institute of Geophysics, University of Tehran.
     * Standard: Fajr 17.7°, Isha 14.0°, Maghrib 4.5°.
     */
    TEHRAN(
        organizationName = "Institute of Geophysics, Tehran",
        region = "Iran, Shia Communities",
        fajrAngle = 17.7,
        ishaAngle = 14.0,
        maghribAngle = 4.5
    ),

    /**
     * Islamic Society of North America (ISNA).
     * Standard: Fajr 15.0°, Isha 15.0°.
     */
    NORTH_AMERICA(
        organizationName = "Islamic Society of North America (ISNA)",
        region = "USA, Canada",
        fajrAngle = 15.0,
        ishaAngle = 15.0
    ),

    /**
     * Custom method allowing full manual configuration of angles and intervals.
     */
    CUSTOM(
        organizationName = "Custom / Manual",
        region = "Custom",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    );

    /**
     * Generates a default [CalculationParameters] instance for this method.
     */
    fun createParameters(
        madhab: Madhab = Madhab.SHAFI,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
        prayerAdjustments: PrayerAdjustments = if (this == KEMENAG_RI) PrayerAdjustments.KEMENAG_DEFAULT_IHTIYATH else PrayerAdjustments.ZERO,
        rounding: RoundingType = RoundingType.NEAREST
    ): CalculationParameters {
        return CalculationParameters(
            method = this,
            fajrAngle = this.fajrAngle,
            ishaAngle = this.ishaAngle,
            ishaInterval = this.ishaInterval,
            maghribAngle = this.maghribAngle,
            madhab = madhab,
            highLatitudeRule = highLatitudeRule,
            prayerAdjustments = prayerAdjustments,
            rounding = rounding
        )
    }
}
