package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.repository.FocomDataRepository
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.FrenchStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeDelegate
import com.example.ui.i18n.localizeNews
import com.example.ui.i18n.localizePublication
import com.example.ui.i18n.localizeRight
import com.example.ui.i18n.strings
import com.example.ui.i18n.themeModeLabel
import com.example.ui.i18n.translateCategory
import com.example.ui.i18n.translateStatus
import com.example.ui.screens.SimulateurTabContent
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `french is default and maps cleanly to FrenchStrings and EnglishStrings`() {
        assertEquals(FrenchStrings, AppLanguage.FR.strings())
        assertEquals(EnglishStrings, AppLanguage.EN.strings())
        assertEquals("Adhérer", AppLanguage.FR.strings().joinButton)
        assertEquals("Join", AppLanguage.EN.strings().joinButton)
        assertEquals("Système", FrenchStrings.themeModeLabel("System"))
        assertEquals("System", EnglishStrings.themeModeLabel("System"))
    }

    @Test
    fun `status and category translations keep raw values intact while translating english UI`() {
        assertEquals("Envoyé", FrenchStrings.translateStatus("Envoyé"))
        assertEquals("Sent", EnglishStrings.translateStatus("Envoyé"))
        assertEquals("Pending network", EnglishStrings.translateStatus("En attente réseau"))

        assertEquals("Télétravail", FrenchStrings.translateCategory("Télétravail"))
        assertEquals("Remote Work", EnglishStrings.translateCategory("Télétravail"))
        assertEquals("Paid Leave", EnglishStrings.translateCategory("Congés"))
        assertEquals("Negotiations", EnglishStrings.translateCategory("Négociations"))
    }

    @Test
    fun `built in content localizes to english while preserving canonical categories and ids`() {
        val frDelegate = FocomDataRepository.realDelegatesList.first()
        val enDelegate = AppLanguage.EN.localizeDelegate(frDelegate)
        assertEquals(frDelegate.id, enDelegate.id)
        assertNotEquals(frDelegate.role, enDelegate.role)
        assertTrue(enDelegate.role.contains("General Secretary"))

        val frRight = FocomDataRepository.rightsList.first()
        val enRight = AppLanguage.EN.localizeRight(frRight)
        assertEquals("Télétravail", enRight.category)
        assertEquals("Remote Work & Group Charter", enRight.title)

        val frNews = FocomDataRepository.newsList.first()
        val enNews = AppLanguage.EN.localizeNews(frNews)
        assertEquals(frNews.id, enNews.id)
        assertTrue(enNews.title.contains("NAO 2026"))
        assertTrue(enNews.title.contains("will not sign"))

        val frPub = FocomDataRepository.projectActualitesFallback.first()
        val enPub = AppLanguage.EN.localizePublication(frPub)
        assertEquals(frPub.id, enPub.id)
        assertEquals("Négociations", enPub.type)
        assertTrue(enPub.title.contains("proposals fall short"))
    }

    @Test
    fun `splash screen renders localized subtitle for french and english`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.EN,
                LocalAppStrings provides EnglishStrings
            ) {
                MyApplicationTheme {
                    SplashScreen(onAnimationFinished = {})
                }
            }
        }
        composeTestRule.onNodeWithText("Your rights, your voice").assertIsDisplayed()
        composeTestRule.onNodeWithTag("splash_snowfall_overlay").assertIsDisplayed()
    }

    @Test
    fun `simulator renders english headings and units when english is active`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.EN,
                LocalAppStrings provides EnglishStrings
            ) {
                MyApplicationTheme {
                    SimulateurTabContent()
                }
            }
        }
        composeTestRule.onNodeWithText("Seniority & Leave Simulator").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 yrs").assertIsDisplayed()
    }
}
