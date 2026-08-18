package com.adzannotif.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll

/** Refreshes all astronomy widgets after a lifecycle or time-zone boundary. */
object AstronomyWidgetUpdater {
    const val ACTION_REFRESH = "com.adzannotif.widget.ACTION_UPDATE_ASTRONOMY"

    fun isRefreshTrigger(action: String?): Boolean = action in REFRESH_ACTIONS

    suspend fun updateAll(context: Context) {
        AstronomyWidget().updateAll(context)
    }

    suspend fun updateForTrigger(context: Context, action: String?): Boolean {
        if (!isRefreshTrigger(action)) return false
        updateAll(context)
        return true
    }

    private val REFRESH_ACTIONS = setOf(
        ACTION_REFRESH,
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )
}
