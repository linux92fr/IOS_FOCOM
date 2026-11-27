package com.example.data.repository

import com.example.data.local.SavedRepoDao
import com.example.data.local.ContactMessageDao
import com.example.data.local.ArticleCacheDao
import com.example.data.local.CachedArticle
import com.example.data.local.FlashBannerCacheDao
import com.example.data.local.CachedFlashBanner
import com.example.data.local.CachedFocomContent
import com.example.data.local.FocomContentCacheDao
import com.example.data.model.ContactMessage
import com.example.data.model.GitHubCommitContainer
import com.example.data.model.GitHubContent
import com.example.data.model.GitHubIssue
import com.example.data.model.GitHubRepo
import com.example.data.model.FocomContent
import com.example.data.model.FocomPublication
import com.example.data.model.FocomFlashBanner
import com.example.data.model.SavedRepo
import com.example.data.remote.GitHubService
import com.example.data.remote.SupabaseContactMessage
import com.example.data.remote.SupabaseService
import java.time.Instant
import java.time.OffsetDateTime
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class GitHubRepository(
    private val savedRepoDao: SavedRepoDao,
    private val contactMessageDao: ContactMessageDao,
    private val articleCacheDao: ArticleCacheDao,
    private val flashBannerCacheDao: FlashBannerCacheDao,
    private val gitHubService: GitHubService,
    private val supabaseService: SupabaseService? = null,
    private val supabasePublishableKey: String = "",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val focomContentCacheDao: FocomContentCacheDao? = null
) {
    private val focomMoshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }
    // Flow of locally saved/previously viewed repositories and cached articles
    val allSavedRepos: Flow<List<SavedRepo>> = savedRepoDao.getAllSavedRepos()
    val allContactMessages: Flow<List<ContactMessage>> = contactMessageDao.getAllMessages()

    data class ArticlesSyncResult(
        val articles: List<FocomPublication>,
        val isFromCache: Boolean,
        val cachedAt: Long?
    )

    fun observeCachedArticles(): Flow<List<FocomPublication>> =
        articleCacheDao.observeAll()
            .map { cached -> cached.map { it.toPublication() } }
            .flowOn(ioDispatcher)

    fun observeArticlesCachedAt(): Flow<Long?> =
        articleCacheDao.observeLastCachedAt()
            .flowOn(ioDispatcher)

    fun observeCachedFocomContent(): Flow<FocomContent?> {
        val dao = focomContentCacheDao ?: return emptyFlow()
        return dao.observeById()
            .map { entity ->
                entity?.let {
                    runCatching {
                        focomMoshi.adapter(FocomContent::class.java).fromJson(it.rawJson)
                    }.getOrNull()
                }
            }
            .flowOn(ioDispatcher)
    }

    suspend fun getCachedArticles(): List<FocomPublication> = withContext(ioDispatcher) {
        articleCacheDao.getAll().map { it.toPublication() }
    }

    suspend fun getCachedArticlesTimestamp(): Long? = withContext(ioDispatcher) {
        articleCacheDao.getLastCachedAt()
    }

    suspend fun getCachedArticleById(id: String): FocomPublication? = withContext(ioDispatcher) {
        articleCacheDao.getById(id)?.toPublication()
    }

    suspend fun getCachedFocomContent(): FocomContent? = withContext(ioDispatcher) {
        focomContentCacheDao?.getById()?.let { cachedEntity ->
            runCatching {
                focomMoshi.adapter(FocomContent::class.java).fromJson(cachedEntity.rawJson)
            }.getOrNull()
        }
    }

    suspend fun saveRepo(repo: SavedRepo) {
        savedRepoDao.insertRepo(repo)
    }

    suspend fun removeSavedRepo(fullName: String) {
        savedRepoDao.deleteRepo(fullName)
    }

    suspend fun getSavedRepo(fullName: String): SavedRepo? {
        return savedRepoDao.getRepo(fullName)
    }

    enum class ContactSendResult {
        SENT_REMOTE,
        SAVED_LOCAL_ONLY,
        FAILED_SAVED_LOCAL
    }

    suspend fun sendContactMessage(message: ContactMessage): ContactSendResult = withContext(ioDispatcher) {
        val canSyncRemote = supabaseService != null &&
            supabasePublishableKey.isNotBlank() &&
            message.name.isNotBlank() &&
            message.email.isNotBlank() &&
            message.subject.isNotBlank() &&
            message.message.isNotBlank()

        val initialStatus = if (canSyncRemote) "En attente" else "Enregistré localement"
        val insertedId = contactMessageDao.insertMessage(message.copy(status = initialStatus))

        if (!canSyncRemote) {
            return@withContext ContactSendResult.SAVED_LOCAL_ONLY
        }

        val sent = postMessageToSupabase(message)
        if (sent) {
            contactMessageDao.updateMessageStatus(insertedId, "Envoyé")
            ContactSendResult.SENT_REMOTE
        } else {
            contactMessageDao.updateMessageStatus(insertedId, "En attente réseau")
            ContactSendResult.FAILED_SAVED_LOCAL
        }
    }

    suspend fun retryPendingContactMessages(): Int = withContext(ioDispatcher) {
        if (supabaseService == null || supabasePublishableKey.isBlank()) return@withContext 0
        val pending = contactMessageDao.getPendingMessages()
        var syncedCount = 0
        for (message in pending) {
            if (message.name.isBlank() || message.email.isBlank() || message.subject.isBlank() || message.message.isBlank()) {
                continue
            }
            if (postMessageToSupabase(message)) {
                contactMessageDao.updateMessageStatus(message.id, "Envoyé")
                syncedCount++
            }
        }
        syncedCount
    }

    private suspend fun postMessageToSupabase(message: ContactMessage): Boolean {
        val service = supabaseService ?: return false
        val apiKey = supabasePublishableKey.trim()
        if (apiKey.isBlank()) return false

        val supabaseMessage = buildString {
            append(message.message.trim())
            if (message.phone.isNotBlank()) {
                appendLine()
                appendLine()
                append("Téléphone : ${message.phone.trim()}")
            }
        }.take(2000)

        return runCatching {
            val response = service.insertContactMessage(
                apiKey = apiKey,
                authorization = authorizationHeaderOrNull(),
                message = SupabaseContactMessage(
                    name = message.name.trim().take(100),
                    email = message.email.trim().take(255),
                    subject = message.subject.trim().take(200),
                    message = supabaseMessage,
                    category = "Application Android"
                )
            )
            response.isSuccessful
        }.getOrDefault(false)
    }

    private fun authorizationHeaderOrNull(): String? {
        val key = supabasePublishableKey.trim()
        // Only legacy JWT anon keys (eyJ...) should be sent as Bearer tokens;
        // sb_publishable_... keys must only be sent in the apikey header.
        return if (key.startsWith("eyJ")) "Bearer $key" else null
    }

    // Remote functions
    suspend fun fetchRepo(owner: String, repo: String, token: String? = null): GitHubRepo = withContext(ioDispatcher) {
        gitHubService.getRepo(owner, repo, token = token)
    }

    suspend fun fetchContents(owner: String, repo: String, path: String = "", token: String? = null): List<GitHubContent> = withContext(ioDispatcher) {
        if (path.isEmpty()) {
            gitHubService.getRootContents(owner, repo, token = token)
        } else {
            gitHubService.getPathContents(owner, repo, path, token = token)
        }
    }

    suspend fun fetchCommits(owner: String, repo: String, token: String? = null): List<GitHubCommitContainer> = withContext(ioDispatcher) {
        gitHubService.getCommits(owner, repo, token = token)
    }

    suspend fun fetchIssues(owner: String, repo: String, token: String? = null): List<GitHubIssue> = withContext(ioDispatcher) {
        gitHubService.getIssues(owner, repo, token = token)
    }

    suspend fun fetchFileContent(downloadUrl: String, token: String? = null): String = withContext(ioDispatcher) {
        val response = gitHubService.getRawText(downloadUrl, token = token)
        if (response.isSuccessful) {
            response.body()?.string() ?: ""
        } else {
            throw IOException("Failed to load file content: ${response.code()} ${response.message()}")
        }
    }

    suspend fun fetchFocomContent(rawJsonUrl: String): FocomContent = withContext(ioDispatcher) {
        val adapter = focomMoshi.adapter(FocomContent::class.java)
        try {
            val json = fetchFileContent(rawJsonUrl)
            val parsed = adapter.fromJson(json) ?: throw IOException("Empty FOCOM content payload")
            focomContentCacheDao?.upsert(
                CachedFocomContent(
                    rawJson = json,
                    sourceUrl = rawJsonUrl,
                    cachedAt = System.currentTimeMillis()
                )
            )
            parsed
        } catch (error: Exception) {
            val cached = focomContentCacheDao?.getById()?.let { cachedEntity ->
                runCatching { adapter.fromJson(cachedEntity.rawJson) }.getOrNull()
            }
            cached ?: throw error
        }
    }

    suspend fun fetchFocomContentFromRepo(
        owner: String,
        repo: String,
        preferredBranch: String? = null,
        token: String? = null
    ): FocomContent = withContext(ioDispatcher) {
        val adapter = focomMoshi.adapter(FocomContent::class.java)
        val branches = listOfNotNull(
            preferredBranch?.trim()?.takeIf { it.isNotBlank() },
            "main",
            "master"
        ).distinct()
        val candidatePaths = listOf("data/focom-content.json", "focom-content.json")

        var lastError: Exception? = null
        for (branch in branches) {
            for (path in candidatePaths) {
                val url = "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
                try {
                    val json = fetchFileContent(url, token = token)
                    val parsed = adapter.fromJson(json) ?: throw IOException("Empty FOCOM content payload")
                    focomContentCacheDao?.upsert(
                        CachedFocomContent(
                            rawJson = json,
                            sourceUrl = url,
                            cachedAt = System.currentTimeMillis()
                        )
                    )
                    return@withContext parsed
                } catch (error: Exception) {
                    lastError = error
                }
            }
        }

        val cached = focomContentCacheDao?.getById()?.let { cachedEntity ->
            runCatching { adapter.fromJson(cachedEntity.rawJson) }.getOrNull()
        }
        cached ?: throw (lastError ?: IOException("Impossible de synchroniser le dépôt GitHub $owner/$repo."))
    }

    suspend fun getCachedFocomContentTimestamp(): Long? = withContext(ioDispatcher) {
        focomContentCacheDao?.getById()?.cachedAt
    }

    suspend fun fetchActiveFlashBanners(): List<FocomFlashBanner> = withContext(ioDispatcher) {
        val now = Instant.now()
        val service = supabaseService
        val apiKey = supabasePublishableKey.trim()
        if (service == null || apiKey.isBlank()) {
            return@withContext flashBannerCacheDao.getAll()
                .map { it.toBanner() }
                .filter { it.isActiveAt(now) }
        }

        try {
            val nowValue = now.toString()
            val remoteBanners = service.getActiveFlashBanners(
                apiKey = apiKey,
                authorization = authorizationHeaderOrNull(),
                startsBeforeOrAt = "lte.$nowValue",
                endsAfterOrAt = "gte.$nowValue"
            ).filter { it.isActive != false }.map { banner ->
                FocomFlashBanner(
                    id = banner.id,
                    message = banner.message,
                    type = banner.type,
                    link = banner.link,
                    startDate = banner.startDate,
                    endDate = banner.endDate
                )
            }
            flashBannerCacheDao.clear()
            flashBannerCacheDao.replaceAll(
                remoteBanners.mapIndexed { index, banner -> CachedFlashBanner.from(banner, index) }
            )
            remoteBanners
        } catch (error: Exception) {
            val cached = flashBannerCacheDao.getAll()
                .map { it.toBanner() }
                .filter { it.isActiveAt(now) }
            if (cached.isNotEmpty()) cached else throw error
        }
    }

    suspend fun fetchPublishedArticles(): List<FocomPublication> =
        fetchPublishedArticlesWithStatus().articles

    suspend fun fetchPublishedArticlesWithStatus(): ArticlesSyncResult = withContext(ioDispatcher) {
        val service = supabaseService
        val apiKey = supabasePublishableKey.trim()
        if (service == null || apiKey.isBlank()) {
            val cached = articleCacheDao.getAll().map { it.toPublication() }
            if (cached.isNotEmpty()) {
                return@withContext ArticlesSyncResult(
                    articles = cached,
                    isFromCache = true,
                    cachedAt = articleCacheDao.getLastCachedAt()
                )
            }
            throw IOException("Supabase n'est pas configuré (SUPABASE_URL / SUPABASE_PUBLISHABLE_KEY manquants).")
        }

        try {
            val response = service.getPublishedArticles(
                apiKey = apiKey,
                authorization = authorizationHeaderOrNull()
            )
            if (!response.isSuccessful) {
                val errorDetail = response.errorBody()?.string()?.take(200).orEmpty()
                throw IOException(
                    "Erreur Supabase HTTP ${response.code()}" +
                        if (errorDetail.isNotBlank()) " : $errorDetail" else ""
                )
            }

            val now = System.currentTimeMillis()
            val remoteArticles = response.body().orEmpty().map { article ->
                val cleanSlug = article.slug?.trim().orEmpty()
                val rawImageUrl = article.imageUrl?.trim()?.takeIf { it.isNotBlank() }
                val resolvedImageUrl = when {
                    rawImageUrl == null -> null
                    rawImageUrl.startsWith("/") -> "https://focomues-iliad.fr$rawImageUrl"
                    else -> rawImageUrl
                }
                val formattedDate = article.publishedAt?.takeIf { it.isNotBlank() }?.toFrenchDateLabel()
                    ?: article.createdAt?.takeIf { it.isNotBlank() }?.toFrenchDateLabel()
                    ?: ""

                FocomPublication(
                    id = article.id,
                    title = article.title,
                    type = article.category?.takeIf { it.isNotBlank() }?.toArticleCategoryLabel() ?: "Actualité",
                    date = formattedDate,
                    thumbnailUrl = resolvedImageUrl,
                    pdfUrl = if (cleanSlug.isNotBlank()) {
                        "https://focomues-iliad.fr/actualites/$cleanSlug"
                    } else {
                        "https://focomues-iliad.fr/actualites"
                    },
                    excerpt = article.excerpt.orEmpty().toReadableArticleText(),
                    content = article.content.orEmpty().toReadableArticleText()
                )
            }
            articleCacheDao.replaceCache(
                remoteArticles.mapIndexed { index, article ->
                    CachedArticle.from(article, index).copy(cachedAt = now)
                }
            )
            ArticlesSyncResult(
                articles = remoteArticles,
                isFromCache = false,
                cachedAt = now
            )
        } catch (error: Exception) {
            val cached = articleCacheDao.getAll().map { it.toPublication() }
            if (cached.isNotEmpty()) {
                ArticlesSyncResult(
                    articles = cached,
                    isFromCache = true,
                    cachedAt = articleCacheDao.getLastCachedAt()
                )
            } else {
                throw error
            }
        }
    }
}

private fun String.toArticleCategoryLabel(): String {
    return when (this) {
        "actualite" -> "Actualité"
        "communique" -> "Communiqué"
        "evenement" -> "Événement"
        "victoire" -> "Victoire syndicale"
        else -> replaceFirstChar { it.uppercaseChar() }
    }
}

private fun String.toFrenchDateLabel(): String {
    return runCatching {
        val date = substringBefore("T")
        val parts = date.split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else date
    }.getOrDefault(this)
}

private fun String.toReadableArticleText(): String {
    if (isBlank()) return ""

    var text = decodeHtmlEntities()
        .replace("\r\n", "\n")
        .replace("\r", "\n")

    text = text
        .replace(Regex("(?is)<(script|style|svg)[^>]*>.*?</\\1>"), " ")
        .replace(Regex("(?is)<br\\s*/?>"), "\n")
        .replace(Regex("(?is)<li[^>]*>"), "\n- ")
        .replace(Regex("(?is)<h[1-6][^>]*>"), "\n")
        .replace(Regex("(?is)</(p|div|section|article|header|footer|h[1-6]|blockquote|ul|ol|li)>"), "\n")
        .replace(Regex("(?is)<[^>]+>"), " ")
        .decodeHtmlEntities()
        .replace(Regex("(?m)^\\s*[•·]\\s+"), "- ")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")

    return text
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { it.isLikelySourceCodeLine() }
        .joinToString("\n")
        .trim()
}

private fun String.decodeHtmlEntities(): String {
    return replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#34;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&rsquo;", "'")
        .replace("&lsquo;", "'")
        .replace("&rdquo;", "\"")
        .replace("&ldquo;", "\"")
        .replace("&euro;", "€")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("&#(\\d+);")) { match ->
            match.groupValues[1].toIntOrNull()?.let { code ->
                runCatching { code.toChar().toString() }.getOrNull()
            } ?: match.value
        }
}

private fun String.isLikelySourceCodeLine(): Boolean {
    val line = trim()
    if (line.length < 4) return false
    return line.startsWith("import ") ||
        line.startsWith("export ") ||
        line.startsWith("const ") ||
        line.startsWith("let ") ||
        line.startsWith("function ") ||
        line.startsWith("return (") ||
        line.startsWith(");") ||
        line.contains(" className=") ||
        line.contains(" from \"") ||
        line.contains(" from '") ||
        line.contains("useState(") ||
        line.contains("=>") && line.contains("{")
}


private fun FocomFlashBanner.isActiveAt(now: Instant): Boolean {
    val start = parseBannerDate(startDate) ?: return false
    val end = parseBannerDate(endDate) ?: return false
    return !now.isBefore(start) && !now.isAfter(end)
}

private fun parseBannerDate(value: String): Instant? {
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
}
