package com.adzannotif.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.unit.ColorProvider

/** Keeps astronomy widgets readable in both launcher light and dark themes. */
internal object AstronomyWidgetPalette {
    val primaryText = dayNight(Color(0xFF191C1B), Color(0xFFF0F4FF))
    val secondaryText = dayNight(Color(0xFF3F4945), Color(0xFFBFC9C3))
    val moonAccent = dayNight(Color(0xFF7A5900), Color(0xFFFAC248))
    val sunAccent = dayNight(Color(0xFF8A4B00), Color(0xFFFFB347))

    fun moonSurface(context: Context): ColorProvider = ColorProvider(
        if (isNight(context)) Color(0xFF111D30) else Color(0xFFF6F9F7),
    )

    fun sunSurface(context: Context): ColorProvider = ColorProvider(
        if (isNight(context)) Color(0xFF0B1525) else Color(0xFFFFFBF3),
    )

    private fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private fun dayNight(day: Color, night: Color): ColorProvider = DayNightColorProvider(day, night)
}
