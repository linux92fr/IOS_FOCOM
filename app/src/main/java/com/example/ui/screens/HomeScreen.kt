package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.R
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ContactMessage
import com.example.data.model.FocomDelegate
import com.example.data.model.FocomContent
import com.example.data.model.FocomNews
import com.example.data.model.FocomPublication
import com.example.data.model.FocomFlashBanner
import com.example.data.model.FocomRight
import com.example.data.repository.FocomDataRepository
import com.example.data.repository.GitHubRepository
import com.example.data.repository.SimulatorLogic
import com.example.notifications.FocomPushManager
import com.example.notifications.NotificationDeepLink
import com.example.ui.components.MarkdownContent
import com.example.ui.components.NotificationOptInBanner
import com.example.ui.components.NotificationSettingsBottomSheet
import com.example.ui.components.PrivacyPolicyAccessCard
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.components.rememberNotificationPermissionState
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeDelegate
import com.example.ui.i18n.localizeNews
import com.example.ui.i18n.localizePublication
import com.example.ui.i18n.localizeRight
import com.example.ui.i18n.themeModeLabel
import com.example.ui.i18n.translateCategory
import com.example.ui.i18n.translateStatus
import com.example.ui.viewmodel.GitResumeViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private fun openExternalPdf(
    context: Context,
    url: String,
    failureMessage: String = "Aucune application disponible pour ouvrir ce document."
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(
            context,
            failureMessage,
            Toast.LENGTH_LONG
        ).show()
    }
}

internal fun buildNewsShareIntent(news: FocomNews, strings: AppStrings): Intent {
    val articleUrl = news.webUrl
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "https://focomues-iliad.fr/actualites"
    val shareText = buildString {
        append(news.title)
        if (news.description.isNotBlank()) {
            append("\n\n")
            append(news.description)
        }
        append("\n\n")
        append(articleUrl)
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, news.title)
        putExtra(Intent.EXTRA_TITLE, news.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    return Intent.createChooser(sendIntent, strings.shareArticleChooserTitle)
}

private fun shareNewsArticle(context: Context, news: FocomNews, strings: AppStrings) {
    runCatching {
        context.startActivity(buildNewsShareIntent(news, strings))
    }.onFailure {
        Toast.makeText(
            context,
            strings.openLinkFailed,
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun FocomContent.withFallback(fallback: FocomContent): FocomContent {
    return FocomContent(
        newsList = newsList.ifEmpty { fallback.newsList },
        rightsList = rightsList.ifEmpty { fallback.rightsList },
        delegatesList = delegatesList.ifEmpty { fallback.delegatesList },
        publicationsList = publicationsList.ifEmpty { fallback.publicationsList }
    )
}

private fun FocomDelegate.resolvedPhotoResName(): String? {
    val normalizedName = name.uppercase(Locale.FRANCE)
    return photoResName ?: when {
        normalizedName.contains("SIDIB") -> "delegate_nysidibe"
        normalizedName.contains("ZERARKA") -> "delegate_mzerarka"
        normalizedName.contains("LAVILLE") -> "delegate_alaville"
        normalizedName.contains("RACAULT") -> "delegate_fracault"
        normalizedName.contains("BROU") -> "delegate_dbrou"
        normalizedName.contains("KENDIRA") -> "delegate_fkendira"
        else -> null
    }
}

@Composable
private fun rememberDelegatePhotoResId(delegate: FocomDelegate): Int? {
    val context = LocalContext.current
    return remember(delegate.name, delegate.photoResName) {
        delegate.resolvedPhotoResName()?.let { resourceName ->
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }?.takeIf { it != 0 }
    }
}

@Composable
private fun FlashBannersSection(banners: List<FocomFlashBanner>) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        banners.forEach { banner ->
            val backgroundColor = when (banner.type.lowercase()) {
                "urgent" -> Color(0xFFB91C1C)
                "event" -> Color(0xFF5B3A9E)
                "announcement" -> Color(0xFF0F766E)
                else -> Color(0xFF155E75)
            }
            val link = banner.link?.trim().orEmpty()
            val targetUrl = when {
                link.isBlank() -> ""
                link.startsWith("/") -> "https://focomues-iliad.fr" + link
                else -> link
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clickable(enabled = targetUrl.isNotBlank()) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            )
                        }.onFailure {
                            Toast.makeText(
                                context,
                                strings.openLinkFailed,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = banner.message,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (targetUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = strings.openLinkDescription,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GitResumeViewModel,
    themeModeName: String,
    onThemeModeSelected: (String) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    pendingDeepLink: NotificationDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val appLanguage = LocalAppLanguage.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // State parameters bridged and preserved across rotation
    var showMembershipSheet by rememberSaveable { mutableStateOf(false) }
    var showNotificationSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showPrivacyPolicyDialog by rememberSaveable { mutableStateOf(false) }
    var newsSearchQuery by rememberSaveable { mutableStateOf("") }
    var rightsSearchQuery by rememberSaveable { mutableStateOf("") }
    var contactSearchQuery by rememberSaveable { mutableStateOf("") }

    // Bottom sheet active detailed contents (stored by stable ID/title for Bundle-safe rotation preservation)
    var activeDetailNewsKey by rememberSaveable { mutableStateOf<String?>(null) }
    var activeDetailRightKey by rememberSaveable { mutableStateOf<String?>(null) }
    var activeDetailPubKey by rememberSaveable { mutableStateOf<String?>(null) }
    val remoteContentState by viewModel.focomContentState.collectAsStateWithLifecycle()
    val focomContentRefreshing by viewModel.focomContentRefreshing.collectAsStateWithLifecycle()
    val contactMessages by viewModel.contactMessages.collectAsStateWithLifecycle()
    val publishedArticlesState by viewModel.publishedArticlesState.collectAsStateWithLifecycle()
    val isOfflineArticleCache by viewModel.isOfflineArticleCache.collectAsStateWithLifecycle()
    val articlesLastCachedAt by viewModel.articlesLastCachedAt.collectAsStateWithLifecycle()
    val articlesRefreshing by viewModel.articlesRefreshing.collectAsStateWithLifecycle()
    val articlesSyncError by viewModel.articlesSyncError.collectAsStateWithLifecycle()
    val flashBannersState by viewModel.flashBannersState.collectAsStateWithLifecycle()
    val flashBanners = (flashBannersState as? UiState.Success)?.data.orEmpty()
    val defaultContent = FocomDataRepository.defaultContent
    val focomContent = when (val state = remoteContentState) {
        is UiState.Success -> state.data.withFallback(defaultContent)
        else -> defaultContent
    }
    val visibleDelegates = remember(focomContent.delegatesList, appLanguage) {
        focomContent.delegatesList
            .filterNot { delegate ->
                delegate.name.contains("CRITON", ignoreCase = true) ||
                        delegate.role.contains("Retraité", ignoreCase = true)
            }
            .map { appLanguage.localizeDelegate(it) }
    }
    val presseItems = when (val state = publishedArticlesState) {
        is UiState.Success -> state.data
        else -> emptyList()
    }
    val fallbackPublications = remember(focomContent, appLanguage) {
        focomContent.newsList.map { news ->
            val localizedNews = appLanguage.localizeNews(news)
            FocomPublication(
                id = localizedNews.id,
                title = localizedNews.title,
                type = localizedNews.category,
                date = localizedNews.date,
                thumbnailUrl = localizedNews.imageUrl,
                pdfUrl = localizedNews.webUrl ?: "https://focomues-iliad.fr/actualites",
                excerpt = localizedNews.description,
                content = localizedNews.content
            )
        }.ifEmpty {
            FocomDataRepository.projectActualitesFallback.map { appLanguage.localizePublication(it) }
        }
    }
    val displayedPublications = remember(presseItems, focomContent.publicationsList, fallbackPublications, appLanguage) {
        val merged = buildList {
            addAll(presseItems)
            focomContent.publicationsList.forEach { githubPub ->
                val alreadyPresent = any {
                    it.id == githubPub.id || it.title.equals(githubPub.title, ignoreCase = true)
                }
                if (!alreadyPresent) add(appLanguage.localizePublication(githubPub))
            }
        }
        merged.ifEmpty { fallbackPublications }
    }
    val localizedRights = remember(focomContent.rightsList, appLanguage) {
        focomContent.rightsList.map { appLanguage.localizeRight(it) }
    }
    val articlesLoading = articlesRefreshing || focomContentRefreshing
    val articlesError = articlesSyncError
    val newsItems = remember(presseItems, focomContent.newsList, appLanguage) {
        if (presseItems.isNotEmpty()) {
            val mappedPresse = presseItems.map { pub ->
                FocomNews(
                    id = pub.id,
                    title = pub.title,
                    description = pub.excerpt.ifBlank { pub.content.take(160) },
                    content = pub.content,
                    date = pub.date,
                    category = pub.type,
                    isHotState = false,
                    imageUrl = pub.thumbnailUrl,
                    webUrl = pub.pdfUrl
                )
            }
            val remoteGithubNews = focomContent.newsList
                .filter { githubNews ->
                    mappedPresse.none {
                        it.id == githubNews.id || it.title.equals(githubNews.title, ignoreCase = true)
                    }
                }
                .map { appLanguage.localizeNews(it) }
            mappedPresse + remoteGithubNews
        } else {
            focomContent.newsList.map { appLanguage.localizeNews(it) }
        }
    }

    val context = LocalContext.current
    val contactSendResult by viewModel.contactSendResult.collectAsState()

    LaunchedEffect(contactSendResult) {
        val result = contactSendResult ?: return@LaunchedEffect
        val feedback = when (result) {
            GitHubRepository.ContactSendResult.SENT_REMOTE -> strings.contactSentRemoteToast
            GitHubRepository.ContactSendResult.FAILED_SAVED_LOCAL -> strings.contactFailedSavedLocalToast
            GitHubRepository.ContactSendResult.SAVED_LOCAL_ONLY -> strings.contactSavedLocalOnlyToast
        }
        Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
        viewModel.clearContactSendResult()
        viewModel.clearContactSendFeedback()
    }

    val activeDetailNews = remember(activeDetailNewsKey, newsItems) {
        activeDetailNewsKey?.let { key ->
            newsItems.firstOrNull { it.id == key || it.title == key }
        }
    }
    val activeDetailRight = remember(activeDetailRightKey, localizedRights) {
        activeDetailRightKey?.let { key ->
            localizedRights.firstOrNull { it.id == key || it.title == key }
        }
    }
    val activeDetailPub = remember(activeDetailPubKey, displayedPublications) {
        activeDetailPubKey?.let { key ->
            displayedPublications.firstOrNull { it.id == key || it.title == key }
        }
    }

    LaunchedEffect(selectedTab) {
        viewModel.refreshTabDataIfStale(selectedTab)
    }

    LaunchedEffect(pendingDeepLink) {
        val deepLink = pendingDeepLink ?: return@LaunchedEffect
        selectedTab = deepLink.targetTab.coerceIn(0, 5)
        viewModel.syncAllGitHubElements()
        deepLink.articleId?.let { targetId ->
            if (selectedTab == 3) {
                activeDetailPubKey = targetId
            } else {
                activeDetailNewsKey = targetId
            }
        }
        onDeepLinkConsumed()
    }

    BackHandler(
        enabled = selectedTab != 0 &&
            activeDetailNews == null &&
            activeDetailRight == null &&
            activeDetailPub == null &&
            !showMembershipSheet &&
            !showNotificationSettingsSheet &&
            !showPrivacyPolicyDialog
    ) {
        selectedTab = 0
    }

    val tabs = listOf(
        Triple(strings.tabHome, Icons.Default.Home, 0),
        Triple(strings.tabUnion, Icons.Default.Groups, 1),
        Triple(strings.tabRights, Icons.AutoMirrored.Filled.LibraryBooks, 2),
        Triple(strings.tabNews, Icons.AutoMirrored.Filled.Article, 3),
        Triple(strings.tabHelp, Icons.Default.FolderOpen, 4),
        Triple(strings.tabContact, Icons.Default.People, 5)
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useSideNavigationRail = maxWidth >= 600.dp || (maxWidth > maxHeight && maxHeight < 500.dp)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                HeaderSection(
                    onJoinClicked = { showMembershipSheet = true },
                    currentThemeName = themeModeName,
                    onThemeSelected = onThemeModeSelected,
                    currentLanguage = appLanguage,
                    onLanguageSelected = onLanguageSelected,
                    onOpenMessages = {
                        contactSearchQuery = ""
                        selectedTab = 5
                    },
                    compact = useSideNavigationRail
                )
            },
            bottomBar = {
                if (!useSideNavigationRail) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .border(
                                BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant),
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .testTag("bottom_navigation_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars
                                        .union(WindowInsets.displayCutout)
                                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                                )
                                .height(64.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            tabs.forEach { (label, icon, index) ->
                                val isSelected = selectedTab == index
                                val activeColor = MaterialTheme.colorScheme.primary
                                val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { selectedTab = index }
                                        .padding(vertical = 4.dp)
                                        .testTag("nav_tab_$index")
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) activeColor else inactiveColor,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .scale(if (isSelected) 1.1f else 1f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) activeColor else inactiveColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (useSideNavigationRail) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxHeight()
                            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant))
                            .testTag("landscape_navigation_rail")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .width(84.dp)
                                .fillMaxHeight()
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars
                                        .union(WindowInsets.displayCutout)
                                        .only(WindowInsetsSides.Left + WindowInsetsSides.Bottom)
                                )
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            tabs.forEach { (label, icon, index) ->
                                val isSelected = selectedTab == index
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_rail_tab_$index")
                                )
                            }
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (useSideNavigationRail) {
                                Modifier.windowInsetsPadding(
                                    WindowInsets.navigationBars
                                        .union(WindowInsets.displayCutout)
                                        .only(WindowInsetsSides.Right + WindowInsetsSides.Bottom)
                                )
                            } else {
                                Modifier.windowInsetsPadding(
                                    WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                                )
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 880.dp)
                            .fillMaxWidth()
                    ) {
                        if (selectedTab == 0 && flashBanners.isNotEmpty()) {
                            FlashBannersSection(banners = flashBanners)
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> ActualitesTabContent(
                                    newsList = newsItems,
                                    isLoading = articlesLoading,
                                    errorMessage = articlesError,
                                    isOfflineCache = isOfflineArticleCache,
                                    cachedAtMillis = articlesLastCachedAt,
                                    searchQuery = newsSearchQuery,
                                    onSearchQueryChanged = { newsSearchQuery = it },
                                    onRefresh = { viewModel.syncAllGitHubElements() },
                                    onNewsSelected = { activeDetailNewsKey = it.id.ifBlank { it.title } },
                                    onJoinClicked = { showMembershipSheet = true },
                                    onOpenNotificationSettings = { showNotificationSettingsSheet = true }
                                )
                                1 -> SyndicatTabContent(
                                    delegatesList = visibleDelegates,
                                    onContactClicked = {
                                        contactSearchQuery = ""
                                        selectedTab = 5
                                    },
                                    onJoinClicked = { showMembershipSheet = true },
                                    onOpenPrivacyPolicy = { showPrivacyPolicyDialog = true }
                                )
                                2 -> VosDroitsTabContent(
                                    rightsList = localizedRights,
                                    searchQuery = rightsSearchQuery,
                                    onSearchQueryChanged = { rightsSearchQuery = it },
                                    isRefreshing = focomContentRefreshing,
                                    onRefresh = { viewModel.refreshFocomContent() },
                                    onRightSelected = { activeDetailRightKey = it.id.ifBlank { it.title } }
                                )
                                3 -> PublicationsTabContent(
                                    publicationsList = displayedPublications,
                                    isLoading = articlesLoading,
                                    errorMessage = articlesError,
                                    isOfflineCache = isOfflineArticleCache,
                                    cachedAtMillis = articlesLastCachedAt,
                                    onRefresh = { viewModel.syncAllGitHubElements() },
                                    onPubSelected = { activeDetailPubKey = it.id.ifBlank { it.title } }
                                )
                                4 -> ReclamationsTabContent(
                                    onSendRequest = { name, email, phone, subject, message ->
                                        viewModel.sendInternalMessage(name, email, phone, subject, message)
                                    }
                                )
                                5 -> ContactsTabContent(
                                    delegatesList = visibleDelegates,
                                    contactMessages = contactMessages,
                                    searchQuery = contactSearchQuery,
                                    onSearchQueryChanged = { contactSearchQuery = it },
                                    onSendMessage = viewModel::sendInternalMessage,
                                    onOpenPrivacyPolicy = { showPrivacyPolicyDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets implementations for premium editorial styling
    if (activeDetailNews != null) {
        NewsDetailBottomSheet(
            news = appLanguage.localizeNews(activeDetailNews),
            onDismissRequest = { activeDetailNewsKey = null }
        )
    }

    if (activeDetailRight != null) {
        RightDetailBottomSheet(
            right = appLanguage.localizeRight(activeDetailRight),
            onDismissRequest = { activeDetailRightKey = null },
            onContactDelegate = { category ->
                activeDetailRightKey = null
                contactSearchQuery = category
                selectedTab = 5 // Redirect seamlessly to contact tab
            }
        )
    }

    if (activeDetailPub != null) {
        PublicationDetailBottomSheet(
            pub = appLanguage.localizePublication(activeDetailPub),
            onDismissRequest = { activeDetailPubKey = null }
        )
    }

    if (showMembershipSheet) {
        MembershipFormBottomSheet(
            onDismissRequest = { showMembershipSheet = false }
        )
    }

    if (showNotificationSettingsSheet) {
        NotificationSettingsBottomSheet(
            onDismissRequest = { showNotificationSettingsSheet = false },
            onOpenPrivacyPolicy = {
                showNotificationSettingsSheet = false
                showPrivacyPolicyDialog = true
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false }
        )
    }
}

@Composable
fun HeaderSection(
    onJoinClicked: () -> Unit,
    currentThemeName: String,
    onThemeSelected: (String) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenMessages: () -> Unit,
    compact: Boolean = false
) {
    val strings = LocalAppStrings.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.statusBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(
                    top = if (compact) 4.dp else 6.dp,
                    start = 14.dp,
                    end = 14.dp,
                    bottom = if (compact) 6.dp else 10.dp
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (compact) 38.dp else 48.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            shape = CircleShape
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.focom_logo_1779471672893),
                        contentDescription = strings.logoDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FO",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "COM",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "UES ILIAD",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                LanguageModeMenu(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )

                Spacer(modifier = Modifier.width(5.dp))

                ThemeModeMenu(
                    currentThemeName = currentThemeName,
                    onThemeSelected = onThemeSelected
                )

                // Beautiful interactive button to join the union
                Button(
                    onClick = onJoinClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(start = 6.dp, end = 6.dp)
                        .testTag("join_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CardMembership,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        strings.joinButton,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onOpenMessages() }
                        .testTag("open_messages_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Forum,
                        contentDescription = strings.openMessagesDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageModeMenu(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f), CircleShape)
                .clickable { expanded = true }
                .testTag("language_selector_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentLanguage.shortCode,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                softWrap = false
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { lang ->
                val label = when (lang) {
                    AppLanguage.FR -> strings.languageFrenchDefault
                    AppLanguage.EN -> strings.languageEnglish
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            fontWeight = if (lang == currentLanguage) FontWeight.Black else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Text(
                            lang.shortCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (lang == currentLanguage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (lang == currentLanguage) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    onClick = {
                        expanded = false
                        onLanguageSelected(lang)
                    },
                    modifier = Modifier.testTag("language_option_${lang.code}")
                )
            }
        }
    }
}

@Composable
fun ThemeModeMenu(
    currentThemeName: String,
    onThemeSelected: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("System", "Light", "Dark")

    Box {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .testTag("theme_selector_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (currentThemeName) {
                    "Light" -> Icons.Default.LightMode
                    "Dark" -> Icons.Default.DarkMode
                    else -> Icons.Default.BrightnessAuto
                },
                contentDescription = strings.changeThemeDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            strings.themeModeLabel(mode),
                            fontWeight = if (mode == currentThemeName) FontWeight.Black else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                "Light" -> Icons.Default.LightMode
                                "Dark" -> Icons.Default.DarkMode
                                else -> Icons.Default.BrightnessAuto
                            },
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onThemeSelected(mode)
                    }
                )
            }
        }
    }
}

@Composable
fun SyndicatTabContent(
    delegatesList: List<FocomDelegate>,
    onContactClicked: () -> Unit,
    onJoinClicked: () -> Unit,
    onOpenPrivacyPolicy: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val entities = listOf("Free SAS", "ILIAD SA", "Free Réseau", "Free Mobile", "Assunet")
    val missions = listOf(
        Triple(Icons.Default.TrackChanges, strings.missionDefenseTitle, strings.missionDefenseBody),
        Triple(Icons.Default.Public, strings.missionInfoTitle, strings.missionInfoBody),
        Triple(Icons.Default.Handshake, strings.missionBargainingTitle, strings.missionBargainingBody)
    )
    val presenceTerrain = listOf(
        Triple(Icons.Default.Business, strings.fieldEntitiesTitle, strings.fieldEntitiesBody),
        Triple(Icons.Default.Place, strings.fieldRegionalTitle, strings.fieldRegionalBody),
        Triple(Icons.Default.SupportAgent, strings.fieldSupportTitle, strings.fieldSupportBody)
    )
    val history = strings.historyItems

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("syndicat_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF13233A), MaterialTheme.colorScheme.primary.copy(alpha = 0.86f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Text(strings.unionAboutEyebrow, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        strings.unionHeroTitle,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 29.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        strings.unionHeroSubtitle,
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(entities) { entity ->
                            Text(
                                entity,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onJoinClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.joinButton, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = onContactClicked,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.unionContactButton, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SyndicatFigureCard(Icons.Default.Groups, "200+", strings.statMembersLabel, Modifier.weight(1f))
                SyndicatFigureCard(Icons.Default.VerifiedUser, "5", strings.statCompaniesLabel, Modifier.weight(1f))
                SyndicatFigureCard(Icons.AutoMirrored.Filled.Assignment, "CSE", strings.statCseLabel, Modifier.weight(1f))
            }
        }

        items(missions) { (icon, title, text) ->
            SyndicatInfoCard(icon = icon, title = title, text = text, tint = MaterialTheme.colorScheme.secondary)
        }

        item {
            SectionTitle(strings.ourRepsTitle, strings.ourRepsSubtitle)
        }

        items(delegatesList) { delegate ->
            SyndicatDelegateCard(delegate)
        }

        item {
            SectionTitle(strings.fieldPresenceTitle, strings.fieldPresenceSubtitle)
        }

        items(presenceTerrain) { (icon, title, text) ->
            SyndicatInfoCard(icon = icon, title = title, text = text, tint = MaterialTheme.colorScheme.secondary)
        }

        item {
            SectionTitle(strings.ourHistoryTitle, strings.ourHistorySubtitle)
        }

        items(history) { (year, title, text) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        year,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
        }

        if (onOpenPrivacyPolicy != null) {
            item {
                PrivacyPolicyAccessCard(onClick = onOpenPrivacyPolicy)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
    }
}

@Composable
fun SyndicatFigureCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(118.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)
        }
    }
}

@Composable
fun SyndicatInfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, tint: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SyndicatDelegateCard(delegate: FocomDelegate) {
    val photoResId = rememberDelegatePhotoResId(delegate)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF7F1D1D)))),
                contentAlignment = Alignment.Center
            ) {
                if (photoResId != null) {
                    Image(
                        painter = painterResource(id = photoResId),
                        contentDescription = delegate.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(delegate.avatarLetters, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(delegate.name, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(delegate.role, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp)
                Spacer(modifier = Modifier.height(5.dp))
                Text(delegate.entity, color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActualitesTabContent(
    newsList: List<FocomNews>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isOfflineCache: Boolean = false,
    cachedAtMillis: Long? = null,
    searchQuery: String? = null,
    onSearchQueryChanged: ((String) -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onNewsSelected: (FocomNews) -> Unit,
    onJoinClicked: () -> Unit,
    onOpenNotificationSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val (notificationsEnabled, requestNotificationPermission) = rememberNotificationPermissionState()
    var bannerDismissed by rememberSaveable {
        mutableStateOf(FocomPushManager.getPreferencesState(context).promptDismissed)
    }
    var internalSearchQuery by rememberSaveable { mutableStateOf("") }
    val activeSearchQuery = searchQuery ?: internalSearchQuery
    val updateSearchQuery: (String) -> Unit = onSearchQueryChanged ?: { internalSearchQuery = it }
    val filteredNews = remember(newsList, activeSearchQuery) {
        if (activeSearchQuery.isBlank()) newsList
        else newsList.filter {
            it.title.contains(activeSearchQuery, ignoreCase = true) ||
            it.description.contains(activeSearchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("actualites_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Promotional Union membership card at top of Home tab
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                val unionGradient = Brush.horizontalGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                )
                Column(
                    modifier = Modifier
                        .background(unionGradient)
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.promoTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.promoBody,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onJoinClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = strings.promoCta,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // News Search box
        item {
            OutlinedTextField(
                value = activeSearchQuery,
                onValueChange = updateSearchQuery,
                placeholder = { Text(strings.searchNewsPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = if (activeSearchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Section header and cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeSearchQuery.isEmpty()) strings.headlinesTitle else strings.searchResultsTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (onOpenNotificationSettings != null) {
                    IconButton(
                        onClick = onOpenNotificationSettings,
                        modifier = Modifier.testTag("open_notifications_button")
                    ) {
                        Icon(
                            imageVector = if (notificationsEnabled) {
                                Icons.Default.NotificationsActive
                            } else {
                                Icons.Default.NotificationsNone
                            },
                            contentDescription = strings.notificationsButtonDescription,
                            tint = if (notificationsEnabled) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                if (onRefresh != null) {
                    IconButton(onClick = onRefresh) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = strings.refreshDescription,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (onOpenNotificationSettings != null && !notificationsEnabled && !bannerDismissed) {
            item {
                NotificationOptInBanner(
                    onEnableClicked = requestNotificationPermission,
                    onDismissClicked = {
                        bannerDismissed = true
                        FocomPushManager.setPromptDismissed(context, true)
                    }
                )
            }
        }

        if (isOfflineCache && newsList.isNotEmpty() && errorMessage == null) {
            item {
                OfflineArticlesCacheBanner(
                    articleCount = newsList.size,
                    cachedAtMillis = cachedAtMillis,
                    isLoading = isLoading,
                    onRefresh = onRefresh
                )
            }
        }

        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.syncUnavailableTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = errorMessage,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (onRefresh != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onRefresh, enabled = !isLoading) {
                                Text(if (isLoading) strings.syncingLabel else strings.retryLabel, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        if (filteredNews.isEmpty() && isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredNews.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noNewsFound,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredNews, key = { it.id }) { item ->
                NewsCardPremium(item, onClick = { onNewsSelected(item) })
            }
        }
    }
}

@Composable
fun NewsCardPremium(item: FocomNews, onClick: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(item.id) { visible = true }
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "news_card_scale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "news_card_alpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .alpha(cardAlpha)
            .clickable(onClick = onClick)
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(16.dp))
    ) {
        Column {
            if (!item.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                NewsVisualBanner(
                    category = item.category,
                    title = item.title,
                    compact = true
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category pill
                Text(
                    text = strings.translateCategory(item.category).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (item.isHotState) {
                    Text(
                        text = strings.hotBadge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = item.date, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { shareNewsArticle(context, item, strings) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("share_news_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = strings.shareArticleDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.shareArticleButton,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.readMore,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            }
        }
    }
}

@Composable
fun NewsVisualBanner(
    category: String,
    title: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val isNao = category.equals("NAO", ignoreCase = true) || title.contains("NAO", ignoreCase = true)
    val icon = when {
        isNao -> Icons.Default.Campaign
        category.contains("CSE", ignoreCase = true) -> Icons.Default.HowToVote
        category.contains("Accord", ignoreCase = true) -> Icons.Default.Handshake
        category.contains("Vie", ignoreCase = true) -> Icons.Default.Groups
        else -> Icons.AutoMirrored.Filled.Article
    }
    val infinite = rememberInfiniteTransition(label = "news_visual_motion")
    val pulse by infinite.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "news_visual_pulse"
    )
    val sweep by infinite.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "news_visual_sweep"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 112.dp else 178.dp)
            .background(
                Brush.linearGradient(
                    colors = if (isNao) {
                        listOf(Color(0xFF13233A), primary, Color(0xFF7F1D1D))
                    } else {
                        listOf(Color(0xFF13233A), secondary, primary.copy(alpha = 0.92f))
                    }
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = size.minDimension * 0.55f * pulse,
                center = Offset(size.width * 0.82f, size.height * 0.08f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.12f, size.height * 0.88f)
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent)
                ),
                topLeft = Offset(size.width * sweep - size.width * 0.28f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.28f, size.height)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 56.dp else 74.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (compact) 30.dp else 40.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.translateCategory(category).uppercase(),
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isNao) strings.bannerOfficialPosition else "FOCOM UES ILIAD",
                    color = Color.White,
                    fontSize = if (compact) 18.sp else 22.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (compact) 21.sp else 26.sp
                )
            }
        }
    }
}

@Composable
fun VosDroitsTabContent(
    rightsList: List<FocomRight>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onRightSelected: (FocomRight) -> Unit
) {
    val strings = LocalAppStrings.current
    val categories = remember(rightsList) {
        listOf("ALL") + rightsList.map { it.category.trim() }.filter { it.isNotBlank() }.distinct()
    }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }

    val filteredRights = remember(rightsList, searchQuery, selectedCategory) {
        rightsList.filter { right ->
            val matchCategory = selectedCategory == "ALL" || right.category == selectedCategory
            val matchQuery = searchQuery.isBlank() ||
                    right.title.contains(searchQuery, ignoreCase = true) ||
                    right.summary.contains(searchQuery, ignoreCase = true) ||
                    right.detailMarkdown.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.rightsTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (onRefresh != null) {
                    IconButton(onClick = onRefresh) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = strings.syncRightsDescription)
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(strings.searchRightsPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Categories
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    val displayLabel = if (category == "ALL") strings.allCategories else strings.translateCategory(category)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
                            .border(
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = displayLabel,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (filteredRights.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(strings.noRightsFound, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            items(filteredRights) { right ->
                RightPremiumCard(right, onClick = { onRightSelected(right) })
            }
        }
    }
}

@Composable
fun RightPremiumCard(right: FocomRight, onClick: () -> Unit) {
    val strings = LocalAppStrings.current
    // Dynamic matching icon for the category
    val icon = when (right.category) {
        "Télétravail" -> Icons.Default.Laptop
        "Congés" -> Icons.Default.DateRange
        "Santé" -> Icons.Default.Favorite
        "Rémunération" -> Icons.Default.Payments
        "Législation" -> Icons.Default.Gavel
        "Sécurité" -> Icons.Default.Security
        else -> Icons.Default.Info
    }

    val isDark = isSystemInDarkTheme()
    val iconBgColor = when (right.category) {
        "Télétravail" -> if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE)
        "Congés" -> if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)
        "Santé" -> if (isDark) Color(0xFF701A75) else Color(0xFFFCE7F3)
        "Rémunération" -> if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
        "Législation" -> if (isDark) Color(0xFF1E2937) else Color(0xFFF1F5F9)
        "Sécurité" -> if (isDark) Color(0xFF581C87) else Color(0xFFF3E8FF)
        else -> if (isDark) Color(0xFF042F2E) else Color(0xFFE2F4F5)
    }

    val iconTint = when (right.category) {
        "Télétravail" -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        "Congés" -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        "Santé" -> if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
        "Rémunération" -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
        "Législation" -> if (isDark) Color(0xFF9CA3AF) else Color(0xFF475569)
        "Sécurité" -> if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
        else -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF148A8F)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.translateCategory(right.category).uppercase(),
                    color = iconTint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = right.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = right.summary,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun OfflineArticlesCacheBanner(
    articleCount: Int,
    cachedAtMillis: Long?,
    isLoading: Boolean,
    onRefresh: (() -> Unit)?
) {
    val strings = LocalAppStrings.current
    val appLanguage = LocalAppLanguage.current
    val formattedDate = remember(cachedAtMillis, appLanguage) {
        cachedAtMillis?.let {
            runCatching {
                SimpleDateFormat("dd/MM HH:mm", appLanguage.locale).format(Date(it))
            }.getOrNull()
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("offline_cache_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.OfflinePin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.offlineCacheTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.offlineCacheSubtitle(articleCount, formattedDate),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onRefresh != null) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onRefresh, enabled = !isLoading) {
                    Text(if (isLoading) strings.syncingLabel else strings.retryLabel, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PublicationsTabContent(
    publicationsList: List<FocomPublication>,
    isLoading: Boolean,
    errorMessage: String? = null,
    isOfflineCache: Boolean = false,
    cachedAtMillis: Long? = null,
    onRefresh: () -> Unit,
    onPubSelected: (FocomPublication) -> Unit
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.publicationsTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.publicationsSubtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = strings.refreshDescription)
                    }
                }
            }
        }

        if (isOfflineCache && publicationsList.isNotEmpty() && errorMessage == null) {
            item {
                OfflineArticlesCacheBanner(
                    articleCount = publicationsList.size,
                    cachedAtMillis = cachedAtMillis,
                    isLoading = isLoading,
                    onRefresh = onRefresh
                )
            }
        }

        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.syncUnavailableTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = errorMessage,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onRefresh, enabled = !isLoading) {
                            Text(if (isLoading) strings.syncingLabel else strings.retryLabel, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        items(publicationsList, key = { it.id }) { pub ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPubSelected(pub) }
                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pub.thumbnailUrl != null) {
                        AsyncImage(
                            model = pub.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Article,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.translateCategory(pub.type).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pub.title,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.datePrefix + pub.date,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (pub.excerpt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pub.excerpt,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = strings.readDescription,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReclamationsTabContent(
    onSendRequest: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val categories = strings.requestCategories
    val entities = listOf("Free Réseau", "Free Mobile", "Iliad", "Free SAS", "Assunet", "ROF", strings.requestOtherOption)
    val priorities = strings.requestPriorities

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var entityIndex by rememberSaveable { mutableIntStateOf(0) }
    var priorityIndex by rememberSaveable { mutableIntStateOf(0) }

    val selectedCategory = categories.getOrElse(categoryIndex) { categories.first() }
    val selectedEntity = entities.getOrElse(entityIndex) { entities.first() }
    val selectedPriority = priorities.getOrElse(priorityIndex) { priorities.first() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0F766E),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderOpen, null, tint = Color.White, modifier = Modifier.size(25.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.requestsHeroTitle, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                            Text(
                                strings.requestsHeroSubtitle,
                                color = Color.White.copy(alpha = 0.84f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                RequestStepCard(Icons.Default.EditNote, "1", strings.stepDescribe, Modifier.weight(1f))
                RequestStepCard(Icons.Default.Shield, "2", strings.stepAnalyze, Modifier.weight(1f))
                RequestStepCard(Icons.Default.Handshake, "3", strings.stepAct, Modifier.weight(1f))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.newRequestTitle, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        strings.newRequestSubtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(strings.requestSubjectLabel) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Subject, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(strings.requestDescriptionLabel) },
                        leadingIcon = { Icon(Icons.Default.Description, null) },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    RequestChipSelector(
                        title = strings.requestCategoryTitle,
                        options = categories,
                        selected = selectedCategory,
                        onSelected = { option ->
                            val idx = categories.indexOf(option)
                            if (idx >= 0) categoryIndex = idx
                        }
                    )

                    RequestChipSelector(
                        title = strings.requestEntityTitle,
                        options = entities,
                        selected = selectedEntity,
                        onSelected = { option ->
                            val idx = entities.indexOf(option)
                            if (idx >= 0) entityIndex = idx
                        }
                    )

                    RequestChipSelector(
                        title = strings.requestPriorityTitle,
                        options = priorities,
                        selected = selectedPriority,
                        onSelected = { option ->
                            val idx = priorities.indexOf(option)
                            if (idx >= 0) priorityIndex = idx
                        }
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.nameLabel) },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(strings.emailLabel) },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(strings.phoneLabel) },
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            if (title.isBlank() || description.isBlank()) {
                                Toast.makeText(context, strings.requestMissingBodyToast, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (name.isBlank() || email.isBlank()) {
                                Toast.makeText(context, strings.requestMissingIdentityToast, Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val subject = "${strings.requestSubjectPrefix} $selectedCategory - $title"
                            val body = buildString {
                                appendLine(description.trim())
                                appendLine()
                                appendLine("${strings.requestCategoryTitle} : $selectedCategory")
                                appendLine("${strings.requestEntityTitle} : $selectedEntity")
                                appendLine("${strings.requestPriorityTitle} : $selectedPriority")
                            }
                            onSendRequest(name, email, phone, subject, body)
                            title = ""
                            description = ""
                            priorityIndex = 0
                            Toast.makeText(context, strings.requestSentToast, Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.submitRequestButton, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        strings.requestAttachmentsNote,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RequestStepCard(
    icon: ImageVector,
    number: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(number, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun RequestChipSelector(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column {
        Text(
            title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelected(option) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        option,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SimulateurTabContent() {
    val strings = LocalAppStrings.current
    val appLanguage = LocalAppLanguage.current
    var salaryInput by rememberSaveable { mutableStateOf("2000") }
    var seniorityYears by rememberSaveable { mutableFloatStateOf(5f) }

    val seniorityInt = seniorityYears.toInt()
    val salaryDouble = salaryInput.toDoubleOrNull() ?: 0.0

    // Calculations based solely on CCNT Télécoms
    val bonusPercentage = SimulatorLogic.calculateBonusPercentage(seniorityInt)
    val calculationBase = salaryDouble

    val monthlyBonus = (calculationBase * bonusPercentage) / 100.0
    val annualBonus = monthlyBonus * 12.0

    val extraLeaveDays = SimulatorLogic.calculateExtraLeaveDays(seniorityInt)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = strings.simulatorTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.simulatorSubtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Salary Input
        item {
            Column {
                Text(
                    text = strings.simulatorSalaryPrompt,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = salaryInput,
                    onValueChange = { salaryInput = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text(strings.simulatorSalaryPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    trailingIcon = { Text("€", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Seniority Slider
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.simulatorSeniorityPrompt,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$seniorityInt ${strings.yearsShort}",
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = seniorityYears,
                        onValueChange = { seniorityYears = it },
                        valueRange = 0f..25f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        strings.yearsLabels.forEach { label ->
                            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Result Title
        item {
            Text(
                text = strings.simulatorResultsTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Prime d'ancienneté result card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(16.dp))
            ) {
                val cardGradient = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                    )
                )
                Column(
                    modifier = Modifier
                        .background(cardGradient)
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ThumbUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.simulatorBonusHeader,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${strings.simulatorAppliedRatePrefix}$bonusPercentage%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (bonusPercentage > 0) "+ $bonusPercentage%" else "0%",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Monthly estimate
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.simulatorMonthlyGain,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(appLanguage.locale, "%.2f ${strings.perMonthSuffix}", monthlyBonus),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Annual estimate
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.simulatorAnnualGain,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(appLanguage.locale, "%.2f ${strings.perYearSuffix}", annualBonus),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Paid Leave Days result card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.simulatorExtraLeaveHeader,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (extraLeaveDays > 0) strings.simulatorExtraLeaveValue(extraLeaveDays) else strings.simulatorExtraLeaveNone,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Militant advice box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.simulatorWarningTitle,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.simulatorWarningBody,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsTabContent(
    delegatesList: List<FocomDelegate>,
    contactMessages: List<ContactMessage>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSendMessage: (String, String, String, String, String) -> Unit,
    onOpenPrivacyPolicy: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    var contactName by rememberSaveable { mutableStateOf("") }
    var contactEmail by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var contactSubject by rememberSaveable { mutableStateOf(strings.defaultSubject) }
    var contactMessage by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(strings.language) {
        if (contactSubject.isBlank() ||
            contactSubject == "Demande d'information" ||
            contactSubject == "Information request"
        ) {
            contactSubject = strings.defaultSubject
        }
    }

    val filteredDelegates = remember(delegatesList, searchQuery) {
        delegatesList.filter { delegate ->
            val matchQuery = searchQuery.isBlank() ||
                    delegate.name.contains(searchQuery, ignoreCase = true) ||
                    delegate.role.contains(searchQuery, ignoreCase = true) ||
                    delegate.region.contains(searchQuery, ignoreCase = true)
            matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contacts_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ContactSyndicatForm(
                name = contactName,
                onNameChanged = { contactName = it },
                email = contactEmail,
                onEmailChanged = { contactEmail = it },
                phone = contactPhone,
                onPhoneChanged = { contactPhone = it },
                subject = contactSubject,
                onSubjectChanged = { contactSubject = it },
                message = contactMessage,
                onMessageChanged = { contactMessage = it },
                onSendClicked = {
                    if (contactMessage.isBlank()) {
                        Toast.makeText(context, strings.addMessageToast, Toast.LENGTH_SHORT).show()
                        return@ContactSyndicatForm
                    }
                    if (contactName.isBlank() || contactEmail.isBlank()) {
                        Toast.makeText(context, strings.addNameEmailToast, Toast.LENGTH_SHORT).show()
                        return@ContactSyndicatForm
                    }
                    onSendMessage(contactName, contactEmail, contactPhone, contactSubject, contactMessage)
                    contactSubject = strings.defaultSubject
                    contactMessage = ""
                }
            )
        }

        if (onOpenPrivacyPolicy != null) {
            item {
                TextButton(
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("contact_privacy_policy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.privacyPolicyLinkLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            InternalMessageHistory(messages = contactMessages)
        }

        item {
            Text(strings.delegatesTitle, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        }

        // Contact Search
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(strings.searchDelegatePlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filteredDelegates.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(strings.noDelegateFound, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            items(filteredDelegates) { delegate ->
                DelegatePremiumCard(
                    delegate = delegate,
                    onCallClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${delegate.phone}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, strings.cannotPlaceCallToast, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEmailClick = {
                        contactSubject = "${strings.preparedSubjectPrefix} - ${delegate.entity}"
                        contactMessage = "${strings.preparedGreetingPrefix} ${delegate.name},\n\n"
                        Toast.makeText(context, strings.messagePreparedToast, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun InternalMessageHistory(messages: List<ContactMessage>) {
    val strings = LocalAppStrings.current
    val appLanguage = LocalAppLanguage.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inbox, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.inboxTitle, fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
                Text("${messages.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (messages.isEmpty()) {
                Text(
                    strings.inboxEmpty,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            } else {
                messages.take(3).forEach { message ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                message.subject.ifBlank { strings.defaultSubject },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                message.createdAt.toShortDate(appLanguage.locale),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            message.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val isDelivered = message.status.equals("Envoyé", ignoreCase = true)
                        Text(
                            text = strings.translateStatus(message.status),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDelivered) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    (if (isDelivered) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                                        .copy(alpha = 0.14f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun Long.toShortDate(locale: Locale = Locale.FRANCE): String {
    val pattern = if (locale.language == "en") "MM/dd HH:mm" else "dd/MM HH:mm"
    return SimpleDateFormat(pattern, locale).format(Date(this))
}

@Composable
fun ContactSyndicatForm(
    name: String,
    onNameChanged: (String) -> Unit,
    email: String,
    onEmailChanged: (String) -> Unit,
    phone: String,
    onPhoneChanged: (String) -> Unit,
    subject: String,
    onSubjectChanged: (String) -> Unit,
    message: String,
    onMessageChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MarkEmailUnread, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.contactUnionTitle, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text(strings.contactUnionSubtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                label = { Text(strings.nameLabel) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChanged,
                    label = { Text(strings.emailLabel) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChanged,
                    label = { Text(strings.phoneLabel) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = subject,
                onValueChange = onSubjectChanged,
                label = { Text(strings.subjectLabel) },
                singleLine = true,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Subject, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChanged,
                label = { Text(strings.messageLabel) },
                minLines = 4,
                leadingIcon = { Icon(Icons.Default.EditNote, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSendClicked,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.sendMessageButton, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DelegatePremiumCard(
    delegate: FocomDelegate,
    onCallClick: () -> Unit,
    onEmailClick: () -> Unit
) {
    val photoResId = rememberDelegatePhotoResId(delegate)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant Avatar circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (photoResId != null) {
                    Image(
                        painter = painterResource(id = photoResId),
                        contentDescription = delegate.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = delegate.avatarLetters,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = delegate.name,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                Text(
                    text = delegate.role,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${delegate.entity} — ${delegate.region}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Call/Mail clickable buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .clickable(onClick = onCallClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        .clickable(onClick = onEmailClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DETAIL BOTTOM SHEETS IMPLEMENTATIONS WITH MODERN EDITORIAL UX
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailBottomSheet(
    news: FocomNews,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            if (!news.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = news.imageUrl,
                    contentDescription = news.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
            } else {
                NewsVisualBanner(
                    category = news.category,
                    title = news.title,
                    compact = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.translateCategory(news.category).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "•", color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = news.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { shareNewsArticle(context, news, strings) },
                    modifier = Modifier.testTag("detail_header_share_article_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = strings.shareArticleDescription,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = news.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ArticleRichContent(news.content)
            Spacer(modifier = Modifier.height(24.dp))
            if (!news.webUrl.isNullOrBlank()) {
                OutlinedButton(
                    onClick = { openExternalPdf(context, news.webUrl, strings.openDocFailed) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.viewOnOfficialSite)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            FilledTonalButton(
                onClick = { shareNewsArticle(context, news, strings) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("detail_share_article_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.shareArticleButton)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(strings.closeArticle, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ArticleRichContent(content: String) {
    val blocks = remember(content) {
        content
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEachIndexed { index, block ->
            var visible by remember(block) { mutableStateOf(false) }
            LaunchedEffect(block) {
                delay((index.coerceAtMost(10) * 28).toLong())
                visible = true
            }
            val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                label = "article_line_alpha"
            )
            val offset by animateFloatAsState(
                targetValue = if (visible) 0f else 10f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                label = "article_line_offset"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .offset(y = offset.dp)
            ) {
                when {
                    block.isArticleHeading() -> ArticleHeading(block)
                    block.startsWith("-") -> ArticleBullet(block.removePrefix("-").trim())
                    block.firstOrNull()?.isDigit() == true && block.contains(".") -> ArticleNumberedItem(block)
                    block.contains("FO refuse", ignoreCase = true) ||
                        block.contains("FO refuses", ignoreCase = true) ||
                        block.contains("ne signera pas", ignoreCase = true) ||
                        block.contains("will not sign", ignoreCase = true) ||
                        block.startsWith("CONCLUSION", ignoreCase = true) -> ArticleCallout(block)
                    else -> ArticleParagraph(block)
                }
            }
        }
    }
}

private fun String.isArticleHeading(): Boolean {
    val letters = filter { it.isLetter() }
    return length <= 42 && letters.isNotEmpty() && letters.all { it.isUpperCase() }
}

@Composable
fun ArticleHeading(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun ArticleParagraph(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp
    )
}

@Composable
fun ArticleBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ArticleNumberedItem(text: String) {
    val number = text.substringBefore(".").trim()
    val body = text.substringAfter(".").trim()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = body,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ArticleCallout(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RightDetailBottomSheet(
    right: FocomRight,
    onDismissRequest: () -> Unit,
    onContactDelegate: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Text(
                text = strings.translateCategory(right.category).uppercase(),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = right.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Simulated beautiful Markdown Renderer in Jetpack Compose
            Text(
                text = strings.summaryOfProvisions,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            MarkdownContent(
                markdown = right.detailMarkdown,
                accentColor = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Custom advice highlighted box
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = strings.focomAdviceHeader,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = right.focomAdvice,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bridge to delegates button
            Button(
                onClick = { onContactDelegate(right.category) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContactPhone, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.contactDelegateForTopic, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicationDetailBottomSheet(
    pub: FocomPublication,
    onDismissRequest: () -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadFinished by remember { mutableStateOf(false) }
    var pageZoom by rememberSaveable(pub.id) { mutableFloatStateOf(1f) }
    val context = LocalContext.current

    LaunchedEffect(pub) {
        downloadProgress = 0f
        downloadFinished = pub.content.isNotBlank()
        if (pub.content.isNotBlank()) return@LaunchedEffect
        // Realistic progress simulation loading
        while (downloadProgress < 1.0f) {
            delay(80)
            downloadProgress += 0.08f
        }
        downloadProgress = 1.0f
        downloadFinished = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pub.title,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!downloadFinished) {
                // Circular loading progress
                Column(
                    modifier = Modifier.padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val progressText = when {
                        downloadProgress < 0.3f -> strings.loadingConnecting
                        downloadProgress < 0.7f -> strings.loadingDownloading
                        downloadProgress < 0.95f -> strings.loadingVerifying
                        else -> strings.loadingOpening
                    }
                    Text(progressText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                if (pub.content.isNotBlank()) {
                    if (pub.thumbnailUrl != null) {
                        AsyncImage(
                            model = pub.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.translateCategory(pub.type).uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(pub.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (pub.excerpt.isNotBlank()) {
                        ArticleCallout(pub.excerpt)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    ArticleRichContent(pub.content)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { openExternalPdf(context, pub.pdfUrl, strings.openDocFailed) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.openOnSiteButton, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    return@Column
                }

                // PDF Reader mock controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (pageZoom > 0.6f) pageZoom -= 0.1f }) {
                            Icon(Icons.Default.ZoomOut, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("${(pageZoom * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { if (pageZoom < 2f) pageZoom += 0.1f }) {
                            Icon(Icons.Default.ZoomIn, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Text(strings.pageLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, pub.title)
                            putExtra(Intent.EXTRA_TEXT, "${strings.shareDocumentTextPrefix} ${pub.pdfUrl}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, strings.shareDocumentChooserTitle))
                    }) {
                        Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { openExternalPdf(context, pub.pdfUrl, strings.openDocFailed) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.openPdfButton,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful simulated page layout
                val isDark = isSystemInDarkTheme()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(pageZoom)
                        .background(if (isDark) Color(0xFF1F2937) else Color.White, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(8.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Logo mock inside the page header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("FOCOM ILIAD", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(strings.pdfMockDate, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Box(Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.primary))
                        
                        Text(strings.pdfMockEditorialTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Text(
                            strings.pdfMockEditorialP1,
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp, textAlign = TextAlign.Justify
                        )
                        Text(
                            strings.pdfMockEditorialP2,
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp, textAlign = TextAlign.Justify
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(strings.pdfMockClaimsHeader, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        strings.pdfMockClaims.forEach { claim ->
                            Row {
                                Text("•", color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(claim, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// UNION ONLINE MEMBERSHIP & STEPPER MODULE WITH INTERACTIVE CARD
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipFormBottomSheet(
    onDismissRequest: () -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    // Stepper Input States
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var jobTitle by rememberSaveable { mutableStateOf("") }
    var selectedMotivationsCsv by rememberSaveable { mutableStateOf("") }
    val selectedMotivationIndices = remember(selectedMotivationsCsv) {
        selectedMotivationsCsv.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }
    var agreementApproved by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            // Stepper top indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicator(stepNumber = 1, currentStep = currentStep, label = strings.stepProfile)
                Box(Modifier.width(30.dp).height(2.dp).background(if (currentStep >= 2) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant))
                StepIndicator(stepNumber = 2, currentStep = currentStep, label = strings.stepPro)
                Box(Modifier.width(30.dp).height(2.dp).background(if (currentStep >= 3) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant))
                StepIndicator(stepNumber = 3, currentStep = currentStep, label = strings.stepCommitment)
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (currentStep) {
                1 -> {
                    // Step 1: Civil Status
                    Text(strings.membershipStep1Title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text(strings.firstNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text(strings.lastNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(strings.emailAddressLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(strings.mobilePhoneLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = { currentStep = 2 },
                        enabled = firstName.isNotBlank() && lastName.isNotBlank() && email.contains("@"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.continueButton, color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
                2 -> {
                    // Step 2: Pro Details
                    Text(strings.membershipStep2Title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text(strings.jobTitleLabel) },
                        singleLine = true,
                        placeholder = { Text(strings.jobTitlePlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { currentStep = 1 },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(strings.backButton)
                        }
                        Button(
                            onClick = { currentStep = 3 },
                            enabled = jobTitle.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.continueButton, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
                3 -> {
                    // Step 3: Motivations and simulated tax dues
                    Text(strings.membershipStep3Title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(strings.motivationsPrompt, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val motivations = strings.motivationsList
                    
                    motivations.forEachIndexed { index, mot ->
                        val contains = selectedMotivationIndices.contains(index)
                        val toggleMotivation = {
                            val updated = if (contains) {
                                selectedMotivationIndices - index
                            } else {
                                selectedMotivationIndices + index
                            }
                            selectedMotivationsCsv = updated.sorted().joinToString(",")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleMotivation() }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = contains,
                                onCheckedChange = { toggleMotivation() },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                            )
                            Text(mot, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real life French tax benefits highlight card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(strings.solidarityFundingTitle, fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.solidarityFundingBody,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { agreementApproved = !agreementApproved }
                    ) {
                        Checkbox(
                            checked = agreementApproved,
                            onCheckedChange = { agreementApproved = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(strings.membershipAgreementLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { currentStep = 2 },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(strings.backButton)
                        }
                        Button(
                            onClick = { currentStep = 4 },
                            enabled = agreementApproved,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.confirmMembershipButton, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                4 -> {
                    // Confetti and Success digital Membership Card
                    MembershipSuccessView(
                        firstName = firstName,
                        lastName = lastName,
                        onFinished = onDismissRequest
                    )
                }
            }
        }
    }
}

@Composable
fun StepIndicator(stepNumber: Int, currentStep: Int, label: String) {
    val active = currentStep >= stepNumber
    val isCurrent = currentStep == stepNumber
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrent) MaterialTheme.colorScheme.primary
                    else if (active) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (active && !isCurrent) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = stepNumber.toString(), 
                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label, 
            fontSize = 9.sp, 
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Custom animated stars representing confetti falling down
@Composable
fun MembershipSuccessView(
    firstName: String,
    lastName: String,
    onFinished: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var animateTrigger by remember { mutableStateOf(false) }
    
    val scaleCard by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card"
    )

    val confettiY by animateFloatAsState(
        targetValue = if (animateTrigger) 800f else 0f,
        animationSpec = tween(durationMillis = 1800, easing = LinearEasing),
        label = "confetti"
    )

    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(260.dp)
        ) {
            // Virtual Card graphic in premium Gold/Crimson glass gradient
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(320.dp)
                    .height(180.dp)
                    .scale(scaleCard)
                    .border(BorderStroke(1.dp, Color(0xFFFFF0D4).copy(alpha = 0.6f)), RoundedCornerShape(16.dp))
                    .align(Alignment.Center)
            ) {
                val cardGradient = Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF0B0F19))
                )
                Column(
                    modifier = Modifier
                        .background(cardGradient)
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FO",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "COM",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = strings.memberCardLabel,
                            color = Color(0xFFFFD700),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "$firstName $lastName",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.memberCardStatusActive,
                            color = Color(0xFF2ECC71),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(Color(0xFF2ECC71).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        val randomNum = rememberSaveable { Random.nextInt(1000, 9999) }
                        Text(
                            text = "ID : FO-2026-$randomNum",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            // Confetti Canvas shower simulator
            Canvas(modifier = Modifier.fillMaxSize().alpha(if (confettiY < 750f) 1f else 0f)) {
                val confettiColors = listOf(primaryColor, secondaryColor, Color(0xFFFFD700), Color.White)
                
                // Draw 24 animated falling rectangles representing paper confetti
                for (i in 0..24) {
                    val color = confettiColors[i % confettiColors.size]
                    val randomX = (i * 20) % size.width
                    val randomOffset = (i * 45) % 150
                    val floatY = confettiY + randomOffset
                    
                    if (floatY < size.height) {
                        drawRect(
                            color = color,
                            topLeft = androidx.compose.ui.geometry.Offset(randomX, floatY),
                            size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.memberWelcomeTitle,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = strings.memberWelcomeBody,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                Toast.makeText(context, strings.memberWelcomeToast, Toast.LENGTH_LONG).show()
                onFinished()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.finishAndCloseButton, color = MaterialTheme.colorScheme.onSecondary)
        }
    }
}
