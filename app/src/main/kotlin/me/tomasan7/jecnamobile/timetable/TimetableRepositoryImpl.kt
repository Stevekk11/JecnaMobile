package me.tomasan7.jecnamobile.timetable

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.data.substitution.LabeledTeacherAbsences
import io.github.tomhula.jecnaapi.data.substitution.SubstitutionStatus
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.testdata.TestAccountManager
import me.tomasan7.jecnamobile.testdata.TestDataProvider
import javax.inject.Inject

class TimetableRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : TimetableRepository
{
    override suspend fun getTimetablePage(withSubstitutions: Boolean) = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateTimetablePage()
        else jecnaClient.getTimetablePage(withSubstitutions)

    override suspend fun getTimetablePage(
        schoolYear: SchoolYear,
        timetablePeriod: TimetablePage.PeriodOption,
        withSubstitutions: Boolean
    ) = if (TestAccountManager.isTestAccountActive) TestDataProvider.generateTimetablePage()
        else jecnaClient.getTimetablePage(schoolYear, timetablePeriod, withSubstitutions)

    override suspend fun getTeacherAbsences(): List<LabeledTeacherAbsences> = 
        if (TestAccountManager.isTestAccountActive) emptyList()
        else jecnaClient.getTeacherAbsences()

    override suspend fun getSubstitutionStatus() = 
        if (TestAccountManager.isTestAccountActive) SubstitutionStatus("unknown", 0)
        else jecnaClient.getSubstitutions().status
}
