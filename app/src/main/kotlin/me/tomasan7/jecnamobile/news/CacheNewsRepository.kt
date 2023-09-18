package me.tomasan7.jecnamobile.news

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.tomasan7.jecnaapi.data.article.NewsPage
import me.tomasan7.jecnamobile.util.CachedData
import java.io.File
import javax.inject.Inject

class CacheNewsRepository @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val newsRepository: NewsRepository
)
{
    private val cacheFile = File(appContext.cacheDir, FILE_NAME)

    fun isCacheAvailable() = cacheFile.exists()

    @OptIn(ExperimentalSerializationApi::class)
    fun getCachedNews(): CachedData<NewsPage>?
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

    suspend fun getRealNews(): NewsPage
    {
        val newsPage = newsRepository.getNewsPage()
        cacheFile.writeText(Json.encodeToString(CachedData(newsPage)))
        return newsPage
    }

    companion object
    {
        private const val FILE_NAME = "news.json"
    }
}