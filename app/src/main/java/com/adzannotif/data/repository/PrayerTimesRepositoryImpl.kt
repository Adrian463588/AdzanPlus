package com.adzannotif.data.repository

import com.adzannotif.core.prayer.CalculationParameters
import com.adzannotif.core.prayer.DateComponents
import com.adzannotif.core.prayer.PrayerTimes
import com.adzannotif.data.local.dao.PrayerScheduleDao
import com.adzannotif.data.local.entity.PrayerScheduleEntity
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.model.UserSettings
import com.adzannotif.domain.repository.PrayerTimesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepositoryImpl @Inject constructor(
    private val prayerScheduleDao: PrayerScheduleDao,
) : PrayerTimesRepository {

    override fun getPrayerTimesForDate(
        date: LocalDate,
        location: LocationInfo,
        settings: UserSettings
    ): Flow<PrayerTimeRecord> = flow {
        val cacheLocationId = cacheLocationId(location, settings)
        val cached = prayerScheduleDao
            .getScheduleForDate(date.toString(), cacheLocationId)
            .first()
            ?.toDomain()
        if (cached != null && isWithinCacheWindow(date, location)) {
            emit(cached)
        } else {
            val computed = calculatePrayerTime(date, location, settings)
            if (isWithinCacheWindow(date, location)) {
                prayerScheduleDao.insertSchedules(
                    listOf(PrayerScheduleEntity.fromDomain(computed, cacheLocationId))
                )
            }
            emit(computed)
        }
    }

    override fun getMonthlyPrayerTimes(
        year: Int,
        month: Int,
        location: LocationInfo,
        settings: UserSettings
    ): Flow<List<PrayerTimeRecord>> = flow {
        val daysInMonth = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        val cacheLocationId = cacheLocationId(location, settings)
        val cached = prayerScheduleDao
            .getMonthlySchedules("%04d-%02d".format(java.util.Locale.ROOT, year, month), cacheLocationId)
            .first()
            .filter { isWithinCacheWindow(LocalDate.parse(it.dateString), location) }
        if (cached.size == daysInMonth) {
            emit(cached.map(PrayerScheduleEntity::toDomain))
            return@flow
        }
        val records = (1..daysInMonth).map { day ->
            calculatePrayerTime(LocalDate(year, month, day), location, settings)
        }
        val cacheableRecords = records.filter { isWithinCacheWindow(it.date, location) }
        if (cacheableRecords.isNotEmpty()) {
            prayerScheduleDao.insertSchedules(
                cacheableRecords.map { PrayerScheduleEntity.fromDomain(it, cacheLocationId) }
            )
        }
        emit(records)
    }

    override suspend fun computeAndCachePrayerTimes(
        startDate: LocalDate,
        daysCount: Int,
        location: LocationInfo,
        settings: UserSettings
    ): List<PrayerTimeRecord> {
        if (daysCount <= 0) return emptyList()

        val records = mutableListOf<PrayerTimeRecord>()
        val entities = mutableListOf<PrayerScheduleEntity>()
        val cacheLocationId = cacheLocationId(location, settings)

        for (i in 0 until daysCount) {
            val date = startDate.plus(DatePeriod(days = i))
            val record = calculatePrayerTime(date, location, settings)
            records.add(record)
            entities.add(PrayerScheduleEntity.fromDomain(record, cacheLocationId))
        }

        prayerScheduleDao.insertSchedules(entities)
        val lastDate = startDate.plus(DatePeriod(days = daysCount - 1))
        prayerScheduleDao.deleteOutsideWindow(
            locationId = cacheLocationId,
            firstDate = startDate.toString(),
            lastDate = lastDate.toString(),
        )
        return records
    }

    private fun calculatePrayerTime(
        date: LocalDate,
        location: LocationInfo,
        settings: UserSettings
    ): PrayerTimeRecord {
        val dateComponents = DateComponents(date.year, date.monthNumber, date.dayOfMonth)
        val coords = location.toCoordinates()
        val params = CalculationParameters(
            method = settings.calculationMethod,
            fajrAngle = settings.calculationMethod.fajrAngle,
            ishaAngle = settings.calculationMethod.ishaAngle,
            ishaInterval = settings.calculationMethod.ishaInterval,
            maghribAngle = settings.calculationMethod.maghribAngle,
            madhab = settings.madhab,
            highLatitudeRule = settings.highLatitudeRule,
            prayerAdjustments = settings.toPrayerAdjustments()
        )

        val prayerTimes = PrayerTimes(
            coordinates = coords,
            dateComponents = dateComponents,
            calculationParameters = params
        )

        return PrayerTimeRecord(
            date = date,
            coordinates = coords,
            imsak = prayerTimes.imsak,
            fajr = prayerTimes.fajr,
            sunrise = prayerTimes.sunrise,
            dhuhr = prayerTimes.dhuhr,
            asr = prayerTimes.asr,
            maghrib = prayerTimes.maghrib,
            isha = prayerTimes.isha,
            midnight = prayerTimes.timeForPrayer(com.adzannotif.core.prayer.Prayer.MIDNIGHT),
            firstThirdOfTheNight = prayerTimes.sunnahTimes.firstThirdOfTheNight,
            lastThirdOfTheNight = prayerTimes.timeForPrayer(com.adzannotif.core.prayer.Prayer.TAHAJJUD),
        )
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun isWithinCacheWindow(date: LocalDate, location: LocationInfo): Boolean {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.of(location.timeZoneId))
            .date
        val lastDate = today.plus(DatePeriod(days = CACHE_DAYS - 1))
        return date in today..lastDate
    }

    private fun cacheLocationId(location: LocationInfo, settings: UserSettings): String = buildString {
        append(location.id)
        append('|').append(location.latitude)
        append('|').append(location.longitude)
        append('|').append(location.timeZoneId)
        append('|').append(settings.calculationMethod.name)
        append('|').append(settings.madhab.name)
        append('|').append(settings.highLatitudeRule.name)
        append('|').append(settings.ihtiyatMinutes)
        append('|').append(settings.fajrAdjustment)
        append('|').append(settings.dhuhrAdjustment)
        append('|').append(settings.asrAdjustment)
        append('|').append(settings.maghribAdjustment)
        append('|').append(settings.ishaAdjustment)
    }

    private companion object {
        const val CACHE_DAYS = 30
    }
}
