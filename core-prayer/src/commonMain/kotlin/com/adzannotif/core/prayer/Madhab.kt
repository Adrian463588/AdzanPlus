package com.adzannotif.core.prayer

import kotlinx.serialization.Serializable

/**
 * Islamic juristic school for calculating Asr prayer time.
 *
 * @property shadowFactor The multiple of an object's shadow length used to determine Asr.
 */
@Serializable
enum class Madhab(val shadowFactor: Double, val displayName: String) {
    /**
     * Shafi, Maliki, and Hanbali schools: Asr begins when the shadow length of an object
     * equals its height plus the shadow at solar noon (shadow factor = 1.0).
     */
    SHAFI(1.0, "Syafi'i / Maliki / Hanbali"),

    /**
     * Hanafi school: Asr begins when the shadow length of an object
     * equals twice its height plus the shadow at solar noon (shadow factor = 2.0).
     */
    HANAFI(2.0, "Hanafi")
}
