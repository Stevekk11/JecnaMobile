package me.tomasan7.jecnamobile.attendances

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import java.time.Month
import me.tomasan7.jecnamobile.testdata.TestAccountManager
import me.tomasan7.jecnamobile.testdata.TestDataProvider
import javax.inject.Inject

class AttendancesRepositoryImpl @Inject constructor(
    val jecnaClient: JecnaClient
) : AttendancesRepository
{
    override suspend fun getAttendancesPage() = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateAttendancesPage()
        else jecnaClient.getAttendancesPage()

    override suspend fun getAttendancesPage(schoolYear: SchoolYear, month: Month) = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateAttendancesPage(schoolYear, month)
        else jecnaClient.getAttendancesPage(schoolYear, month)
}
