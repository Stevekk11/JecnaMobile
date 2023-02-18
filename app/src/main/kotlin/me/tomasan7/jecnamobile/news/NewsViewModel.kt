package me.tomasan7.jecnamobile.news

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.ktor.http.*
import io.ktor.util.network.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnaapi.JecnaClient
import me.tomasan7.jecnaapi.data.article.ArticleFile
import me.tomasan7.jecnaapi.data.article.NewsPage
import me.tomasan7.jecnaapi.web.jecna.JecnaWebClient
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import okhttp3.Headers
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@HiltViewModel
class NewsViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val repository: CacheNewsRepository,
    private val jecnaClient: JecnaClient
) : ViewModel()
{
    var uiState by mutableStateOf(NewsState())
        private set

    private var loadNewsJob: Job? = null

    private val loginBroadcastReceiver = createBroadcastReceiver { _, _ ->
        if (loadNewsJob == null || loadNewsJob!!.isCompleted)
        {
            changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.back_online)))
            loadReal()
        }
    }

    private val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val downloadFinishedBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        openDownloadedFile(downloadId)
    }

    fun enteredComposition()
    {
        loadCache()
        loadReal()
        appContext.registerReceiver(
            loginBroadcastReceiver, IntentFilter(JecnaMobileApplication.SUCCESSFUL_LOGIN_ACTION)
        )
        appContext.registerReceiver(
            downloadFinishedBroadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    fun leftComposition()
    {
        loadNewsJob?.cancel()
        appContext.unregisterReceiver(loginBroadcastReceiver)
    }

    fun downloadAndOpenArticleFile(articleFile: ArticleFile) = viewModelScope.launch {
        val url = JecnaWebClient.ENDPOINT + articleFile.downloadPath
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(articleFile.label)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, articleFile.filename)
            setMimeType(getMimeTypeByExtension(articleFile.fileExtension))

            val sessionCookie = getSessionCookie() ?: return@launch
            addRequestHeader("Cookie", sessionCookie.toHeaderString())
        }

        downloadManager.enqueue(request)
    }

    private fun getSessionCookieBlocking() = runBlocking { jecnaClient.getSessionCookie() }

    private suspend fun getSessionCookie() = jecnaClient.getSessionCookie()

    private fun Cookie.toHeaderString() = "$name=$value"

    @SuppressLint("Range")
    private fun openDownloadedFile(downloadId: Long)
    {
        val query = DownloadManager.Query().setFilterById(downloadId)

        val uri: Uri
        val mimeType: String

        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst())
                return

            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

            if (status != DownloadManager.STATUS_SUCCESSFUL)
                return

            uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)).toUri()
            mimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
        }

        val providedUri =
            FileProvider.getUriForFile(appContext, appContext.packageName + ".fileprovider", File(uri.path!!))

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(providedUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            /* So you can start the activity from app context. (normally it has to be done from activity context) */
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try
        {
            appContext.startActivity(openIntent)
        }
        catch (e: ActivityNotFoundException)
        {
            changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.error_unable_to_open_file)))
        }
    }

    private fun getMimeTypeByExtension(extension: String) =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

    fun createImageRequest(path: String) = ImageRequest.Builder(appContext).apply {
        data(JecnaWebClient.ENDPOINT + path)
        crossfade(true)
        val sessionCookie = getSessionCookieBlocking() ?: return@apply
        headers(Headers.headersOf("Cookie", sessionCookie.toHeaderString()))
    }.build()

    private fun loadCache()
    {
        if (!repository.isCacheAvailable())
            return

        viewModelScope.launch {
            val cachedGrades = repository.getCachedNews() ?: return@launch

            changeUiState(
                newsPage = cachedGrades.data,
                lastUpdateTimestamp = cachedGrades.timestamp
            )
        }
    }

    private fun loadReal()
    {
        loadNewsJob?.cancel()

        changeUiState(loading = true)

        loadNewsJob = viewModelScope.launch {
            try
            {
                val realNews = repository.getRealNews()

                changeUiState(
                    newsPage = realNews,
                    lastUpdateTimestamp = Instant.now()
                )
            }
            catch (e: UnresolvedAddressException)
            {
                changeUiState(snackBarMessageEvent = triggered(getOfflineMessage()))
            }
            finally
            {
                changeUiState(loading = false)
            }
        }
    }

    private fun getOfflineMessage(): String
    {
        val cacheTimestamp = uiState.lastUpdateTimestamp
        val localDateTime = LocalDateTime.ofInstant(cacheTimestamp, ZoneId.systemDefault())
        val localDate = localDateTime.toLocalDate()

        return if (localDate == LocalDate.now())
        {
            val time = localDateTime.toLocalTime()
            val timeStr = time.format(OFFLINE_MESSAGE_TIME_FORMATTER)
            appContext.getString(R.string.grade_showing_offline_data_time, timeStr)
        }
        else
        {
            val dateStr = localDateTime.format(OFFLINE_MESSAGE_DATE_FORMATTER)
            appContext.getString(R.string.grade_showing_offline_data_date, dateStr)
        }
    }

    fun reload() = loadReal()

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        newsPage: NewsPage? = uiState.newsPage,
        lastUpdateTimestamp: Instant? = uiState.lastUpdateTimestamp,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            newsPage = newsPage,
            lastUpdateTimestamp = lastUpdateTimestamp,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }

    companion object
    {
        val OFFLINE_MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        val OFFLINE_MESSAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("d. M.")
    }
}