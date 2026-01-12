package me.tomasan7.jecnamobile.news

import io.github.tomhula.jecnaapi.JecnaClient
import me.tomasan7.jecnamobile.testdata.TestAccountManager
import me.tomasan7.jecnamobile.testdata.TestDataProvider
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val client: JecnaClient
) : NewsRepository
{
    override suspend fun getNewsPage() =
        client.getNewsPage()
}


