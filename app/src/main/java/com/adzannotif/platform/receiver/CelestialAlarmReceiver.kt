package com.adzannotif.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adzannotif.platform.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CelestialAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CELESTIAL_ALARM) return

        val eventTypeStr = intent.getStringExtra(EXTRA_EVENT_TYPE) ?: return
        val eventLabel = intent.getStringExtra(EXTRA_EVENT_LABEL) ?: return
        if (eventLabel.isBlank()) return
        notificationHelper.showCelestialEventNotification(eventTypeStr, eventLabel)
    }

    companion object {
        const val ACTION_CELESTIAL_ALARM = "com.adzannotif.ACTION_CELESTIAL_ALARM"
        const val EXTRA_EVENT_TYPE = "extra_event_type"
        const val EXTRA_EVENT_LABEL = "extra_event_label"
    }
}
