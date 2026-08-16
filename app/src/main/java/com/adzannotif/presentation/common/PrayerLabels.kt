package com.adzannotif.presentation.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.adzannotif.R
import com.adzannotif.core.prayer.Prayer

@Composable
fun Prayer.localizedName(): String = stringResource(prayerLabelRes())

@StringRes
private fun Prayer.prayerLabelRes(): Int = when (this) {
    Prayer.IMSAK -> R.string.prayer_imsak
    Prayer.FAJR -> R.string.prayer_fajr
    Prayer.SUNRISE -> R.string.prayer_sunrise
    Prayer.DHUHR -> R.string.prayer_dhuhr
    Prayer.ASR -> R.string.prayer_asr
    Prayer.MAGHRIB -> R.string.prayer_maghrib
    Prayer.ISHA -> R.string.prayer_isha
    Prayer.MIDNIGHT -> R.string.prayer_midnight
    Prayer.TAHAJJUD -> R.string.prayer_tahajjud
}
