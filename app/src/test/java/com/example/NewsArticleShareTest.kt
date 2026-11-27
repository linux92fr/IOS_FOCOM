package com.example

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FocomNews
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.FrenchStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.NewsCardPremium
import com.example.ui.screens.NewsDetailBottomSheet
import com.example.ui.screens.buildNewsShareIntent
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewsArticleShareTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleNews = FocomNews(
        id = "nao-2026-update",
        title = "NAO 2026 : Propositions FO COM",
        description = "Synthèse des revendications salariales UES ILIAD.",
        content = "Contenu complet de l'actualité.",
        date = "19/11/2026",
        category = "Négociations",
        isHotState = true,
        webUrl = "https://focomues-iliad.fr/actualites/nao-2026-update"
    )

    @Test
    fun `buildNewsShareIntent wraps text ACTION_SEND in Android ShareSheet ACTION_CHOOSER`() {
        val chooser = buildNewsShareIntent(sampleNews, FrenchStrings)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)

        @Suppress("DEPRECATION")
        val targetIntent = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("text/plain", targetIntent?.type)
        assertEquals(sampleNews.title, targetIntent?.getStringExtra(Intent.EXTRA_SUBJECT))

        val sharedText = targetIntent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(sharedText.contains(sampleNews.title))
        assertTrue(sharedText.contains(sampleNews.webUrl!!))
    }

    @Test
    fun `news card share button launches Android ShareSheet with article link`() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.FR,
                LocalAppStrings provides FrenchStrings
            ) {
                MyApplicationTheme {
                    NewsCardPremium(item = sampleNews, onClick = {})
                }
            }
        }

        composeTestRule.onNodeWithTag("share_news_${sampleNews.id}")
            .assertIsDisplayed()
            .performClick()

        val launched = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(launched)
        assertEquals(Intent.ACTION_CHOOSER, launched.action)

        @Suppress("DEPRECATION")
        val innerIntent = launched.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertTrue(innerIntent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty().contains(sampleNews.webUrl!!))
    }

    @Test
    fun `news detail bottom sheet share button launches Android ShareSheet in English`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val noUrlNews = sampleNews.copy(id = "local-1", webUrl = null)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.EN,
                LocalAppStrings provides EnglishStrings
            ) {
                MyApplicationTheme {
                    NewsDetailBottomSheet(news = noUrlNews, onDismissRequest = {})
                }
            }
        }

        composeTestRule.onNodeWithTag("detail_share_article_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        val launched = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(launched)
        assertEquals(Intent.ACTION_CHOOSER, launched.action)

        @Suppress("DEPRECATION")
        val innerIntent = launched.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertTrue(
            innerIntent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                .contains("https://focomues-iliad.fr/actualites")
        )
    }
}
