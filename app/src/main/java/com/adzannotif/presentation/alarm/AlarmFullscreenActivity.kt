package com.adzannotif.presentation.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adzannotif.domain.model.ThemeMode
import com.adzannotif.platform.audio.AudioGateway
import com.adzannotif.platform.audio.AdhanPlaybackService
import com.adzannotif.presentation.theme.AdzanNotifTheme
import com.adzannotif.presentation.common.rememberMotionAnimationsEnabled
import com.adzannotif.R
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.repository.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@AndroidEntryPoint
class AlarmFullscreenActivity : ComponentActivity() {

    @Inject
    lateinit var audioGateway: AudioGateway

    @Inject
    lateinit var alarmRepository: AlarmRepository

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TITLE = "extra_prayer_title"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndShowOnLockScreen()

        val prayer = intent.getStringExtra(EXTRA_PRAYER_NAME)
            ?.let { name -> runCatching { Prayer.valueOf(name) }.getOrNull() }
        val prayerTitle = intent.getStringExtra(EXTRA_PRAYER_TITLE)
            ?.takeIf(String::isNotBlank)
            ?: getString(R.string.alarm_default_title)
        val locationName = intent.getStringExtra(EXTRA_LOCATION_NAME)
            ?.takeIf(String::isNotBlank)
            ?: getString(R.string.location_unavailable)

        setContent {
            AdzanNotifTheme(themeMode = ThemeMode.DARK) {
                AlarmFullscreenScreen(
                    prayerTitle = prayerTitle,
                    locationName = locationName,
                    onDismiss = {
                        AdhanPlaybackService.stop(this)
                        audioGateway.stop()
                        finish()
                    },
                    onSnooze = {
                        AdhanPlaybackService.stop(this)
                        audioGateway.stop()
                        prayer?.let { prayerToSnooze ->
                            CoroutineScope(Dispatchers.Default).launch {
                                val snoozeInstant = Clock.System.now().plus(10.minutes)
                                alarmRepository.scheduleExactAlarm(
                                    prayer = prayerToSnooze,
                                    targetInstant = snoozeInstant,
                                    title = getString(R.string.alarm_snooze_title, prayerTitle),
                                    isPreReminder = false,
                                )
                            }
                        }
                        finish()
                    }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun turnScreenOnAndShowOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun AlarmFullscreenScreen(
    prayerTitle: String,
    locationName: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val pulseScale = if (rememberMotionAnimationsEnabled()) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "scale",
        )
        animatedScale
    } else {
        1f
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.ROOT)
    val currentTime = timeFormat.format(Date())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071F17),
                        Color(0xFF0F3E30),
                        Color(0xFF05120E)
                    )
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Top: Location
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
                Text(
                    text = locationName.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PANGGILAN ADZAN",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Center: Pulsating Icon & Prayer Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = androidx.compose.ui.res.stringResource(R.string.alarm_adhan_content_description),
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = prayerTitle,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Bottom: Action buttons (Dismiss / Snooze)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.AlarmOff, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.alarm_dismiss), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Snooze, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.alarm_snooze))
                }
            }
        }
    }
}
