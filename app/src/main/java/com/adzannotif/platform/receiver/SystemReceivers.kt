package com.adzannotif.platform.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.adzannotif.platform.alarm.AlarmScheduler
import com.adzannotif.platform.audio.AudioGateway
import com.adzannotif.platform.worker.PrayerSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles device boot completion and app update broadcasts.
 * Reschedules alarms and ensures daily sync worker is active.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            // Room/DataStore are credential-protected. Do not touch them before the
            // user unlocks the device; the normal BOOT_COMPLETED broadcast follows.
            Log.i("BootReceiver", "Locked boot received; deferring reconciliation until user unlock")
            return
        }

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Received $action, rescheduling alarms & enqueuing sync worker")
            PrayerSyncWorker.enqueuePeriodicWork(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    alarmScheduler.schedule().onFailure { error ->
                        Log.e("BootReceiver", "Prayer alarm reconciliation was not completed", error)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

/**
 * Handles device time, date, and timezone changes.
 * Immediately recalculates and re-arms prayer alarms to prevent drift.
 */
@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            Log.d("TimeChangeReceiver", "Received $action, updating prayer alarm schedule")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    alarmScheduler.schedule().onFailure { error ->
                        Log.e("TimeChangeReceiver", "Prayer alarm reconciliation was not completed", error)
                    }
                } catch (e: Exception) {
                    Log.e("TimeChangeReceiver", "Failed to reschedule alarms on time change", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

/**
 * Handles exact alarm permission state changes (Android 12+ API 31+).
 * When user grants or revokes SCHEDULE_EXACT_ALARM permission in system settings.
 */
@AndroidEntryPoint
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                Log.d("ExactAlarmPermissionReceiver", "Exact alarm permission state changed, re-evaluating alarms")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        alarmScheduler.schedule().onFailure { error ->
                            Log.e("ExactAlarmPermissionReceiver", "Prayer alarm reconciliation was not completed", error)
                        }
                    } catch (e: Exception) {
                        Log.e("ExactAlarmPermissionReceiver", "Failed to reschedule alarms on permission change", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}

/**
 * Handles action to stop adhan audio playback and dismiss notification.
 */
@AndroidEntryPoint
class StopAdhanReceiver : BroadcastReceiver() {

    @Inject
    lateinit var audioGateway: AudioGateway

    companion object {
        const val ACTION_STOP_ADHAN = "com.adzannotif.ACTION_STOP_ADHAN"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_ADHAN) {
            Log.d("StopAdhanReceiver", "Stopping adhan playback")
            audioGateway.stop()
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notificationId != -1) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(notificationId)
            }
        }
    }
}
