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
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
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
        // Calculate immediately via KMP engine (sub-millisecond pure math)
        val computed = calculatePrayerTime(date, location, settings)
        emit(computed)
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
        val records = (1..daysInMonth).map { day ->
            val date = LocalDate(year, month, day)
            calculatePrayerTime(date, location, settings)
        }
        emit(records)
    }

    override suspend fun computeAndCachePrayerTimes(
        startDate: LocalDate,
        daysCount: Int,
        location: LocationInfo,
        settings: UserSettings
    ): List<PrayerTimeRecord> {
        val records = mutableListOf<PrayerTimeRecord>()
        val entities = mutableListOf<PrayerScheduleEntity>()

        for (i in 0 until daysCount) {
            val date = startDate.plus(DatePeriod(days = i))
            val record = calculatePrayerTime(date, location, settings)
            records.add(record)
            entities.add(PrayerScheduleEntity.fromDomain(record, location.id))
        }

        prayerScheduleDao.insertSchedules(entities)
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
            firstThirdOfTheNight = null,
            lastThirdOfTheNight = prayerTimes.timeForPrayer(com.adzannotif.core.prayer.Prayer.TAHAJJUD),
        )
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
