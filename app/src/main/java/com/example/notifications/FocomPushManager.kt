package com.example.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging

data class PushPreferencesState(
    val breakingNewsEnabled: Boolean = true,
    val updatesEnabled: Boolean = true,
    val promptDismissed: Boolean = false,
    val isFirebaseConfigured: Boolean = false,
    val fcmToken: String? = null
)

object FocomPushManager {

    private const val TAG = "FocomPushManager"
    private const val PREFS_NAME = "focom_preferences"

    const val TOPIC_BREAKING_NEWS = "focom_breaking_news"
    const val TOPIC_SYNDICATE_UPDATES = "focom_updates"

    private const val KEY_BREAKING_ENABLED = "push_breaking_news_enabled"
    private const val KEY_UPDATES_ENABLED = "push_updates_enabled"
    private const val KEY_PROMPT_DISMISSED = "push_prompt_dismissed"
    private const val KEY_FCM_TOKEN = "fcm_registration_token"

    fun getPreferencesState(context: Context): PushPreferencesState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return PushPreferencesState(
            breakingNewsEnabled = prefs.getBoolean(KEY_BREAKING_ENABLED, true),
            updatesEnabled = prefs.getBoolean(KEY_UPDATES_ENABLED, true),
            promptDismissed = prefs.getBoolean(KEY_PROMPT_DISMISSED, false),
            isFirebaseConfigured = isFirebaseConfigured(context),
            fcmToken = prefs.getString(KEY_FCM_TOKEN, null)?.takeIf { it.isNotBlank() }
        )
    }

    fun isFirebaseConfigured(context: Context): Boolean {
        val existingApps = runCatching { FirebaseApp.getApps(context) }.getOrNull()
        if (!existingApps.isNullOrEmpty()) return true

        val options = runCatching { FirebaseOptions.fromResource(context) }.getOrNull() ?: return false
        return runCatching {
            FirebaseApp.initializeApp(context.applicationContext, options)
            true
        }.getOrDefault(false)
    }

    fun setBreakingNewsEnabled(context: Context, enabled: Boolean): PushPreferencesState {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BREAKING_ENABLED, enabled)
            .apply()
        syncTopicSubscriptions(context)
        return getPreferencesState(context)
    }

    fun setUpdatesEnabled(context: Context, enabled: Boolean): PushPreferencesState {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_UPDATES_ENABLED, enabled)
            .apply()
        syncTopicSubscriptions(context)
        return getPreferencesState(context)
    }

    fun setPromptDismissed(context: Context, dismissed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PROMPT_DISMISSED, dismissed)
            .apply()
    }

    fun saveFcmToken(context: Context, token: String) {
        if (token.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    fun syncTopicSubscriptions(context: Context) {
        if (!isFirebaseConfigured(context)) return
        val prefsState = getPreferencesState(context)

        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            if (prefsState.breakingNewsEnabled) {
                messaging.subscribeToTopic(TOPIC_BREAKING_NEWS)
            } else {
                messaging.unsubscribeFromTopic(TOPIC_BREAKING_NEWS)
            }

            if (prefsState.updatesEnabled) {
                messaging.subscribeToTopic(TOPIC_SYNDICATE_UPDATES)
            } else {
                messaging.unsubscribeFromTopic(TOPIC_SYNDICATE_UPDATES)
            }

            messaging.token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    saveFcmToken(context, token)
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "Unable to sync FCM topics", error)
        }
    }
}
