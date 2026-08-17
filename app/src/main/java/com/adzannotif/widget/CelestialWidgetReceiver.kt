package com.adzannotif.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object CelestialWidgetRoute {
    private const val ACTION_OPEN_ROUTE = "com.adzannotif.OPEN_ASTRONOMY_ROUTE"
    private const val EXTRA_ROUTE = "com.adzannotif.extra.ASTRONOMY_ROUTE"

    fun intent(context: Context, route: String): Intent = Intent(context, com.adzannotif.MainActivity::class.java).apply {
        action = ACTION_OPEN_ROUTE
        data = Uri.parse("adzannotif://$route")
        putExtra(EXTRA_ROUTE, route)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}

class MoonWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoonWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (AstronomyWidgetUpdater.isRefreshTrigger(intent.action)) {
            refresh(context, intent.action, goAsync())
        }
    }
}

class SunWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SunWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (AstronomyWidgetUpdater.isRefreshTrigger(intent.action)) {
            refresh(context, intent.action, goAsync())
        }
    }
}

private fun refresh(
    context: Context,
    action: String?,
    pendingResult: BroadcastReceiver.PendingResult,
) {
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            AstronomyWidgetUpdater.updateForTrigger(context.applicationContext, action)
        } finally {
            pendingResult.finish()
        }
    }
}
