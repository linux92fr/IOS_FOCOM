package com.example.notifications

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Locale

class FocomMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token received")
        FocomPushManager.saveFcmToken(applicationContext, token)
        FocomPushManager.syncTopicSubscriptions(applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        handleIncomingPayload(
            context = applicationContext,
            data = remoteMessage.data,
            notificationTitle = remoteMessage.notification?.title,
            notificationBody = remoteMessage.notification?.body
        )
    }

    companion object {
        private const val TAG = "FocomMessagingService"

        @VisibleForTesting
        internal fun handleIncomingPayload(
            context: Context,
            data: Map<String, String>,
            notificationTitle: String? = null,
            notificationBody: String? = null
        ): Boolean {
            val prefs = FocomPushManager.getPreferencesState(context)

            val rawChannelType = data["type"]
                ?: data["channel"]
                ?: data["priority"]
                ?: ""
            val isBreaking = rawChannelType.lowercase(Locale.ROOT).let {
                it.contains("breaking") || it.contains("urgent") || it.contains("flash") || it == "high"
            }

            if (isBreaking && !prefs.breakingNewsEnabled) return false
            if (!isBreaking && !prefs.updatesEnabled) return false

            val title = (notificationTitle ?: data["title"])?.trim().orEmpty().ifBlank {
                "FO COM UES ILIAD"
            }
            val body = (notificationBody ?: data["body"] ?: data["message"])?.trim().orEmpty().ifBlank {
                "Nouvelle communication syndicale disponible."
            }

            val channelId = if (isBreaking) {
                FocomNotificationHelper.CHANNEL_BREAKING_NEWS
            } else {
                FocomNotificationHelper.CHANNEL_SYNDICATE_UPDATES
            }

            val targetTab = resolveTargetTab(data["target_tab"] ?: data["screen"] ?: data["tab"])
            val articleId = data["article_id"]?.takeIf { it.isNotBlank() }

            return FocomNotificationHelper.showSyndicateNotification(
                context = context,
                title = title,
                body = body,
                channelId = channelId,
                targetTab = targetTab,
                articleId = articleId
            )
        }

        private fun resolveTargetTab(rawTab: String?): Int {
            val numeric = rawTab?.toIntOrNull()
            if (numeric != null) return numeric.coerceIn(0, 5)
            return when (rawTab?.trim()?.lowercase(Locale.ROOT)) {
                "union", "syndicat" -> 1
                "rights", "droits" -> 2
                "publications", "presse" -> 3
                "help", "aide", "requests", "demandes" -> 4
                "contact", "messages", "delegates" -> 5
                else -> 0
            }
        }
    }
}
