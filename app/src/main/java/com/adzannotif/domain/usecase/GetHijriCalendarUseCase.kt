package com.adzannotif.domain.usecase

import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.repository.AstronomyRepository
import javax.inject.Inject

class GetHijriCalendarUseCase @Inject constructor(
    private val repository: AstronomyRepository
) {
    suspend operator fun invoke(location: LocationInfo, year: Int, month: Int): List<CalendarDay> {
        return repository.getMonthCalendar(location, year, month)
    }
}
