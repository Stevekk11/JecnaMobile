package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.data.substitution.LabeledTeacherAbsences
import io.github.tomhula.jecnaapi.util.SchoolYear
import javax.inject.Inject

class TimetableRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : TimetableRepository
{
    override suspend fun getTimetablePage(withSubstitutions: Boolean) = jecnaClient.getTimetablePage(withSubstitutions)

    override suspend fun getTimetablePage(
        schoolYear: SchoolYear,
        timetablePeriod: TimetablePage.PeriodOption,
        withSubstitutions: Boolean
    ) = jecnaClient.getTimetablePage(schoolYear, timetablePeriod, withSubstitutions)

    override suspend fun getTeacherAbsences(): List<LabeledTeacherAbsences> = jecnaClient.getTeacherAbsences()

    override suspend fun getSubstitutionStatus() = jecnaClient.getSubstitutions().status
}
