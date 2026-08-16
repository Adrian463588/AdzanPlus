package com.adzannotif

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.adzannotif.platform.worker.PrayerSyncWorker
import com.adzannotif.platform.alarm.AdhanScheduler
import com.adzannotif.widget.PrayerTimesWidgetReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AdzanApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var adhanScheduler: AdhanScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Enqueue daily midnight reconciliation worker
        PrayerSyncWorker.enqueuePeriodicWork(this)
        
        // Immediately arm all upcoming prayer alarms and sync widgets
        adhanScheduler.rescheduleAllAlarms()
        PrayerTimesWidgetReceiver.updateAll(this)
    }
}
