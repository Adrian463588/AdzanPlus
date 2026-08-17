package com.adzannotif

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.adzannotif.platform.worker.PrayerSyncWorker
import com.adzannotif.platform.alarm.AlarmScheduler
import com.adzannotif.presentation.widget.PrayerTimesWidgetReceiver
import com.adzannotif.widget.AstronomyWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AdzanApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Enqueue daily midnight reconciliation worker
        PrayerSyncWorker.enqueuePeriodicWork(this)
        
        // Immediately arm all upcoming prayer alarms and sync widgets
        alarmScheduler.rescheduleAllAlarms()
        PrayerTimesWidgetReceiver.updateAll(this)
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { AstronomyWidgetUpdater.updateAll(this@AdzanApp) }
        }
    }
}
