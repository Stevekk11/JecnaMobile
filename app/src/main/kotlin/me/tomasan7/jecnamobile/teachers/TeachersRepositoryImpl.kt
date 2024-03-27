package me.tomasan7.jecnamobile.teachers

import me.tomasan7.jecnaapi.JecnaClient
import javax.inject.Inject

class TeachersRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : TeachersRepository
{
    override suspend fun getTeachersPage() = jecnaClient.getTeachersPage()

    override suspend fun getTeacher(tag: String) = jecnaClient.getTeacher(tag)
}
