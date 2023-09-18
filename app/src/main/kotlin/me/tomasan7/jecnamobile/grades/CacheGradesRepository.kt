package me.tomasan7.jecnamobile.grades

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.tomasan7.jecnaapi.data.grade.GradesPage
import me.tomasan7.jecnaapi.util.SchoolYear
import me.tomasan7.jecnaapi.util.SchoolYearHalf
import me.tomasan7.jecnamobile.util.CachedData
import java.io.File
import javax.inject.Inject

class CacheGradesRepository @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val gradesRepository: GradesRepository
)
{
    private val cacheFile = File(appContext.cacheDir, FILE_NAME)

    fun isCacheAvailable() = cacheFile.exists()

    @OptIn(ExperimentalSerializationApi::class)
    fun getCachedGrades(): CachedData<GradesPage>?
    {
        if (!isCacheAvailable())
            return null

        return try
        {
            Json.decodeFromStream(cacheFile.inputStream())
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            null
        }
    }

    suspend fun getRealGrades(): GradesPage
    {
        val gradesPage = gradesRepository.getGradesPage()
        cacheFile.writeText(Json.encodeToString(CachedData(gradesPage)))
        return gradesPage
    }

    /** Will not cache anything. */
    suspend fun getRealGrades(schoolYear: SchoolYear, schoolYearHalf: SchoolYearHalf) = gradesRepository.getGradesPage(schoolYear, schoolYearHalf)

    companion object
    {
        private const val FILE_NAME = "grades.json"
    }
}