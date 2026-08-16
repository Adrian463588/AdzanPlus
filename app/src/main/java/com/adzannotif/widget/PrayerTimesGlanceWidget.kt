package com.adzannotif.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.text.Text
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.usecase.GetTodayPrayerTimesUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

class PrayerTimesGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Text(text = "AdzanNotif Widget")
        }
    }
}

@AndroidEntryPoint
class PrayerTimesGlanceReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = PrayerTimesGlanceWidget()

    @Inject
    lateinit var getTodayPrayerTimesUseCase: GetTodayPrayerTimesUseCase

    @Inject
    lateinit var locationRepository: LocationRepository

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val todayRecord = getTodayPrayerTimesUseCase().first()
                val location = locationRepository.currentOrSelectedLocation.first()
                val now = Clock.System.now()
                val nextPrayerPair = todayRecord.findNextPrayer(now)
                val nextName = nextPrayerPair?.first?.displayNameId ?: "Subuh"
                val nextTarget = nextPrayerPair?.second ?: todayRecord.fajr

                for (appWidgetId in appWidgetIds) {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)

                    val remoteViews = if (minWidth >= 250) {
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
            } finally {
                pendingResult.finish()
            }
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
}
