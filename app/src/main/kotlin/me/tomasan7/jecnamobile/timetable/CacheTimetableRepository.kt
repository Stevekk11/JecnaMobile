package me.tomasan7.jecnamobile.timetable

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import io.github.tomhula.jecnaapi.data.timetable.TimetablePage
import io.github.tomhula.jecnaapi.data.substitution.SubstitutionStatus
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.util.CachedData
import java.io.File
import javax.inject.Inject

class CacheTimetableRepository @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val timetableRepository: TimetableRepository
)
{
    private val cacheFile = File(appContext.cacheDir, FILE_NAME)

    fun isCacheAvailable() = cacheFile.exists()

    @OptIn(ExperimentalSerializationApi::class)
    fun getCachedTimetable(): CachedData<TimetablePage>?
    {
        if (!isCacheAvailable())
            return null

        val inputStream = cacheFile.inputStream()

        return try
        {
            Json.decodeFromStream(inputStream)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            null
        }
        finally
        {
            inputStream.close()
        }
    }

    suspend fun getRealTimetable(withSubstitutions: Boolean = true): TimetablePage
    {
        val timetablePage = timetableRepository.getTimetablePage(withSubstitutions)
        cacheFile.writeText(Json.encodeToString(CachedData(timetablePage)))
        return timetablePage
    }

    /** Will not cache anything. */
    suspend fun getRealTimetable(schoolYear: SchoolYear, timetablePeriod: TimetablePage.PeriodOption, withSubstitutions: Boolean = true) =
        timetableRepository.getTimetablePage(schoolYear, timetablePeriod, withSubstitutions)

    suspend fun getTeacherAbsences() = timetableRepository.getTeacherAbsences()

    suspend fun getSubstitutionStatus(): SubstitutionStatus = timetableRepository.getSubstitutionStatus()

    companion object
    {
        private const val FILE_NAME = "timetable.json"
    }
}
