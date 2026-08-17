package com.adzannotif.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AlarmConfig
import com.adzannotif.presentation.common.localizedName
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

@Composable
fun PrayerTimeRow(
    prayer: Prayer,
    timeInstant: Instant,
    timeZoneId: String,
    isActivePrayer: Boolean,
    alarmConfig: AlarmConfig,
    onToggleAlarm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localTime = timeInstant.toLocalDateTime(TimeZone.of(timeZoneId))
    val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", localTime.hour, localTime.minute)

    val icon: ImageVector = when (prayer) {
        Prayer.IMSAK -> Icons.Default.WbTwilight
        Prayer.FAJR -> Icons.Default.Brightness5
        Prayer.SUNRISE -> Icons.Default.WbSunny
        Prayer.DHUHR -> Icons.Default.Brightness7
        Prayer.ASR -> Icons.Default.Brightness6
        Prayer.MAGHRIB -> Icons.Default.WbTwilight
        Prayer.ISHA -> Icons.Default.NightsStay
        else -> Icons.Default.WbSunny
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActivePrayer) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isActivePrayer) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActivePrayer) 1.5.dp else 1.dp,
            color = if (isActivePrayer) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActivePrayer) 3.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isActivePrayer) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (isActivePrayer) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                            modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = prayer.displayNameId,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = prayer.localizedName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActivePrayer) FontWeight.Bold else FontWeight.Medium
                )
            }

            // Right: Time + Alarm toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isActivePrayer) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (prayer.isFardPrayer) {
                    IconButton(
                        onClick = onToggleAlarm,
                    ) {
                        Icon(
                            imageVector = if (alarmConfig.isEnabled) {
                                Icons.Default.Notifications
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = androidx.compose.ui.res.stringResource(
                                if (alarmConfig.isEnabled) {
                                    com.adzannotif.R.string.disable_prayer_alarm
                                } else {
                                    com.adzannotif.R.string.enable_prayer_alarm
                                },
                                prayer.localizedName(),
                            ),
                            tint = if (alarmConfig.isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
