package com.adzannotif.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adzannotif.platform.notification.NotificationGateway
import com.adzannotif.widget.AstronomyWidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CelestialAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationGateway: NotificationGateway

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CELESTIAL_ALARM) return

        val eventTypeStr = intent.getStringExtra(EXTRA_EVENT_TYPE) ?: return
        val eventLabel = intent.getStringExtra(EXTRA_EVENT_LABEL) ?: return
        if (eventLabel.isBlank()) return
        notificationGateway.showCelestialEventNotification(eventTypeStr, eventLabel)
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { AstronomyWidgetUpdater.updateAll(context) }
        }
    }

    companion object {
        const val ACTION_CELESTIAL_ALARM = "com.adzannotif.ACTION_CELESTIAL_ALARM"
        const val EXTRA_EVENT_TYPE = "extra_event_type"
        const val EXTRA_EVENT_LABEL = "extra_event_label"
    }
}
