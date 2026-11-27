package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.FocomMessagingService
import com.example.notifications.FocomNotificationHelper
import com.example.notifications.FocomPushManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FcmNotificationTest {

    private lateinit var application: Application
    private lateinit var context: Context

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        context = application
        context.getSharedPreferences("focom_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `ensureNotificationChannels creates breaking news and syndicate updates channels`() {
        FocomNotificationHelper.ensureNotificationChannels(context)

        val manager = context.getSystemService(NotificationManager::class.java)
        val breaking = manager.getNotificationChannel(FocomNotificationHelper.CHANNEL_BREAKING_NEWS)
        val updates = manager.getNotificationChannel(FocomNotificationHelper.CHANNEL_SYNDICATE_UPDATES)

        assertNotNull(breaking)
        assertNotNull(updates)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, breaking.importance)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, updates.importance)
    }

    @Test
    fun `isFirebaseConfigured and syncTopicSubscriptions fail-safe without google-services json`() {
        assertFalse(FocomPushManager.isFirebaseConfigured(context))
        // Should execute cleanly without throwing IllegalStateException
        FocomPushManager.syncTopicSubscriptions(context)
        val prefs = FocomPushManager.getPreferencesState(context)
        assertTrue(prefs.breakingNewsEnabled)
        assertTrue(prefs.updatesEnabled)
        assertFalse(prefs.isFirebaseConfigured)
    }

    @Test
    fun `incoming FCM breaking news payload posts high-priority notification with deep link when granted`() {
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val posted = FocomMessagingService.handleIncomingPayload(
            context = context,
            data = mapOf(
                "type" to "breaking_news",
                "title" to "Flash NAO UES ILIAD",
                "body" to "Réunion extraordinaire ouverte aujourd'hui à 14h.",
                "article_id" to "flash-nao-2026",
                "target_tab" to "0"
            )
        )

        assertTrue(posted)
        val manager = context.getSystemService(NotificationManager::class.java)
        val shadowManager = Shadows.shadowOf(manager)
        assertEquals(1, shadowManager.allNotifications.size)
        val notification = shadowManager.allNotifications.first()
        assertEquals(FocomNotificationHelper.CHANNEL_BREAKING_NEWS, notification.channelId)
    }

    @Test
    fun `incoming FCM payload respects disabled breaking news preference`() {
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        FocomPushManager.setBreakingNewsEnabled(context, false)

        val posted = FocomMessagingService.handleIncomingPayload(
            context = context,
            data = mapOf(
                "type" to "urgent",
                "title" to "Alerte Grève",
                "body" to "Préavis déposé."
            )
        )

        assertFalse(posted)
        val manager = context.getSystemService(NotificationManager::class.java)
        assertEquals(0, Shadows.shadowOf(manager).allNotifications.size)
    }

    @Test
    fun `deep link extraction reads target tab and article id and clears cleanly`() {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(FocomNotificationHelper.EXTRA_TARGET_TAB, 3)
            putExtra(FocomNotificationHelper.EXTRA_ARTICLE_ID, "tract-nao-1")
            putExtra(FocomNotificationHelper.EXTRA_NOTIFICATION_TYPE, FocomNotificationHelper.CHANNEL_SYNDICATE_UPDATES)
        }

        val deepLink = FocomNotificationHelper.extractDeepLink(intent)
        assertNotNull(deepLink)
        assertEquals(3, deepLink?.targetTab)
        assertEquals("tract-nao-1", deepLink?.articleId)

        FocomNotificationHelper.clearDeepLinkExtras(intent)
        assertNull(FocomNotificationHelper.extractDeepLink(intent))
    }
}
