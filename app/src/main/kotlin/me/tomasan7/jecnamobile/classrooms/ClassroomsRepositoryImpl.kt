package me.tomasan7.jecnamobile.classrooms

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.classroom.ClassroomReference
import me.tomasan7.jecnamobile.testdata.TestAccountManager
import me.tomasan7.jecnamobile.testdata.TestDataProvider
import javax.inject.Inject

class ClassroomsRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : ClassroomsRepository
{
    override suspend fun getClassroomsPage() = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateClassroomsPage()
        else jecnaClient.getClassroomsPage()

    override suspend fun getClassroom(ref: ClassroomReference) = 
        if (TestAccountManager.isTestAccountActive) TestDataProvider.generateClassroom(ref)
        else jecnaClient.getClassroom(ref)
}
