package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.util.SchoolYear

interface TimetableRepository
{
    suspend fun getTimetablePage(withSubstitutions: Boolean = true): TimetablePage
    suspend fun getTimetablePage(schoolYear: SchoolYear, timetablePeriod: TimetablePage.PeriodOption, withSubstitutions: Boolean = true): TimetablePage
}
