package com.adzannotif.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.usecase.GetNextPrayerUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AppWidgetProvider managing home screen prayer schedule widgets (Compact and Detailed),
 * supporting live SystemUI Chronometer countdowns without polling.
 */
@AndroidEntryPoint
class PrayerTimesWidgetReceiver : AppWidgetProvider() {

    @Inject
    lateinit var getNextPrayerUseCase: GetNextPrayerUseCase

    @Inject
    lateinit var locationRepository: LocationRepository

    companion object {
        private const val TAG = "PrayerTimesWidget"
        const val ACTION_UPDATE_PRAYER_WIDGET = "com.adzannotif.widget.ACTION_UPDATE_PRAYER_WIDGET"

        /**
         * Broadcast helper to instantly refresh all active home screen widgets.
         */
        fun updateAll(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, PrayerTimesWidgetReceiver::class.java)
                val ids = appWidgetManager.getAppWidgetIds(componentName)
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, PrayerTimesWidgetReceiver::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to broadcast widget update", e)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (appWidgetIds.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val location = locationRepository.currentOrSelectedLocation.first()
                if (location == null) {
                    updateUnavailableWidgets(context, appWidgetManager, appWidgetIds)
                    return@launch
                }
                val nextInfo = getNextPrayerUseCase().first()
                if (nextInfo == null) {
                    updateUnavailableWidgets(context, appWidgetManager, appWidgetIds)
                    return@launch
                }

                val nextName = nextInfo.nextPrayer.displayNameId
                val nextTarget = nextInfo.targetTime
                val todayRecord = nextInfo.todayRecord

                for (appWidgetId in appWidgetIds) {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)

                    val remoteViews = if (minWidth >= 220) {
                        WidgetRenderer.buildDetailedWidget(
                            context = context,
                            location = location,
                            nextPrayerName = nextName,
                            targetInstant = nextTarget,
                            record = todayRecord
                        )
                    } else {
                        WidgetRenderer.buildCompactWidget(
                            context = context,
                            location = location,
                            nextPrayerName = nextName,
                            targetInstant = nextTarget
                        )
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating prayer widgets", e)
                updateUnavailableWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun updateUnavailableWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            appWidgetManager.updateAppWidget(
                appWidgetId,
                WidgetRenderer.buildUnavailableWidget(context, detailed = minWidth >= 220),
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_UPDATE_PRAYER_WIDGET ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, PrayerTimesWidgetReceiver::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                onUpdate(context, appWidgetManager, ids)
            }
        }
    }
}
