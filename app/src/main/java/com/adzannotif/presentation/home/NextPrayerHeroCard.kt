package com.adzannotif.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adzannotif.R
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.presentation.common.localizedName
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

@Composable
fun NextPrayerHeroCard(
    nextPrayer: Prayer,
    targetInstant: Instant,
    countdownSecondsRemaining: Long,
    location: LocationInfo,
    hijriDateFormatted: String?,
    modifier: Modifier = Modifier,
) {
    val targetLocal = targetInstant.toLocalDateTime(TimeZone.of(location.timeZoneId))
    val targetFormatted = String.format(Locale.US, "%02d:%02d", targetLocal.hour, targetLocal.minute)

    val safeSeconds = maxOf(0L, countdownSecondsRemaining)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    val countdownFormatted = String.format(Locale.US, "-%02d:%02d:%02d", hours, minutes, seconds)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row: Location chip + Hijri Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Lokasi",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = location.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = hijriDateFormatted ?: stringResource(R.string.hijri_unavailable),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Center: Next Prayer Title
                Text(
                    text = stringResource(R.string.remaining_time).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = nextPrayer.localizedName().uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                val tzSuffix = when (location.timeZoneId) {
                    "Asia/Jakarta", "Asia/Pontianak" -> "WIB"
                    "Asia/Makassar", "Asia/Ujung_Pandang", "Asia/Bali" -> "WITA"
                    "Asia/Jayapura" -> "WIT"
                    "Asia/Riyadh" -> "AST"
                    else -> ""
                }
                val timeLabel = if (tzSuffix.isNotEmpty()) {
                    stringResource(R.string.prayer_time_with_zone, targetFormatted, tzSuffix)
                } else {
                    stringResource(R.string.prayer_time, targetFormatted)
                }

                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                val prayerName = nextPrayer.localizedName()
                val countdownDesc = stringResource(
                    R.string.next_prayer_countdown_description,
                    prayerName,
                    countdownFormatted
                )

                // Large Countdown Display Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    ),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = countdownFormatted,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .semantics {
                                contentDescription = countdownDesc
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                }
            }
        }
    }
}
