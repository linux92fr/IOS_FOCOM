package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CachedArticle
import com.example.data.local.CachedFocomContent
import com.example.data.model.FocomPublication
import com.example.data.remote.GitHubService
import com.example.data.remote.SupabaseArticle
import com.example.data.remote.SupabaseContactMessage
import com.example.data.remote.SupabaseFlashBanner
import com.example.data.remote.SupabaseService
import com.example.data.repository.GitHubRepository
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.FrenchStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GitResumeViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineNewsCacheTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
    fun `room article cache DAO emits Flow updates and supports lookup by id`() = runTest(testDispatcher) {
        val dao = database.articleCacheDao()
        assertTrue(dao.observeAll().first().isEmpty())

        val cached = CachedArticle(
            id = "offline-1",
            title = "Accord CSE Télétravail",
            type = "Accord",
            date = "19/11/2026",
            thumbnailUrl = null,
            pdfUrl = "https://focomues-iliad.fr/actualites/accord-cse",
            excerpt = "Synthèse de l'accord",
            content = "Contenu détaillé consultable hors ligne.",
            position = 0,
            cachedAt = 1_700_000_000_000L
        )
        dao.replaceCache(listOf(cached))

        val list = dao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals("Accord CSE Télétravail", list.first().title)
        assertEquals(1_700_000_000_000L, dao.getLastCachedAt())
        assertNotNull(dao.getById("offline-1"))
    }

    @Test
    fun `offline startup serves cached news articles from Room and switches to live when online`() = runTest(testDispatcher) {
        // Seed Room caches from a prior online session
        database.articleCacheDao().replaceCache(
            listOf(
                CachedArticle(
                    id = "cached-nao-1",
                    title = "Article en cache NAO 2026",
                    type = "Victoire syndicale",
                    date = "18/11/2026",
                    thumbnailUrl = null,
                    pdfUrl = "https://focomues-iliad.fr/actualites/nao-2026",
                    excerpt = "Résumé hors ligne",
                    content = "Détail complet disponible sans réseau.",
                    position = 0,
                    cachedAt = 1_700_000_000_000L
                )
            )
        )
        database.focomContentCacheDao().upsert(
            CachedFocomContent(
                rawJson = """
                    {
                      "newsList": [
                        {
                          "id": "gh-cached-1",
                          "title": "Flash Terrain Hors Ligne",
                          "description": "Info locale",
                          "content": "Texte complet",
                          "date": "2026-11-18",
                          "category": "CSE"
                        }
                      ]
                    }
                """.trimIndent(),
                sourceUrl = "https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/main/data/focom-content.json",
                cachedAt = 1_700_000_000_000L
            )
        )

        var isOffline = true
        val toggleableSupabase = object : SupabaseService {
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
                if (isOffline) {
                    throw IOException("Airplane mode")
                }
                return Response.success(
                    listOf(
                        SupabaseArticle(
                            id = "live-nao-2",
                            title = "Nouvel article en ligne",
                            content = "Contenu synchronisé",
                            excerpt = "Extrait en ligne",
                            category = "actualite",
                            slug = "nouvel-article",
                            publishedAt = "2026-11-19T10:00:00Z",
                            createdAt = "2026-11-19T10:00:00Z"
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

        val offlineGitHubService = Proxy.newProxyInstance(
            GitHubService::class.java.classLoader,
            arrayOf(GitHubService::class.java)
        ) { _, _, _ ->
            throw IOException("Offline")
        } as GitHubService

        val repository = GitHubRepository(
            savedRepoDao = database.savedRepoDao(),
            contactMessageDao = database.contactMessageDao(),
            articleCacheDao = database.articleCacheDao(),
            flashBannerCacheDao = database.flashBannerCacheDao(),
            gitHubService = offlineGitHubService,
            supabaseService = toggleableSupabase,
            supabasePublishableKey = "sb_publishable_test",
            ioDispatcher = testDispatcher,
            focomContentCacheDao = database.focomContentCacheDao()
        )

        val viewModel = GitResumeViewModel(repository)
        advanceUntilIdle()

        // Offline startup immediately serves Room cached articles without surfacing an error state
        val offlineState = viewModel.publishedArticlesState.value
        assertTrue(offlineState is UiState.Success<List<FocomPublication>>)
        assertEquals("Article en cache NAO 2026", (offlineState as UiState.Success).data.first().title)
        assertTrue(viewModel.isOfflineArticleCache.value)
        assertNull(viewModel.articlesSyncError.value)

        val focomOfflineState = viewModel.focomContentState.value
        assertTrue(focomOfflineState is UiState.Success)
        assertEquals("Flash Terrain Hors Ligne", (focomOfflineState as UiState.Success).data.newsList.first().title)

        // Verify HomeScreen renders the cached article and offline cache banner
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.FR,
                LocalAppStrings provides FrenchStrings
            ) {
                MyApplicationTheme {
                    HomeScreen(
                        viewModel = viewModel,
                        themeModeName = "System",
                        onThemeModeSelected = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("actualites_list")
            .performScrollToNode(hasTestTag("offline_cache_banner"))
        composeTestRule.onNodeWithTag("offline_cache_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("actualites_list")
            .performScrollToNode(hasText("Article en cache NAO 2026"))
        composeTestRule.onNodeWithText("Article en cache NAO 2026").assertIsDisplayed()

        // Reconnect and refresh: live data updates Room and clears offline cache indicator
        isOffline = false
        viewModel.refreshPublishedArticles(force = true)
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        val liveState = viewModel.publishedArticlesState.value
        assertTrue(liveState is UiState.Success<List<FocomPublication>>)
        assertEquals("Nouvel article en ligne", (liveState as UiState.Success).data.first().title)
        assertFalse(viewModel.isOfflineArticleCache.value)
        assertEquals("Nouvel article en ligne", repository.getCachedArticles().first().title)
    }
}
