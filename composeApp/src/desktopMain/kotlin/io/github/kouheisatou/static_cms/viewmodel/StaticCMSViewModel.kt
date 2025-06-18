package io.github.kouheisatou.static_cms.viewmodel

import io.github.kouheisatou.static_cms.model.*
import io.github.kouheisatou.static_cms.util.FileOperations
import io.github.kouheisatou.static_cms.util.GitHubApiClient
import io.github.kouheisatou.static_cms.util.GitOperations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import org.eclipse.jgit.api.Git
import java.io.File

class StaticCMSViewModel {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    // GitHub API client
    private val gitHubApiClient = GitHubApiClient()
    val gitHubAuthState = gitHubApiClient.authenticationState
    val currentUser = gitHubApiClient.currentUser
    
    // Git operations
    private val gitOperations = GitOperations()
    val gitOperationState = gitOperations.operationState
    val gitOperationProgress = gitOperations.operationProgress
    
    // Current Git repository instance
    private var currentGitRepository: Git? = null
    
    // GitHub credentials
    private var githubToken: String = ""
    private var githubUsername: String = ""
    private var githubEmail: String = ""
    
    fun updateGitHubToken(token: String) {
        _state.value = _state.value.copy(githubToken = token)
    }
    
    fun updateRepositoryUrl(url: String) {
        _state.value = _state.value.copy(repositoryUrl = url)
    }
    
    fun proceedToRepositoryInput() {
        _state.value = _state.value.copy(currentScreen = AppScreen.REPOSITORY_INPUT)
    }
    
    fun authenticateWithGitHub(token: String) {
        githubToken = token
        gitHubApiClient.initialize(token)
        
        scope.launch {
            val result = gitHubApiClient.authenticate()
            if (result.isSuccess) {
                val user = result.getOrThrow()
                githubUsername = user.login
                githubEmail = user.email ?: "${user.login}@users.noreply.github.com"
                println("GitHub authentication successful: ${user.login}")
            } else {
                println("GitHub authentication failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    fun checkRepositoryPermissions(repositoryUrl: String, callback: (Boolean, String?) -> Unit) {
        val repoInfo = gitOperations.parseRepositoryUrl(repositoryUrl)
        if (repoInfo == null) {
            callback(false, "Invalid repository URL")
            return
        }
        
        scope.launch {
            val (owner, repo) = repoInfo
            val result = gitHubApiClient.hasWritePermission(owner, repo)
            if (result.isSuccess) {
                val hasPermission = result.getOrThrow()
                callback(hasPermission, if (hasPermission) null else "No write permission to repository")
            } else {
                callback(false, "Failed to check permissions: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    fun startClone() {
        println("DEBUG: startClone() called")
        val currentState = _state.value
        val repositoryUrl = currentState.repositoryUrl
        println("DEBUG: Repository URL: '$repositoryUrl'")
        println("DEBUG: Current screen: ${currentState.currentScreen}")
        println("DEBUG: Is cloning: ${currentState.isCloning}")
        
        if (repositoryUrl.isBlank()) {
            println("DEBUG: Repository URL is blank, returning")
            return
        }
        
        // GitHub認証が必要かチェック
        if (githubToken.isEmpty()) {
            println("ERROR: GitHub authentication required but token is empty")
            return
        }
        
        println("DEBUG: Starting clone process...")
        println("DEBUG: Changing state to CLONE_PROGRESS")
        
        _state.value = _state.value.copy(
            currentScreen = AppScreen.CLONE_PROGRESS,
            isCloning = true,
            cloneProgress = 0f
        )
        
        println("DEBUG: State changed, launching coroutine...")
        
        scope.launch {
            try {
                println("DEBUG: Inside coroutine, parsing repository URL...")
                
                // GitOperationsの進行状況を監視
                val progressJob = launch {
                    gitOperations.operationProgress.collect { progress ->
                        _state.value = _state.value.copy(cloneProgress = progress)
                        println("DEBUG: Clone progress updated: $progress")
                    }
                }
                
                // 実際のGitクローンを実行
                val repoInfo = gitOperations.parseRepositoryUrl(repositoryUrl)
                if (repoInfo == null) {
                    progressJob.cancel()
                    throw Exception("Invalid repository URL")
                }
                
                val (owner, repo) = repoInfo
                val localPath = "${System.getProperty("user.home")}/.staticcms/repositories/${owner}_${repo}"
                println("DEBUG: Will clone to: $localPath")
                
                val gitResult = gitOperations.cloneRepository(
                    repositoryUrl = repositoryUrl,
                    destinationPath = localPath,
                    username = githubUsername,
                    token = githubToken
                )
                
                progressJob.cancel() // 進行状況監視を停止
                
                if (gitResult.isSuccess) {
                    println("DEBUG: Clone successful, setting up content directories...")
                    currentGitRepository = gitResult.getOrThrow()
                    val rootDir = File(localPath)
                    
                    // プログレスバーが完了したことを確認してから遷移
                    delay(500)
                    
                    val contentDirectories = FileOperations.scanContentDirectories(rootDir)
                    println("DEBUG: Found ${contentDirectories.size} content directories")
                    
                    _state.value = _state.value.copy(
                        currentScreen = AppScreen.MAIN_VIEW,
                        isCloning = false,
                        contentDirectories = contentDirectories,
                        rootDirectory = rootDir,
                        cloneProgress = 1.0f
                    )
                    println("DEBUG: Transition to MAIN_VIEW completed")
                } else {
                    throw gitResult.exceptionOrNull() ?: Exception("Clone failed")
                }
            } catch (e: Exception) {
                println("ERROR: Clone failed: ${e.message}")
                e.printStackTrace()
                _state.value = _state.value.copy(
                    currentScreen = AppScreen.REPOSITORY_INPUT,
                    isCloning = false,
                    cloneProgress = 0f
                )
            }
        }
        
        println("DEBUG: startClone() method completed")
    }
    
    fun selectDirectory(index: Int) {
        _state.value = _state.value.copy(selectedDirectoryIndex = index)
    }
    
    fun openArticle(rowIndex: Int, colIndex: Int) {
        val currentState = _state.value
        val selectedDirectory = currentState.contentDirectories.getOrNull(currentState.selectedDirectoryIndex)
        
        if (selectedDirectory?.type == DirectoryType.ARTICLE && colIndex == 0) {
            val selectedRow = selectedDirectory.data.getOrNull(rowIndex)
            if (selectedRow != null) {
                val articleDir = File(selectedDirectory.path, selectedRow.id)
                val articleContent = FileOperations.readMarkdownFile(articleDir)
                
                if (articleContent != null) {
                    _state.value = _state.value.copy(
                        currentScreen = AppScreen.ARTICLE_DETAIL,
                        selectedArticle = articleContent
                    )
                }
            }
        }
    }
    
    fun updateArticleContent(content: String) {
        val currentArticle = _state.value.selectedArticle
        if (currentArticle != null) {
            _state.value = _state.value.copy(
                selectedArticle = currentArticle.copy(content = content)
            )
        }
    }
    
    fun saveArticle() {
        val currentArticle = _state.value.selectedArticle
        if (currentArticle != null) {
            FileOperations.writeMarkdownFile(currentArticle, currentArticle.content)
            println("Article saved: ${currentArticle.markdownFile.absolutePath}")
        }
    }
    
    fun backToMain() {
        _state.value = _state.value.copy(
            currentScreen = AppScreen.MAIN_VIEW,
            selectedArticle = null
        )
    }
    
    // Additional utility functions
    fun refreshContentDirectories() {
        val rootDir = _state.value.rootDirectory
        if (rootDir != null) {
            val contentDirectories = FileOperations.scanContentDirectories(rootDir)
            _state.value = _state.value.copy(contentDirectories = contentDirectories)
        }
    }
    
    fun commitAndPush(commitMessage: String = "Update content via StaticCMS") {
        val git = currentGitRepository
        if (git == null) {
            println("No Git repository available")
            return
        }
        
        if (githubToken.isEmpty() || githubUsername.isEmpty()) {
            println("GitHub authentication required")
            return
        }
        
        scope.launch {
            try {
                val result = gitOperations.commitAndPush(
                    git = git,
                    commitMessage = commitMessage,
                    username = githubUsername,
                    email = githubEmail,
                    token = githubToken
                )
                
                if (result.isSuccess) {
                    println("Changes committed and pushed successfully")
                } else {
                    println("Commit/Push failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("Commit/Push error: ${e.message}")
            }
        }
    }
    
    fun exportChanges() {
        commitAndPush()
    }
    
    fun dispose() {
        gitHubApiClient.close()
        gitOperations.reset()
        currentGitRepository?.close()
        scope.cancel()
    }
} 