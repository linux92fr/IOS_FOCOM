package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

data class NotificationDeepLink(
    val targetTab: Int = 0,
    val articleId: String? = null
)

object FocomNotificationHelper {

    const val CHANNEL_BREAKING_NEWS = "focom_breaking_news"
    const val CHANNEL_SYNDICATE_UPDATES = "focom_syndicate_updates"

    const val EXTRA_TARGET_TAB = "focom_target_tab"
    const val EXTRA_ARTICLE_ID = "focom_article_id"
    const val EXTRA_NOTIFICATION_TYPE = "focom_notification_type"

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val isEnglish = isEnglishPreferred(context)

        val breakingChannel = NotificationChannel(
            CHANNEL_BREAKING_NEWS,
            if (isEnglish) "Breaking News & Urgent Alerts" else "Alertes urgentes & Flash Info",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = if (isEnglish) {
                "Urgent union alerts, NAO flashes, and breaking announcements from FO COM UES ILIAD."
            } else {
                "Alertes syndicales prioritaires, flashs NAO et communications urgentes FO COM UES ILIAD."
            }
            enableVibration(true)
            enableLights(true)
        }

        val updatesChannel = NotificationChannel(
            CHANNEL_SYNDICATE_UPDATES,
            if (isEnglish) "Union News & Agreements" else "Actualités & Accords syndicaux",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = if (isEnglish) {
                "New articles, collective agreements, CSE updates, and publications."
            } else {
                "Nouveaux articles, accords d'entreprise, comptes rendus CSE et publications."
            }
        }

        manager.createNotificationChannel(breakingChannel)
        manager.createNotificationChannel(updatesChannel)
    }

    fun hasPostNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun areNotificationsEffectivelyEnabled(context: Context): Boolean {
        return hasPostNotificationPermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showSyndicateNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = CHANNEL_SYNDICATE_UPDATES,
        targetTab: Int = 0,
        articleId: String? = null,
        notificationId: Int = stableNotificationId(articleId, title)
    ): Boolean {
        ensureNotificationChannels(context)
        if (!hasPostNotificationPermission(context)) return false

        val resolvedChannel = if (channelId == CHANNEL_BREAKING_NEWS) {
            CHANNEL_BREAKING_NEWS
        } else {
            CHANNEL_SYNDICATE_UPDATES
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_TAB, targetTab)
            if (!articleId.isNullOrBlank()) {
                putExtra(EXTRA_ARTICLE_ID, articleId)
            }
            putExtra(EXTRA_NOTIFICATION_TYPE, resolvedChannel)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = if (resolvedChannel == CHANNEL_BREAKING_NEWS) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(context, resolvedChannel)
            .setSmallIcon(R.drawable.ic_notification_focom)
            .setColor(ContextCompat.getColor(context, R.color.focom_notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun extractDeepLink(intent: Intent?): NotificationDeepLink? {
        if (intent == null) return null
        val hasTab = intent.hasExtra(EXTRA_TARGET_TAB)
        val articleId = intent.getStringExtra(EXTRA_ARTICLE_ID)?.takeIf { it.isNotBlank() }
        val hasType = intent.hasExtra(EXTRA_NOTIFICATION_TYPE)
        if (!hasTab && articleId == null && !hasType) return null
        val targetTab = intent.getIntExtra(EXTRA_TARGET_TAB, 0).coerceIn(0, 5)
        return NotificationDeepLink(targetTab = targetTab, articleId = articleId)
    }

    fun clearDeepLinkExtras(intent: Intent?) {
        intent?.removeExtra(EXTRA_TARGET_TAB)
        intent?.removeExtra(EXTRA_ARTICLE_ID)
        intent?.removeExtra(EXTRA_NOTIFICATION_TYPE)
    }

    private fun stableNotificationId(articleId: String?, title: String): Int {
        val seed = articleId?.takeIf { it.isNotBlank() } ?: title
        return (seed.hashCode() and 0x7FFFFFFF).coerceAtLeast(1001)
    }

    private fun isEnglishPreferred(context: Context): Boolean {
        val prefs = context.getSharedPreferences("focom_preferences", Context.MODE_PRIVATE)
        val saved = prefs.getString("app_language", null)
        return when {
            saved.equals("EN", ignoreCase = true) -> true
            saved.equals("FR", ignoreCase = true) -> false
            else -> Locale.getDefault().language.equals("en", ignoreCase = true)
        }
    }
}
