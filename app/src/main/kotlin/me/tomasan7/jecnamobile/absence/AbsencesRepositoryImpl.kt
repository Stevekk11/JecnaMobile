package me.tomasan7.jecnamobile.absence

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.testdata.TestAccountManager
import me.tomasan7.jecnamobile.testdata.TestDataProvider
import javax.inject.Inject

class AbsencesRepositoryImpl @Inject constructor(
    val jecnaClient: JecnaClient
) : AbsencesRepository
{
    override suspend fun getAbsencesPage() = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateAbsencesPage()
        else jecnaClient.getAbsencesPage()

    override suspend fun getAbsencesPage(schoolYear: SchoolYear) = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateAbsencesPage(schoolYear)
        else jecnaClient.getAbsencesPage(schoolYear)
}
