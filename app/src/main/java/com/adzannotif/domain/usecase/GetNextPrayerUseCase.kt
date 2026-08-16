package com.adzannotif.domain.usecase

import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.PrayerTimeRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

data class NextPrayerInfo(
    val currentPrayer: Prayer?,
    val nextPrayer: Prayer,
    val targetTime: Instant,
    val todayRecord: PrayerTimeRecord,
)

class GetNextPrayerUseCase @Inject constructor(
    private val getTodayPrayerTimesUseCase: GetTodayPrayerTimesUseCase,
) {
    operator fun invoke(): Flow<NextPrayerInfo?> {
        return getTodayPrayerTimesUseCase().map { todayRecord ->
            val now = Clock.System.now()
            val nextPair = todayRecord.findNextPrayer(now)
            val current = todayRecord.findCurrentPrayer(now)

            if (nextPair != null) {
                NextPrayerInfo(
                    currentPrayer = current,
                    nextPrayer = nextPair.first,
                    targetTime = nextPair.second,
                    todayRecord = todayRecord,
                )
            } else {
                // If past Isha, the next prayer is tomorrow's Fajr
                NextPrayerInfo(
                    currentPrayer = Prayer.ISHA,
                    nextPrayer = Prayer.FAJR,
                    targetTime = todayRecord.fajr,
                    todayRecord = todayRecord,
                )
            }
        }
    }
}
