package com.adzannotif.platform.audio

import com.adzannotif.domain.model.AdhanVoice

/** Platform audio boundary; a missing source remains unavailable. */
interface AudioGateway {
    fun playAdhan(
        voice: AdhanVoice,
        customUriString: String? = null,
        durationMinutes: Int = 15,
        onCompletion: (() -> Unit)? = null,
    )

    fun stop()
}
