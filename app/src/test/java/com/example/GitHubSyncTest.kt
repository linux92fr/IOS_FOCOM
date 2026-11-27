package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.ContactMessage
import com.example.data.model.FocomContent
import com.example.data.model.GitHubRepo
import com.example.data.remote.GitHubService
import com.example.data.remote.SupabaseArticle
import com.example.data.remote.SupabaseContactMessage
import com.example.data.remote.SupabaseFlashBanner
import com.example.data.remote.SupabaseService
import com.example.data.repository.GitHubRepository
import com.example.ui.viewmodel.GitResumeViewModel
import com.example.ui.viewmodel.UiState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GitHubSyncTest {

    private lateinit var database: AppDatabase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchFocomContentFromRepo tries fallback branches and caches partial JSON offline`() = runTest(testDispatcher) {
        val requestedUrls = mutableListOf<String>()
        var failNetwork = false

        val partialPayload = """
            {
              "newsList": [
                {
                  "id": "news-1",
                  "title": "Accord Teletravail",
                  "category": "Accord",
                  "date": "2026-09-01",
                  "description": "Nouveau texte signe"
                }
              ],
              "rightsList": [
                {
                  "id": "right-1",
                  "title": "Conges annuels"
                }
              ],
              "delegatesList": [
                {
                  "id": "del-1",
                  "name": "Nelly Sidibe",
                  "role": "DSC"
                }
              ],
              "publicationsList": [
                {
                  "id": "pub-1",
                  "title": "Tract Rentree",
                  "date": "01/09/2026",
                  "pdfUrl": "https://focomues-iliad.fr/tract.pdf"
                }
              ]
            }
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                requestedUrls.add(url)
                if (failNetwork) {
                    throw IOException("Offline")
                }
                if (url == "https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/master/focom-content.json") {
                    okhttp3.Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(partialPayload.toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    okhttp3.Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(404)
                        .message("Not Found")
                        .body("Not Found".toResponseBody("text/plain".toMediaType()))
                        .build()
                }
            }
            .build()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val gitHubService = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubService::class.java)

        val repository = GitHubRepository(
            savedRepoDao = database.savedRepoDao(),
            contactMessageDao = database.contactMessageDao(),
            articleCacheDao = database.articleCacheDao(),
            flashBannerCacheDao = database.flashBannerCacheDao(),
            gitHubService = gitHubService,
            supabaseService = null,
            supabasePublishableKey = "",
            ioDispatcher = testDispatcher,
            focomContentCacheDao = database.focomContentCacheDao()
        )

        val live: FocomContent = repository.fetchFocomContentFromRepo("linux92fr", "focom-ues-iliad2")
        assertEquals(
            listOf(
                "https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/main/data/focom-content.json",
                "https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/main/focom-content.json",
                "https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/master/data/focom-content.json",
                "https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/master/focom-content.json"
            ),
            requestedUrls
        )
        assertEquals(1, live.newsList.size)
        assertEquals("", live.newsList.first().content)
        assertEquals("Législation", live.rightsList.first().category)
        assertEquals("Nelly Sidibe", live.delegatesList.first().name)
        assertEquals("Tract Rentree", live.publicationsList.first().title)
        assertNotNull(repository.getCachedFocomContentTimestamp())

        failNetwork = true
        val offline: FocomContent = repository.fetchFocomContentFromRepo("linux92fr", "focom-ues-iliad2")
        assertEquals(live, offline)
    }

    @Test
    fun `syncAllGitHubElements loads repo metadata files readme commits issues and focom json`() = runTest(testDispatcher) {
        var issuesStateQuery: String? = null
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url
                val fullUrl = url.toString()
                val bodyJson = when {
                    fullUrl == "https://api.github.com/repos/linux92fr/focom-ues-iliad2" -> """
                        {
                          "name": "focom-ues-iliad2",
                          "full_name": "linux92fr/focom-ues-iliad2",
                          "owner": { "login": "linux92fr", "avatar_url": "https://avatars.example/u.png" },
                          "description": "Portail FOCOM",
                          "stargazers_count": 12,
                          "forks_count": 3,
                          "open_issues_count": 1,
                          "language": "TypeScript",
                          "default_branch": "main"
                        }
                    """.trimIndent()
                    fullUrl == "https://api.github.com/repos/linux92fr/Android_FOCOM" -> """
                        {
                          "name": "Android_FOCOM",
                          "full_name": "linux92fr/Android_FOCOM",
                          "owner": { "login": "linux92fr", "avatar_url": null },
                          "description": "App Android",
                          "stargazers_count": 5,
                          "forks_count": 1,
                          "open_issues_count": 0,
                          "language": "Kotlin",
                          "default_branch": "main"
                        }
                    """.trimIndent()
                    url.encodedPath.endsWith("/contents") -> """
                        [
                          {
                            "name": "README.md",
                            "path": "README.md",
                            "type": "file",
                            "size": 128,
                            "download_url": "https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/main/README.md"
                          },
                          {
                            "name": "data",
                            "path": "data",
                            "type": "dir",
                            "size": 0,
                            "download_url": null
                          }
                        ]
                    """.trimIndent()
                    url.encodedPath.endsWith("/commits") -> """
                        [
                          {
                            "sha": "abcdef1234567",
                            "commit": {
                              "message": "Mise a jour focom-content.json",
                              "author": { "name": "linux92fr", "email": "dev@example.com", "date": "2026-09-02T10:00:00Z" }
                            },
                            "author": { "login": "linux92fr", "avatar_url": null }
                          }
                        ]
                    """.trimIndent()
                    url.encodedPath.endsWith("/issues") -> {
                        issuesStateQuery = url.queryParameter("state")
                        """
                        [
                          {
                            "number": 42,
                            "title": "Synchroniser les accords",
                            "body": "Verifier le JSON",
                            "state": "closed",
                            "created_at": "2026-08-20T09:00:00Z",
                            "user": { "login": "linux92fr", "avatar_url": null }
                          }
                        ]
                        """.trimIndent()
                    }
                    fullUrl.endsWith("/README.md") -> "# FOCOM UES ILIAD"
                    fullUrl == "https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/main/data/focom-content.json" -> """
                        {
                          "newsList": [
                            { "id": "n1", "title": "Info GitHub", "category": "CSE", "date": "2026-09-02", "description": "Desc", "content": "Contenu" }
                          ],
                          "rightsList": [],
                          "delegatesList": [],
                          "publicationsList": [
                            { "id": "p1", "title": "Publication GitHub", "type": "Tract", "date": "02/09/2026", "pdfUrl": "https://focomues-iliad.fr/p1.pdf" }
                          ]
                        }
                    """.trimIndent()
                    else -> null
                }

                if (bodyJson != null) {
                    val mediaType = if (fullUrl.endsWith("/README.md")) "text/plain" else "application/json"
                    okhttp3.Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(bodyJson.toResponseBody(mediaType.toMediaType()))
                        .build()
                } else {
                    okhttp3.Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(404)
                        .message("Not Found")
                        .body("Not Found".toResponseBody("text/plain".toMediaType()))
                        .build()
                }
            }
            .build()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val gitHubService = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubService::class.java)

        val stubSupabase = object : SupabaseService {
            override suspend fun insertContactMessage(
                apiKey: String,
                authorization: String?,
                prefer: String,
                message: SupabaseContactMessage
            ): Response<Unit> = Response.success(Unit)

            override suspend fun getPublishedArticles(
                apiKey: String,
                authorization: String?,
                select: String,
                isPublished: String,
                order: String,
                limit: Int
            ): Response<List<SupabaseArticle>> = Response.success(emptyList())

            override suspend fun getActiveFlashBanners(
                apiKey: String,
                authorization: String?,
                select: String,
                isActive: String,
                startsBeforeOrAt: String,
                endsAfterOrAt: String,
                order: String
            ): List<SupabaseFlashBanner> = emptyList()
        }

        val repository = GitHubRepository(
            savedRepoDao = database.savedRepoDao(),
            contactMessageDao = database.contactMessageDao(),
            articleCacheDao = database.articleCacheDao(),
            flashBannerCacheDao = database.flashBannerCacheDao(),
            gitHubService = gitHubService,
            supabaseService = stubSupabase,
            supabasePublishableKey = "sb_publishable_test",
            ioDispatcher = testDispatcher,
            focomContentCacheDao = database.focomContentCacheDao()
        )

        val viewModel = GitResumeViewModel(repository)
        advanceUntilIdle()

        val repoState = viewModel.activeRepoState.value
        assertTrue(repoState is UiState.Success<GitHubRepo>)
        assertEquals("linux92fr/focom-ues-iliad2", (repoState as UiState.Success).data.fullName)

        val contentsState = viewModel.contentsState.value
        assertTrue(contentsState is UiState.Success)
        assertEquals(2, (contentsState as UiState.Success).data.size)

        val readmeState = viewModel.readmeState.value
        assertTrue(readmeState is UiState.Success)
        assertEquals("# FOCOM UES ILIAD", (readmeState as UiState.Success).data)

        val commitsState = viewModel.commitsState.value
        assertTrue(commitsState is UiState.Success)
        assertEquals("abcdef1234567", (commitsState as UiState.Success).data.first().sha)

        val issuesState = viewModel.issuesState.value
        assertTrue(issuesState is UiState.Success)
        assertEquals("all", issuesStateQuery)
        assertEquals(42, (issuesState as UiState.Success).data.first().number)

        val focomState = viewModel.focomContentState.value
        assertTrue(focomState is UiState.Success)
        assertEquals("Info GitHub", (focomState as UiState.Success).data.newsList.first().title)
        assertEquals("Publication GitHub", focomState.data.publicationsList.first().title)
        assertNotNull(viewModel.lastGitHubSyncAt.value)
        assertNotNull(database.savedRepoDao().getRepo("linux92fr/focom-ues-iliad2"))

        // Switch explorer repo to Android_FOCOM while keeping editorial focom-content synced
        viewModel.syncAllGitHubElements("linux92fr", "Android_FOCOM")
        advanceUntilIdle()

        val switchedRepo = viewModel.activeRepoState.value
        assertTrue(switchedRepo is UiState.Success<GitHubRepo>)
        assertEquals("linux92fr/Android_FOCOM", (switchedRepo as UiState.Success).data.fullName)
        assertTrue(viewModel.focomContentState.value is UiState.Success)
    }

    @Test
    fun `sendContactMessage marks failed remote post as pending and retries when network recovers`() = runTest(testDispatcher) {
        var shouldFailPost = true
        val fakeSupabase = object : SupabaseService {
            override suspend fun insertContactMessage(
                apiKey: String,
                authorization: String?,
                prefer: String,
                message: SupabaseContactMessage
            ): Response<Unit> {
                return if (shouldFailPost) {
                    Response.error(503, "Unavailable".toResponseBody("text/plain".toMediaType()))
                } else {
                    Response.success(Unit)
                }
            }

            override suspend fun getPublishedArticles(
                apiKey: String,
                authorization: String?,
                select: String,
                isPublished: String,
                order: String,
                limit: Int
            ): Response<List<SupabaseArticle>> = Response.success(emptyList())

            override suspend fun getActiveFlashBanners(
                apiKey: String,
                authorization: String?,
                select: String,
                isActive: String,
                startsBeforeOrAt: String,
                endsAfterOrAt: String,
                order: String
            ): List<SupabaseFlashBanner> = emptyList()
        }

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val dummyGitHubService = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubService::class.java)

        val repository = GitHubRepository(
            savedRepoDao = database.savedRepoDao(),
            contactMessageDao = database.contactMessageDao(),
            articleCacheDao = database.articleCacheDao(),
            flashBannerCacheDao = database.flashBannerCacheDao(),
            gitHubService = dummyGitHubService,
            supabaseService = fakeSupabase,
            supabasePublishableKey = "sb_publishable_test",
            ioDispatcher = testDispatcher,
            focomContentCacheDao = database.focomContentCacheDao()
        )

        val firstResult = repository.sendContactMessage(
            ContactMessage(
                name = "Fadil",
                email = "fadil@focomues-iliad.fr",
                phone = "0600000000",
                subject = "Question CSE",
                message = "Bonjour, besoin d'une info.",
                createdAt = 1000L
            )
        )
        assertEquals(GitHubRepository.ContactSendResult.FAILED_SAVED_LOCAL, firstResult)

        val pendingList = database.contactMessageDao().getAllMessages().first()
        assertEquals(1, pendingList.size)
        assertEquals("En attente réseau", pendingList.first().status)

        shouldFailPost = false
        val retriedCount = repository.retryPendingContactMessages()
        assertEquals(1, retriedCount)

        val deliveredList = database.contactMessageDao().getAllMessages().first()
        assertEquals("Envoyé", deliveredList.first().status)
    }
}
