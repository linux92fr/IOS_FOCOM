package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.data.repository.FocomDataRepository
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.FrenchStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.ContactsTabContent
import com.example.ui.screens.SyndicatTabContent
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrivacyPolicyDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `union tab opens and closes French privacy policy dialog`() {
        var showDialog by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.FR,
                LocalAppStrings provides FrenchStrings
            ) {
                MyApplicationTheme {
                    SyndicatTabContent(
                        delegatesList = FocomDataRepository.realDelegatesList,
                        onContactClicked = {},
                        onJoinClicked = {},
                        onOpenPrivacyPolicy = { showDialog = true }
                    )
                    if (showDialog) {
                        PrivacyPolicyDialog(onDismissRequest = { showDialog = false })
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("syndicat_list")
            .performScrollToNode(hasTestTag("open_privacy_policy_button"))
        composeTestRule.onNodeWithTag("open_privacy_policy_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag("privacy_policy_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Politique de confidentialité").assertIsDisplayed()
        composeTestRule.onNodeWithText("6. Vos droits (RGPD)")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("close_privacy_policy_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag("privacy_policy_dialog").assertDoesNotExist()
    }

    @Test
    fun `contact tab displays privacy policy link in English and opens English dialog`() {
        var showDialog by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.EN,
                LocalAppStrings provides EnglishStrings
            ) {
                MyApplicationTheme {
                    ContactsTabContent(
                        delegatesList = FocomDataRepository.realDelegatesList,
                        contactMessages = emptyList(),
                        searchQuery = "",
                        onSearchQueryChanged = {},
                        onSendMessage = { _, _, _, _, _ -> },
                        onOpenPrivacyPolicy = { showDialog = true }
                    )
                    if (showDialog) {
                        PrivacyPolicyDialog(onDismissRequest = { showDialog = false })
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("contacts_list")
            .performScrollToNode(hasTestTag("contact_privacy_policy_button"))
        composeTestRule.onNodeWithTag("contact_privacy_policy_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag("privacy_policy_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
        composeTestRule.onNodeWithText("6. Your GDPR Rights")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
