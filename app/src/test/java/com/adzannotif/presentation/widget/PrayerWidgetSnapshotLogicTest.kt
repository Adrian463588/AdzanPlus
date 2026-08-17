package com.adzannotif.presentation.widget

import com.adzannotif.core.prayer.Coordinates
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PrayerWidgetSnapshotLogicTest {
    private val location = LocationInfo(
        id = "terban-yogyakarta",
        name = "Terban",
        country = "Indonesia",
        latitude = -7.7758,
        longitude = 110.3776,
        elevation = 100.0,
        timeZoneId = "Asia/Jakarta",
        isAutoDetected = false,
    )

    @Test
    fun rolloverUsesTomorrowFajrAndKeepsIshaActive() {
        val today = record(
            date = LocalDate(2026, 8, 17),
            fajr = "2026-08-16T21:00:00Z",
            sunrise = "2026-08-16T22:20:00Z",
            dhuhr = "2026-08-17T05:00:00Z",
            asr = "2026-08-17T08:20:00Z",
            maghrib = "2026-08-17T11:20:00Z",
            isha = "2026-08-17T12:45:00Z",
        )
        val tomorrow = record(
            date = LocalDate(2026, 8, 18),
            fajr = "2026-08-17T21:00:00Z",
            sunrise = "2026-08-17T22:20:00Z",
            dhuhr = "2026-08-18T05:00:00Z",
            asr = "2026-08-18T08:20:00Z",
            maghrib = "2026-08-18T11:20:00Z",
            isha = "2026-08-18T12:45:00Z",
        )

        val snapshot = PrayerWidgetSnapshotLogic.create(
            location = location,
            todayRecord = today,
            tomorrowRecord = tomorrow,
            hijriDate = null,
            now = Instant.parse("2026-08-17T13:00:00Z"),
        )

        assertEquals(Prayer.ISHA, snapshot.currentPrayer)
        assertEquals(Prayer.FAJR, snapshot.nextPrayer)
        assertEquals(Instant.parse("2026-08-17T21:00:00Z").toEpochMilliseconds(), snapshot.nextTargetEpochMillis)
        assertEquals(6, snapshot.prayerTimes.size)
    }

    @Test
    fun activePrayerIsMarkedForTheCurrentWindow() {
        val today = record(
            date = LocalDate(2026, 8, 17),
            fajr = "2026-08-16T21:00:00Z",
            sunrise = "2026-08-16T22:20:00Z",
            dhuhr = "2026-08-17T05:00:00Z",
            asr = "2026-08-17T08:20:00Z",
            maghrib = "2026-08-17T11:20:00Z",
            isha = "2026-08-17T12:45:00Z",
        )

        val snapshot = PrayerWidgetSnapshotLogic.create(
            location = location,
            todayRecord = today,
            tomorrowRecord = null,
            hijriDate = null,
            now = Instant.parse("2026-08-17T06:00:00Z"),
        )

        assertEquals(Prayer.DHUHR, snapshot.currentPrayer)
        assertEquals(Prayer.ASR, snapshot.nextPrayer)
        assertEquals(Prayer.DHUHR, snapshot.prayerTimes.single { it.isCurrent }.prayer)
        assertEquals(8, snapshot.timetableItems.size)
        assertEquals(PrayerWidgetTimetableEntry.IMSAK, snapshot.timetableItems[0].entry)
        assertEquals(PrayerWidgetTimetableEntry.DHUHA, snapshot.timetableItems[3].entry)
        assertEquals(PrayerWidgetTimetableEntry.MAGHRIB, snapshot.timetableItems[6].entry)
    }

    private fun record(
        date: LocalDate,
        fajr: String,
        sunrise: String,
        dhuhr: String,
        asr: String,
        maghrib: String,
        isha: String,
    ): PrayerTimeRecord = PrayerTimeRecord(
        date = date,
        coordinates = Coordinates(location.latitude, location.longitude),
        imsak = Instant.parse(fajr),
        fajr = Instant.parse(fajr),
        sunrise = Instant.parse(sunrise),
        dhuhr = Instant.parse(dhuhr),
        asr = Instant.parse(asr),
        maghrib = Instant.parse(maghrib),
        isha = Instant.parse(isha),
        midnight = Instant.parse("2026-08-17T16:00:00Z"),
    )
}
