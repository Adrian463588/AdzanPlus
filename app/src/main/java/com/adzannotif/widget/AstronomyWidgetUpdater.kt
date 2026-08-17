package com.adzannotif.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Refreshes all astronomy widgets after a lifecycle or time-zone boundary. */
object AstronomyWidgetUpdater {
    suspend fun updateAll(context: Context) {
        MoonWidget().updateAll(context)
        SunWidget().updateAll(context)
    }
}
