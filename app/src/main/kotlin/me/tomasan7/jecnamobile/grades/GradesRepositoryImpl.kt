package me.tomasan7.jecnamobile.grades

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.SchoolYearHalf
import me.tomasan7.jecnamobile.testdata.TestAccountManager
import me.tomasan7.jecnamobile.testdata.TestDataProvider
import javax.inject.Inject

class GradesRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : GradesRepository
{
    override suspend fun getGradesPage() = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateGradesPage()
        else jecnaClient.getGradesPage()

    override suspend fun getGradesPage(schoolYear: SchoolYear, schoolYearHalf: SchoolYearHalf) = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateGradesPage()
        else jecnaClient.getGradesPage(schoolYear, schoolYearHalf)
}


