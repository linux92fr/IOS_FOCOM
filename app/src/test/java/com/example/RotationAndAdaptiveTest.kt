package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.remote.GitHubService
import com.example.data.repository.GitHubRepository
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.FrenchStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GitResumeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RotationAndAdaptiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var viewModel: GitResumeViewModel
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
        val gitHubService = Proxy.newProxyInstance(
            GitHubService::class.java.classLoader,
            arrayOf(GitHubService::class.java)
        ) { _, _, _ ->
            throw IOException("Stubbed in test")
        } as GitHubService
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
        viewModel = GitResumeViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun `splash screen uses compact horizontal layout in landscape while preserving snowfall`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.FR,
                LocalAppStrings provides FrenchStrings
            ) {
                MyApplicationTheme {
                    SplashScreen(onAnimationFinished = {})
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_landscape_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("splash_logo_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("splash_snowfall_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("FOCOM UES ILIAD").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `home screen uses bottom navigation bar in portrait`() {
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
        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("landscape_navigation_rail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `home screen switches to side navigation rail in landscape and preserves selected tab`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
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
        composeTestRule.onNodeWithTag("landscape_navigation_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertDoesNotExist()

        composeTestRule.onNodeWithTag("nav_rail_tab_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(FrenchStrings.rightsTitle).assertIsDisplayed()

        // Simulate Activity recreation on screen rotation
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("landscape_navigation_rail").assertIsDisplayed()
        composeTestRule.onNodeWithText(FrenchStrings.rightsTitle).assertIsDisplayed()
    }
}
