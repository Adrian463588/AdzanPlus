package com.adzannotif.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.PrayerTimesRepository
import com.adzannotif.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in UPDATE_ACTIONS) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                try {
                    glanceAppWidget.updateAll(context.applicationContext)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_PRAYER_WIDGET = "com.adzannotif.widget.ACTION_UPDATE_PRAYER_WIDGET"

        private val UPDATE_ACTIONS = setOf(
            ACTION_UPDATE_PRAYER_WIDGET,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )

        fun updateAll(context: Context) {
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                PrayerTimesWidget().updateAll(context.applicationContext)
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PrayerWidgetEntryPoint {
    fun locationRepository(): LocationRepository
    fun prayerTimesRepository(): PrayerTimesRepository
    fun settingsRepository(): SettingsRepository
}

class PrayerTimesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 180.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PrayerWidgetEntryPoint::class.java,
        )
        val snapshot = PrayerWidgetSnapshotLoader.load(
            locationRepository = entryPoint.locationRepository(),
            prayerTimesRepository = entryPoint.prayerTimesRepository(),
            settingsRepository = entryPoint.settingsRepository(),
        )
        provideContent {
            WidgetRenderer.Content(snapshot)
        }
    }
}
