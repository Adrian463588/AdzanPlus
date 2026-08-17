package com.adzannotif.presentation.localization

import android.content.Context
import com.adzannotif.R
import com.adzannotif.core.prayer.Prayer

internal fun prayerLabel(context: Context, prayer: Prayer): String = when (prayer) {
    Prayer.IMSAK -> context.getString(R.string.prayer_imsak)
    Prayer.FAJR -> context.getString(R.string.prayer_fajr)
    Prayer.SUNRISE -> context.getString(R.string.prayer_sunrise)
    Prayer.DHUHR -> context.getString(R.string.prayer_dhuhr)
    Prayer.ASR -> context.getString(R.string.prayer_asr)
    Prayer.MAGHRIB -> context.getString(R.string.prayer_maghrib)
    Prayer.ISHA -> context.getString(R.string.prayer_isha)
    Prayer.MIDNIGHT -> context.getString(R.string.prayer_midnight)
    Prayer.TAHAJJUD -> context.getString(R.string.prayer_tahajjud)
}
