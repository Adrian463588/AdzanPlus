package com.adzannotif.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adzannotif.domain.model.astronomy.SkyEventType
import com.adzannotif.platform.notification.NotificationGateway
import com.adzannotif.presentation.localization.astronomyEventLabel
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
        val eventType = runCatching { SkyEventType.valueOf(eventTypeStr) }.getOrNull() ?: return
        val eventLabel = astronomyEventLabel(context, eventType.name)
        notificationGateway.showCelestialEventNotification(eventTypeStr, eventLabel)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AstronomyWidgetUpdater.updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CELESTIAL_ALARM = "com.adzannotif.ACTION_CELESTIAL_ALARM"
        const val EXTRA_EVENT_TYPE = "extra_event_type"
    }
}
