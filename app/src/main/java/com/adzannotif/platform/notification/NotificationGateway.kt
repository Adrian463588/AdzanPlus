package com.adzannotif.platform.notification

import com.adzannotif.core.prayer.Prayer

/** Platform notification boundary; implementations must preserve real event data. */
interface NotificationGateway {
    fun showAdhanNotification(
        prayer: Prayer,
        prayerTitle: String,
        locationName: String,
        vibrate: Boolean = true,
    )

    fun showBeepNotification(
        prayer: Prayer,
        prayerTitle: String,
        locationName: String,
        vibrate: Boolean = true,
    )

    fun showSilentNotification(
        prayer: Prayer,
        prayerTitle: String,
        locationName: String,
        vibrate: Boolean = true,
    )

    fun showPreReminderNotification(
        prayer: Prayer,
        minutesBefore: Int,
        locationName: String,
        vibrate: Boolean = true,
    )

    fun showCelestialEventNotification(eventType: String, label: String)

    fun areNotificationsEnabled(): Boolean
}
