package com.adzannotif.core.astronomy

data class HijriDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val monthName: String
) {
    companion object {
        val MONTH_NAMES = listOf(
            "Muharram", "Safar", "Rabi'ul Awwal", "Rabi'ul Akhir",
            "Jumadil Awwal", "Jumadil Akhir", "Rajab", "Sya'ban",
            "Ramadhan", "Syawwal", "Dzulqa'dah", "Dzulhijjah"
        )
    }
}
