package com.example

import android.content.Intent
import android.os.Bundle
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.remote.GitHubService
import com.example.data.remote.SupabaseService
import com.example.data.repository.GitHubRepository
import com.example.notifications.FocomNotificationHelper
import com.example.notifications.FocomPushManager
import com.example.notifications.NotificationDeepLink
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.strings
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GitResumeViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var gitHubService: GitHubService
    private lateinit var gitHubRepository: GitHubRepository
    private lateinit var viewModel: GitResumeViewModel
    private lateinit var connectivityManager: ConnectivityManager
    private var pendingDeepLinkState by mutableStateOf<NotificationDeepLink?>(null)
    private val articleNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (::viewModel.isInitialized) runOnUiThread {
                viewModel.refreshPublishedArticles(force = false)
                viewModel.refreshFlashBanners(force = false)
                viewModel.refreshFocomContent(force = false)
                viewModel.retryPendingMessages()
            }
        }
    }

    enum class AppThemeMode(val label: String) {
        System("Système"),
        Light("Clair"),
        Dark("Sombre")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FocomNotificationHelper.ensureNotificationChannels(this)
        FocomPushManager.syncTopicSubscriptions(this)
        pendingDeepLinkState = FocomNotificationHelper.extractDeepLink(intent)
        FocomNotificationHelper.clearDeepLinkExtras(intent)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "focom_database"
        )
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        gitHubService = retrofit.create(GitHubService::class.java)
        val supabaseService = createSupabaseService(moshi)
        val supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
            .trim()
            .takeUnless { it.isBlank() || it.startsWith("YOUR_") }
            .orEmpty()
        gitHubRepository = GitHubRepository(
            savedRepoDao = database.savedRepoDao(),
            contactMessageDao = database.contactMessageDao(),
            articleCacheDao = database.articleCacheDao(),
            flashBannerCacheDao = database.flashBannerCacheDao(),
            gitHubService = gitHubService,
            supabaseService = supabaseService,
            supabasePublishableKey = supabaseKey,
            focomContentCacheDao = database.focomContentCacheDao()
        )

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return GitResumeViewModel(gitHubRepository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[GitResumeViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val preferences = remember {
                getSharedPreferences("focom_preferences", MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    runCatching {
                        AppThemeMode.valueOf(preferences.getString("theme_mode", AppThemeMode.System.name)!!)
                    }.getOrDefault(AppThemeMode.System)
                )
            }
            var appLanguage by remember {
                mutableStateOf(
                    runCatching {
                        AppLanguage.valueOf(
                            preferences.getString("app_language", AppLanguage.FR.name) ?: AppLanguage.FR.name
                        )
                    }.getOrDefault(AppLanguage.FR)
                )
            }
            val darkTheme = when (themeMode) {
                AppThemeMode.System -> isSystemInDarkTheme()
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                onDispose {}
            }

            CompositionLocalProvider(
                LocalAppLanguage provides appLanguage,
                LocalAppStrings provides appLanguage.strings()
            ) {
                MyApplicationTheme(darkTheme = darkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val startDestination = if (pendingDeepLinkState != null) "home" else "splash"

                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("splash") {
                                SplashScreen(onAnimationFinished = {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                })
                            }
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    themeModeName = themeMode.name,
                                    onThemeModeSelected = { modeName ->
                                        themeMode = AppThemeMode.entries.firstOrNull {
                                            it.name == modeName || it.label == modeName
                                        } ?: AppThemeMode.System
                                        preferences.edit().putString("theme_mode", themeMode.name).apply()
                                    },
                                    onLanguageSelected = { newLanguage ->
                                        appLanguage = newLanguage
                                        preferences.edit().putString("app_language", newLanguage.name).apply()
                                    },
                                    pendingDeepLink = pendingDeepLinkState,
                                    onDeepLinkConsumed = { pendingDeepLinkState = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val deepLink = FocomNotificationHelper.extractDeepLink(intent)
        if (deepLink != null) {
            pendingDeepLinkState = deepLink
            FocomNotificationHelper.clearDeepLinkExtras(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        runCatching { connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), articleNetworkCallback) }
    }

    override fun onStop() {
        if (::connectivityManager.isInitialized) {
            runCatching { connectivityManager.unregisterNetworkCallback(articleNetworkCallback) }
        }
        super.onStop()
    }

    private fun createSupabaseService(moshi: Moshi): SupabaseService? {
        val rawUrl = BuildConfig.SUPABASE_URL
            .trim()
            .takeUnless { it.isBlank() || it.contains("YOUR_PROJECT_REF") }
            ?: return null

        val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }

        return runCatching {
            Retrofit.Builder()
                .baseUrl(normalizedUrl.trimEnd('/') + "/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SupabaseService::class.java)
        }.getOrNull()
    }
}
