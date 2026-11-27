package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.FocomPublication
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArticleSyncTest {

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
    fun `sb_publishable key omits Authorization Bearer header and omits status filter`() = runTest {
        var capturedRequest: Request? = null
        val jsonPayload = """
            [
              {
                "id": "art-1",
                "title": "Accord NAO",
                "content": "<p>Texte <b>important</b></p>",
                "excerpt": null,
                "category": "victoire",
                "slug": null,
                "image_url": "/images/nao.png",
                "published_at": "2026-06-15T10:00:00Z",
                "created_at": null
              }
            ]
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(jsonPayload.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val supabaseService = Retrofit.Builder()
            .baseUrl("https://demo.supabase.co/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseService::class.java)

        val repository = GitHubRepository(
            database.savedRepoDao(),
            database.contactMessageDao(),
            database.articleCacheDao(),
            database.flashBannerCacheDao(),
            stubGitHubService(),
            supabaseService,
            "sb_publishable_demo_key"
        )

        val articles = repository.fetchPublishedArticles()

        assertEquals(1, articles.size)
        assertEquals("Accord NAO", articles[0].title)
        assertEquals("Victoire syndicale", articles[0].type)
        assertEquals("15/06/2026", articles[0].date)
        assertEquals("https://focomues-iliad.fr/images/nao.png", articles[0].thumbnailUrl)
        assertEquals("https://focomues-iliad.fr/actualites", articles[0].pdfUrl)
        assertEquals("Texte important", articles[0].content)

        val req = capturedRequest!!
        assertEquals("sb_publishable_demo_key", req.header("apikey"))
        assertNull(req.header("Authorization"))
        assertEquals("eq.true", req.url.queryParameter("is_published"))
        assertNull(req.url.queryParameter("status"))
    }

    @Test
    fun `legacy eyJ anon key includes Authorization Bearer header and caches articles for offline fallback`() = runTest {
        var shouldFail = false
        var seenAuthHeader: String? = null
        val jsonPayload = """
            [
              {
                "id": "art-2",
                "title": "Flash CSE",
                "content": "Compte rendu CSE",
                "excerpt": "Résumé",
                "category": "actualite",
                "slug": "flash-cse",
                "image_url": null,
                "published_at": "2026-07-01T08:00:00Z",
                "created_at": "2026-07-01T07:00:00Z"
              }
            ]
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                seenAuthHeader = chain.request().header("Authorization")
                if (shouldFail) {
                    throw IOException("Network unavailable")
                }
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(jsonPayload.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val supabaseService = Retrofit.Builder()
            .baseUrl("https://demo.supabase.co/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseService::class.java)

        val repository = GitHubRepository(
            database.savedRepoDao(),
            database.contactMessageDao(),
            database.articleCacheDao(),
            database.flashBannerCacheDao(),
            stubGitHubService(),
            supabaseService,
            "eyJhbGciOiJIUzI1NiJ9.payload.sig"
        )

        val liveArticles = repository.fetchPublishedArticles()
        assertEquals("Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig", seenAuthHeader)
        assertEquals(1, liveArticles.size)
        assertEquals("https://focomues-iliad.fr/actualites/flash-cse", liveArticles[0].pdfUrl)

        shouldFail = true
        val cachedArticles = repository.fetchPublishedArticles()
        assertEquals(liveArticles, cachedArticles)
    }

    @Test
    fun `viewModel preserves previously loaded articles when subsequent refresh fails`() = runTest(testDispatcher) {
        var failNext = false
        val fakeService = object : SupabaseService {
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
            ): Response<List<SupabaseArticle>> {
                if (failNext) {
                    return Response.error(
                        500,
                        """{"message":"Temporary failure"}""".toResponseBody("application/json".toMediaType())
                    )
                }
                return Response.success(
                    listOf(
                        SupabaseArticle(
                            id = "1",
                            title = "Article Sync OK",
                            content = "Contenu",
                            slug = "article-sync-ok",
                            publishedAt = "2026-08-01T10:00:00Z",
                            createdAt = "2026-08-01T10:00:00Z"
                        )
                    )
                )
            }

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
            database.savedRepoDao(),
            database.contactMessageDao(),
            database.articleCacheDao(),
            database.flashBannerCacheDao(),
            stubGitHubService(),
            fakeService,
            "sb_publishable_test",
            testDispatcher
        )

        val viewModel = GitResumeViewModel(repository)
        advanceUntilIdle()

        val initialState = viewModel.publishedArticlesState.value
        assertTrue(initialState is UiState.Success<List<FocomPublication>>)
        assertEquals(1, (initialState as UiState.Success).data.size)

        // Clear cache and force remote error to verify ViewModel retains existing Success state
        database.articleCacheDao().clear()
        failNext = true
        viewModel.refreshPublishedArticles()
        advanceUntilIdle()

        val afterErrorState = viewModel.publishedArticlesState.value
        assertTrue(afterErrorState is UiState.Success<List<FocomPublication>>)
        assertEquals("Article Sync OK", (afterErrorState as UiState.Success).data.first().title)
        assertTrue(viewModel.articlesSyncError.value?.contains("500") == true)
    }

    private fun stubGitHubService(): GitHubService {
        return Proxy.newProxyInstance(
            GitHubService::class.java.classLoader,
            arrayOf(GitHubService::class.java)
        ) { _, _, _ ->
            throw IOException("Network stubbed in test")
        } as GitHubService
    }
}
