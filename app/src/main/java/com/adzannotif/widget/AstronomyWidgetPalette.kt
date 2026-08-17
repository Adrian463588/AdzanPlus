package com.adzannotif.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/** Keeps astronomy widgets readable in both launcher light and dark themes. */
internal object AstronomyWidgetPalette {
    val moonBackground = adaptive(Color(0xFFF6F9F7), Color(0xFF111D30))
    val sunBackground = adaptive(Color(0xFFFFFBF3), Color(0xFF0B1525))
    val primaryText = adaptive(Color(0xFF191C1B), Color(0xFFF0F4FF))
    val secondaryText = adaptive(Color(0xFF3F4945), Color(0xFFBFC9C3))
    val moonAccent = adaptive(Color(0xFF7A5900), Color(0xFFFAC248))
    val sunAccent = adaptive(Color(0xFF8A4B00), Color(0xFFFFB347))

    private fun adaptive(day: Color, night: Color): ColorProvider = object : ColorProvider {
        override fun getColor(context: Context): Color {
            val isNight = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            return if (isNight) night else day
        }
    }
}
