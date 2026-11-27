package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContactMessage
import com.example.data.model.FocomContent
import com.example.data.model.FocomPublication
import com.example.data.model.FocomFlashBanner
import com.example.data.model.GitHubCommitContainer
import com.example.data.model.GitHubContent
import com.example.data.model.GitHubIssue
import com.example.data.model.GitHubOwner
import com.example.data.model.GitHubRepo
import com.example.data.model.SavedRepo
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Stack

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class GitResumeViewModel(
    private val repository: GitHubRepository
) : ViewModel() {

    // 0. Token for authenticating GitHub API requests (resolves API limits)
    private val _apiToken = MutableStateFlow("")
    val apiToken = _apiToken.asStateFlow()

    fun updateApiToken(token: String) {
        _apiToken.value = token
    }

    // 1. Saved/previous exploration resume history
    val savedRepos: StateFlow<List<SavedRepo>> = repository.allSavedRepos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val contactMessages: StateFlow<List<ContactMessage>> = repository.allContactMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Active Repository
    private val _activeRepoState = MutableStateFlow<UiState<GitHubRepo>>(UiState.Idle)
    val activeRepoState: StateFlow<UiState<GitHubRepo>> = _activeRepoState.asStateFlow()

    // 3. File Explorer Path Stack
    private val directoryStack = Stack<String>()
    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _contentsState = MutableStateFlow<UiState<List<GitHubContent>>>(UiState.Idle)
    val contentsState: StateFlow<UiState<List<GitHubContent>>> = _contentsState.asStateFlow()

    // 4. Opened file reader state
    private val _openedFileState = MutableStateFlow<UiState<Pair<GitHubContent, String>>>(UiState.Idle)
    val openedFileState: StateFlow<UiState<Pair<GitHubContent, String>>> = _openedFileState.asStateFlow()

    // 5. Commits
    private val _commitsState = MutableStateFlow<UiState<List<GitHubCommitContainer>>>(UiState.Idle)
    val commitsState: StateFlow<UiState<List<GitHubCommitContainer>>> = _commitsState.asStateFlow()

    // 6. Issues
    private val _issuesState = MutableStateFlow<UiState<List<GitHubIssue>>>(UiState.Idle)
    val issuesState: StateFlow<UiState<List<GitHubIssue>>> = _issuesState.asStateFlow()

    // 7. Readme content
    private val _readmeState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val readmeState: StateFlow<UiState<String>> = _readmeState.asStateFlow()

    private val _focomContentState = MutableStateFlow<UiState<FocomContent>>(UiState.Idle)
    val focomContentState: StateFlow<UiState<FocomContent>> = _focomContentState.asStateFlow()

    private val _focomContentRefreshing = MutableStateFlow(false)
    val focomContentRefreshing: StateFlow<Boolean> = _focomContentRefreshing.asStateFlow()

    private val _focomContentSyncError = MutableStateFlow<String?>(null)
    val focomContentSyncError: StateFlow<String?> = _focomContentSyncError.asStateFlow()

    private val _lastGitHubSyncAt = MutableStateFlow<Long?>(null)
    val lastGitHubSyncAt: StateFlow<Long?> = _lastGitHubSyncAt.asStateFlow()

    private val _publishedArticlesState = MutableStateFlow<UiState<List<FocomPublication>>>(UiState.Idle)
    val publishedArticlesState: StateFlow<UiState<List<FocomPublication>>> = _publishedArticlesState.asStateFlow()

    val cachedArticles: StateFlow<List<FocomPublication>> = repository.observeCachedArticles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val articlesLastCachedAt: StateFlow<Long?> = repository.observeArticlesCachedAt()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isOfflineArticleCache = MutableStateFlow(false)
    val isOfflineArticleCache: StateFlow<Boolean> = _isOfflineArticleCache.asStateFlow()

    private val _articlesRefreshing = MutableStateFlow(false)
    val articlesRefreshing: StateFlow<Boolean> = _articlesRefreshing.asStateFlow()

    private val _articlesSyncError = MutableStateFlow<String?>(null)
    val articlesSyncError: StateFlow<String?> = _articlesSyncError.asStateFlow()

    private val _flashBannersState = MutableStateFlow<UiState<List<FocomFlashBanner>>>(UiState.Idle)
    val flashBannersState: StateFlow<UiState<List<FocomFlashBanner>>> = _flashBannersState.asStateFlow()

    private val _contactSendFeedback = MutableStateFlow<String?>(null)
    val contactSendFeedback: StateFlow<String?> = _contactSendFeedback.asStateFlow()

    private val _contactSendResult = MutableStateFlow<GitHubRepository.ContactSendResult?>(null)
    val contactSendResult: StateFlow<GitHubRepository.ContactSendResult?> = _contactSendResult.asStateFlow()

    private var activeOwner: String = DEFAULT_GITHUB_OWNER
    private var activeName: String = DEFAULT_GITHUB_REPO
    private var lastFocomSyncElapsedMs: Long = 0L
    private var lastArticlesSyncElapsedMs: Long = 0L
    private var lastBannersSyncElapsedMs: Long = 0L

    init {
        // Chargement automatique par défaut du dépôt du syndicat FOCOM UES ILIAD
        syncAllGitHubElements(DEFAULT_GITHUB_OWNER, DEFAULT_GITHUB_REPO)
        retryPendingMessages()
    }

    fun syncAllGitHubElements(
        owner: String = activeOwner.ifBlank { DEFAULT_GITHUB_OWNER },
        repo: String = activeName.ifBlank { DEFAULT_GITHUB_REPO }
    ) {
        selectRepository(owner, repo)
        refreshFocomContent(owner = DEFAULT_GITHUB_OWNER, repo = DEFAULT_GITHUB_REPO, force = true)
        refreshPublishedArticles(force = true)
        refreshFlashBanners(force = true)
        retryPendingMessages()
    }

    fun refreshTabDataIfStale(selectedTab: Int) {
        when (selectedTab) {
            0 -> {
                refreshFlashBanners(force = false)
                refreshPublishedArticles(force = false)
                refreshFocomContent(force = false)
            }
            1, 2 -> refreshFocomContent(force = false)
            3 -> {
                refreshPublishedArticles(force = false)
                refreshFocomContent(force = false)
            }
        }
    }

    fun refreshFocomContent(
        owner: String = DEFAULT_GITHUB_OWNER,
        repo: String = DEFAULT_GITHUB_REPO,
        preferredBranch: String? = (_activeRepoState.value as? UiState.Success)?.data?.defaultBranch,
        force: Boolean = true
    ) {
        if (_focomContentRefreshing.value) return
        val now = System.currentTimeMillis()
        if (!force && _focomContentState.value is UiState.Success && (now - lastFocomSyncElapsedMs) < AUTO_SYNC_COOLDOWN_MS) {
            return
        }
        _focomContentRefreshing.value = true
        _focomContentSyncError.value = null
        viewModelScope.launch {
            if (_focomContentState.value !is UiState.Success) {
                val cachedContent = runCatching { repository.getCachedFocomContent() }.getOrNull()
                if (cachedContent != null) {
                    _focomContentState.value = UiState.Success(cachedContent)
                    _lastGitHubSyncAt.value = repository.getCachedFocomContentTimestamp()
                } else {
                    _focomContentState.value = UiState.Loading
                }
            }

            val tokenHeader = if (_apiToken.value.isNotBlank()) "token ${_apiToken.value}" else null
            try {
                val content = repository.fetchFocomContentFromRepo(
                    owner = owner,
                    repo = repo,
                    preferredBranch = preferredBranch,
                    token = tokenHeader
                )
                _focomContentState.value = UiState.Success(content)
                _focomContentSyncError.value = null
                lastFocomSyncElapsedMs = System.currentTimeMillis()
                _lastGitHubSyncAt.value = repository.getCachedFocomContentTimestamp() ?: lastFocomSyncElapsedMs
            } catch (e: Exception) {
                val message = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Impossible de synchroniser les données FOCOM depuis GitHub."
                _focomContentSyncError.value = message
                if (_focomContentState.value !is UiState.Success) {
                    _focomContentState.value = UiState.Error(message)
                }
            } finally {
                _focomContentRefreshing.value = false
            }
        }
    }

    fun refreshFlashBanners(force: Boolean = true) {
        val now = System.currentTimeMillis()
        if (!force && _flashBannersState.value is UiState.Success && (now - lastBannersSyncElapsedMs) < AUTO_SYNC_COOLDOWN_MS) {
            return
        }
        viewModelScope.launch {
            try {
                _flashBannersState.value = UiState.Success(repository.fetchActiveFlashBanners())
                lastBannersSyncElapsedMs = System.currentTimeMillis()
            } catch (e: Exception) {
                if (_flashBannersState.value !is UiState.Success) {
                    _flashBannersState.value = UiState.Error(
                        e.localizedMessage ?: "Impossible de charger les bandeaux d'information."
                    )
                }
            }
        }
    }

    fun refreshPublishedArticles(force: Boolean = true) {
        if (_articlesRefreshing.value) return
        val now = System.currentTimeMillis()
        if (!force && _publishedArticlesState.value is UiState.Success && !_isOfflineArticleCache.value && (now - lastArticlesSyncElapsedMs) < AUTO_SYNC_COOLDOWN_MS) {
            return
        }
        _articlesRefreshing.value = true
        _articlesSyncError.value = null
        viewModelScope.launch {
            if (_publishedArticlesState.value !is UiState.Success) {
                val cached = runCatching { repository.getCachedArticles() }.getOrDefault(emptyList())
                if (cached.isNotEmpty()) {
                    _publishedArticlesState.value = UiState.Success(cached)
                    _isOfflineArticleCache.value = true
                } else {
                    _publishedArticlesState.value = UiState.Loading
                }
            }

            try {
                val result = repository.fetchPublishedArticlesWithStatus()
                _publishedArticlesState.value = UiState.Success(result.articles)
                _isOfflineArticleCache.value = result.isFromCache
                _articlesSyncError.value = null
                if (!result.isFromCache) {
                    lastArticlesSyncElapsedMs = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                val message = e.message?.takeIf { it.isNotBlank() }
                    ?: "Impossible de charger les actualités du site."
                _articlesSyncError.value = message
                if (_publishedArticlesState.value is UiState.Success) {
                    _isOfflineArticleCache.value = true
                } else {
                    _publishedArticlesState.value = UiState.Error(message)
                }
            } finally {
                _articlesRefreshing.value = false
            }
        }
    }

    fun sendInternalMessage(
        name: String,
        email: String,
        phone: String,
        subject: String,
        message: String
    ) {
        if (message.isBlank()) return
        viewModelScope.launch {
            val result = repository.sendContactMessage(
                ContactMessage(
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    subject = subject.trim().ifBlank { "Demande d'information" },
                    message = message.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
            _contactSendResult.value = result
            _contactSendFeedback.value = when (result) {
                GitHubRepository.ContactSendResult.SENT_REMOTE ->
                    "Message envoyé au syndicat et enregistré dans l'historique."
                GitHubRepository.ContactSendResult.FAILED_SAVED_LOCAL ->
                    "Enregistré localement : l'envoi sera retenté dès le retour du réseau."
                GitHubRepository.ContactSendResult.SAVED_LOCAL_ONLY ->
                    "Message enregistré dans votre historique local."
            }
        }
    }

    fun clearContactSendFeedback() {
        _contactSendFeedback.value = null
    }

    fun clearContactSendResult() {
        _contactSendResult.value = null
    }

    fun retryPendingMessages() {
        viewModelScope.launch {
            runCatching { repository.retryPendingContactMessages() }
        }
    }

    // Selects and initiates browsing for a repository
    fun selectRepository(ownerPath: String, repoName: String) {
        val cleanOwner = ownerPath.trim()
        val cleanRepo = repoName.trim()
        if (cleanOwner.isEmpty() || cleanRepo.isEmpty()) return

        activeOwner = cleanOwner
        activeName = cleanRepo

        // Reset state
        directoryStack.clear()
        _currentPath.value = ""
        _activeRepoState.value = UiState.Loading
        _contentsState.value = UiState.Loading
        _commitsState.value = UiState.Loading
        _issuesState.value = UiState.Loading
        _openedFileState.value = UiState.Idle
        _readmeState.value = UiState.Idle

        viewModelScope.launch {
            val tokenHeader = if (_apiToken.value.isNotBlank()) "token ${_apiToken.value}" else null

            // 1. Fetch Repository Metadata
            try {
                val repo = repository.fetchRepo(cleanOwner, cleanRepo, token = tokenHeader)
                _activeRepoState.value = UiState.Success(repo)
                _lastGitHubSyncAt.value = System.currentTimeMillis()

                // Add to (or update) Saved/Resume Database
                repository.saveRepo(
                    SavedRepo(
                        fullName = repo.fullName,
                        owner = repo.owner.login,
                        name = repo.name,
                        description = repo.description,
                        stars = repo.stars,
                        forks = repo.forks,
                        language = repo.language,
                        lastAccessed = System.currentTimeMillis(),
                        avatarUrl = repo.owner.avatarUrl
                    )
                )

                // 2. Load root directory contents, commits, and issues concurrently
                coroutineScope {
                    launch { loadContents("", tokenHeader) }
                    launch { loadCommits(tokenHeader) }
                    launch { loadIssues(tokenHeader) }
                }

            } catch (e: Exception) {
                val cachedRepo = repository.getSavedRepo("$cleanOwner/$cleanRepo")
                if (cachedRepo != null) {
                    _activeRepoState.value = UiState.Success(
                        GitHubRepo(
                            name = cachedRepo.name,
                            fullName = cachedRepo.fullName,
                            owner = GitHubOwner(
                                login = cachedRepo.owner,
                                avatarUrl = cachedRepo.avatarUrl
                            ),
                            description = cachedRepo.description,
                            stars = cachedRepo.stars,
                            forks = cachedRepo.forks,
                            openIssues = null,
                            language = cachedRepo.language,
                            defaultBranch = "main"
                        )
                    )
                } else {
                    _activeRepoState.value = UiState.Error(
                        e.localizedMessage ?: "Failed to find repository '$cleanOwner/$cleanRepo'. Check name or access token."
                    )
                }
                _contentsState.value = UiState.Error("Failed to load content")
                _commitsState.value = UiState.Error("Failed to load commits")
                _issuesState.value = UiState.Error("Failed to load issues")
            }
        }
    }

    private suspend fun loadContents(path: String, token: String?) {
        _contentsState.value = UiState.Loading
        try {
            val items = repository.fetchContents(activeOwner, activeName, path, token = token)
            _contentsState.value = UiState.Success(items)

            // Look for a README file to cache/view automatically
            if (path.isEmpty() && _readmeState.value is UiState.Idle) {
                val readmeFile = items.firstOrNull { it.name.lowercase() == "readme.md" || it.name.lowercase() == "readme" }
                if (readmeFile != null) {
                    loadReadme(readmeFile, token)
                } else {
                    _readmeState.value = UiState.Error("README absent dans ce dépôt.")
                }
            }
        } catch (e: Exception) {
            _contentsState.value = UiState.Error(e.localizedMessage ?: "Failed to load directory")
        }
    }

    private suspend fun loadReadme(readmeFile: GitHubContent, token: String?) {
        val url = readmeFile.downloadUrl
        if (url == null) {
            _readmeState.value = UiState.Error("README indisponible.")
            return
        }
        try {
            val content = repository.fetchFileContent(url, token = token)
            _readmeState.value = UiState.Success(content)
        } catch (e: Exception) {
            _readmeState.value = UiState.Error("README indisponible.")
        }
    }

    private suspend fun loadCommits(token: String?) {
        try {
            val commits = repository.fetchCommits(activeOwner, activeName, token = token)
            _commitsState.value = UiState.Success(commits)
        } catch (e: Exception) {
            _commitsState.value = UiState.Error(e.localizedMessage ?: "Failed to load commits")
        }
    }

    private suspend fun loadIssues(token: String?) {
        try {
            val issues = repository.fetchIssues(activeOwner, activeName, token = token)
            _issuesState.value = UiState.Success(issues)
        } catch (e: Exception) {
            _issuesState.value = UiState.Error(e.localizedMessage ?: "Failed to load issues")
        }
    }

    // Navigation inside folder structure
    fun enterDirectory(content: GitHubContent) {
        if (content.type != "dir") return
        viewModelScope.launch {
            val tokenHeader = if (_apiToken.value.isNotBlank()) "token ${_apiToken.value}" else null
            directoryStack.push(content.path)
            _currentPath.value = content.path
            loadContents(content.path, tokenHeader)
        }
    }

    // Go up one level in folders
    fun navigateBackDirectory() {
        if (directoryStack.isEmpty()) return
        directoryStack.pop()
        val newPath = if (directoryStack.isEmpty()) "" else directoryStack.peek()
        _currentPath.value = newPath
        viewModelScope.launch {
            val tokenHeader = if (_apiToken.value.isNotBlank()) "token ${_apiToken.value}" else null
            loadContents(newPath, tokenHeader)
        }
    }

    // Read full text content of code file
    fun openFile(content: GitHubContent) {
        if (content.type != "file" || content.downloadUrl == null) return
        
        // Safety size check to prevent Out Of Memory crash on giant binary or generated files
        if (content.size > 1024 * 1024) {
            _openedFileState.value = UiState.Error("Fichier trop volumineux (> 1 Mo) pour un affichage textuel.")
            return
        }
        
        _openedFileState.value = UiState.Loading
        viewModelScope.launch {
            val tokenHeader = if (_apiToken.value.isNotBlank()) "token ${_apiToken.value}" else null
            try {
                val fileText = repository.fetchFileContent(content.downloadUrl, token = tokenHeader)
                _openedFileState.value = UiState.Success(Pair(content, fileText))
            } catch (e: Exception) {
                _openedFileState.value = UiState.Error(e.localizedMessage ?: "Failed to open file: ${content.name}")
            }
        }
    }

    fun closeFile() {
        _openedFileState.value = UiState.Idle
    }

    // Delete bookmark
    fun deleteSavedRepo(fullName: String) {
        viewModelScope.launch {
            repository.removeSavedRepo(fullName)
        }
    }

    companion object {
        const val AUTO_SYNC_COOLDOWN_MS = 60_000L
        const val DEFAULT_GITHUB_OWNER = "linux92fr"
        const val DEFAULT_GITHUB_REPO = "focom-ues-iliad2"
        const val ANDROID_GITHUB_REPO = "Android_FOCOM"
        const val FOCOM_CONTENT_URL =
            "https://raw.githubusercontent.com/linux92fr/focom-ues-iliad2/main/data/focom-content.json"
    }
}
