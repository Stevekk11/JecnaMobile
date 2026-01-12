package me.tomasan7.jecnamobile.teachers

import io.github.tomhula.jecnaapi.JecnaClient
import javax.inject.Inject
import me.tomasan7.jecnamobile.testdata.TestAccountManager
import me.tomasan7.jecnamobile.testdata.TestDataProvider

class TeachersRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : TeachersRepository
{
    override suspend fun getTeachersPage() = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateTeachersPage()
        else jecnaClient.getTeachersPage()

    override suspend fun getTeacher(tag: String) = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateTeacher(tag)
        else jecnaClient.getTeacher(tag)
}
