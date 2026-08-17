package com.adzannotif.presentation.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.adzannotif.core.astronomy.AstronomyEngine
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

internal object PrayerWidgetRoute {
    private const val ACTION_OPEN_ROUTE = "com.adzannotif.OPEN_ASTRONOMY_ROUTE"
    private const val EXTRA_ROUTE = "com.adzannotif.extra.ASTRONOMY_ROUTE"

    fun settingsIntent(context: Context): Intent = Intent(
        context,
        com.adzannotif.MainActivity::class.java,
    ).apply {
        action = ACTION_OPEN_ROUTE
        data = Uri.parse("adzannotif://${com.adzannotif.presentation.common.Screen.Settings.route}")
        putExtra(EXTRA_ROUTE, com.adzannotif.presentation.common.Screen.Settings.route)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PrayerWidgetEntryPoint {
    fun locationRepository(): LocationRepository
    fun prayerTimesRepository(): PrayerTimesRepository
    fun settingsRepository(): SettingsRepository
    fun astronomyEngine(): AstronomyEngine
}

class PrayerTimesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(200.dp, 110.dp),
            DpSize(180.dp, 150.dp),
            DpSize(250.dp, 220.dp),
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
            astronomyEngine = entryPoint.astronomyEngine(),
        )
        provideContent {
            WidgetRenderer.Content(snapshot)
        }
    }
}
