package me.tomasan7.jecnamobile.student

import io.github.tomhula.jecnaapi.JecnaClient
import javax.inject.Inject
import me.tomasan7.jecnamobile.testdata.TestAccountManager
import me.tomasan7.jecnamobile.testdata.TestDataProvider

class StudentProfileRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : StudentProfileRepository
{
    override suspend fun getCurrentStudent() = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateStudent()
        else jecnaClient.getStudentProfile()
    override suspend fun getLocker() = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateLocker()
        else jecnaClient.getLocker()
}
