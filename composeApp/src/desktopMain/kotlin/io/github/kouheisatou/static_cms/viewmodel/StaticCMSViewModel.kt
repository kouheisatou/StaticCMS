package io.github.kouheisatou.static_cms.viewmodel

import io.github.kouheisatou.static_cms.model.*
import io.github.kouheisatou.static_cms.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.api.Git
import java.io.File

class StaticCMSViewModel {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    // GitHub API client
    private val gitHubApiClient = GitHubApiClient()
    
    // GitHub認証関連のプロパティ
    private var githubToken: String = ""
    private var githubUsername: String = ""
    private var githubEmail: String = ""
    
    private val _gitHubAuthState = MutableStateFlow<io.github.kouheisatou.static_cms.model.AuthState>(io.github.kouheisatou.static_cms.model.AuthState.Idle)
    val gitHubAuthState: StateFlow<io.github.kouheisatou.static_cms.model.AuthState> = _gitHubAuthState
    
    private val _currentUser = MutableStateFlow<io.github.kouheisatou.static_cms.model.GitHubUser?>(null)
    val currentUser: StateFlow<io.github.kouheisatou.static_cms.model.GitHubUser?> = _currentUser
    
    // Git operations
    private val gitOperations = GitOperations()
    val gitOperationState = gitOperations.operationState
    val gitOperationProgress = gitOperations.operationProgress
    
    // Current Git repository instance
    private var currentGitRepository: Git? = null
    
    init {
        // GitHubApiClientの認証状態を監視
        scope.launch {
            gitHubApiClient.authenticationState.collect { authState ->
                _gitHubAuthState.value = authState
                when (authState) {
                    is io.github.kouheisatou.static_cms.model.AuthState.Success -> {
                        _currentUser.value = authState.user
                        githubToken = gitHubApiClient.getToken() ?: ""
                        githubUsername = authState.user.login
                        githubEmail = authState.user.email ?: "${authState.user.login}@users.noreply.github.com"
                        
                        // 認証成功後、自動的にリポジトリ選択画面に遷移
                        println("DEBUG: Authentication successful, proceeding to repository selection")
                        scope.launch {
                            delay(1000) // 1秒待ってから遷移（成功メッセージを表示する時間を確保）
                            proceedToRepositoryInput()
                        }
                    }
                    is io.github.kouheisatou.static_cms.model.AuthState.Error -> {
                        _currentUser.value = null
                        githubToken = ""
                        githubUsername = ""
                        githubEmail = ""
                    }
                    else -> {
                        // その他の状態では現在のユーザー情報を保持
                    }
                }
            }
        }
    }
    

    
    fun updateRepositoryUrl(url: String) {
        _state.value = _state.value.copy(repositoryUrl = url)
    }
    
    fun proceedToRepositoryInput() {
        _state.value = _state.value.copy(currentScreen = AppScreen.REPOSITORY_INPUT)
        // リポジトリ一覧を自動的に読み込み
        loadRepositories()
    }
    
    fun loadRepositories() {
        scope.launch {
            _state.value = _state.value.copy(isLoadingRepositories = true)
            try {
                val result = gitHubApiClient.getUserRepositories()
                if (result.isSuccess) {
                    val repositories = result.getOrThrow()
                    _state.value = _state.value.copy(
                        availableRepositories = repositories,
                        isLoadingRepositories = false
                    )
                    println("DEBUG: Loaded ${repositories.size} repositories")
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to load repositories")
                }
            } catch (e: Exception) {
                println("ERROR: Failed to load repositories: ${e.message}")
                _state.value = _state.value.copy(
                    availableRepositories = emptyList(),
                    isLoadingRepositories = false
                )
            }
        }
    }
    
    fun selectRepository(repository: io.github.kouheisatou.static_cms.model.GitHubRepository) {
        _state.value = _state.value.copy(
            selectedRepository = repository,
            repositoryUrl = repository.clone_url ?: repository.html_url ?: ""
        )
        println("DEBUG: Selected repository: ${repository.name}")
        println("DEBUG: Repository URL: ${_state.value.repositoryUrl}")
        
        // 即座にクローン進捗画面に遷移してUIフリーズを防ぐ
        _state.value = _state.value.copy(
            currentScreen = AppScreen.CLONE_PROGRESS,
            isCloning = true,
            cloneProgress = 0f
        )
        
        // 非同期でクローンを開始
        scope.launch {
            delay(100) // UIの更新を待つ
            startCloneProcess()
        }
    }
    
    private suspend fun startCloneProcess() {
        println("DEBUG: startCloneProcess() called")
        val currentState = _state.value
        val repositoryUrl = currentState.repositoryUrl
        println("DEBUG: Repository URL: '$repositoryUrl'")
        
        if (repositoryUrl.isBlank()) {
            println("DEBUG: Repository URL is blank, returning")
            _state.value = _state.value.copy(
                currentScreen = AppScreen.REPOSITORY_INPUT,
                isCloning = false,
                cloneProgress = 0f
            )
            return
        }
        
        // GitHub認証が必要かチェック
        if (githubToken.isEmpty()) {
            println("ERROR: GitHub authentication required but token is empty")
            _state.value = _state.value.copy(
                currentScreen = AppScreen.REPOSITORY_INPUT,
                isCloning = false,
                cloneProgress = 0f
            )
            return
        }
        
        try {
            println("DEBUG: Starting clone process...")
            
            // 実際のGitクローンを実行
            val repoInfo = gitOperations.parseRepositoryUrl(repositoryUrl)
            if (repoInfo == null) {
                throw Exception("Invalid repository URL")
            }
            
            val (owner, repo) = repoInfo
            val localPath = "${System.getProperty("user.home")}/.staticcms/repositories/${owner}_${repo}"
            println("DEBUG: Will clone to: $localPath")
            
            // GitOperationsの進行状況を監視（バックグラウンドで）
            val progressJob = scope.launch {
                gitOperations.operationProgress.collect { progress ->
                    _state.value = _state.value.copy(cloneProgress = progress)
                    println("DEBUG: ViewModel received clone progress: ${(progress * 100).toInt()}%")
                }
            }
            
            // 進捗監視の状態を監視
            val stateJob = scope.launch {
                gitOperations.operationState.collect { operationState ->
                    println("DEBUG: Git operation state: $operationState")
                }
            }
            
            println("DEBUG: Starting Git clone operation...")
            val gitResult = gitOperations.cloneRepository(
                repositoryUrl = repositoryUrl,
                destinationPath = localPath,
                username = githubUsername,
                token = githubToken
            )
            
            progressJob.cancel() // 進行状況監視を停止
            stateJob.cancel() // 状態監視を停止
            
            if (gitResult.isSuccess) {
                println("DEBUG: Clone successful, setting up content directories...")
                currentGitRepository = gitResult.getOrThrow()
                val rootDir = File(localPath)
                
                // プログレスバーが完了したことを確認してから遷移
                _state.value = _state.value.copy(cloneProgress = 1.0f)
                delay(1000) // ユーザーに完了を見せる時間
                
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
    
    fun updateCellValue(directoryIndex: Int, rowIndex: Int, colIndex: Int, newValue: String) {
        val currentState = _state.value
        val directory = currentState.contentDirectories.getOrNull(directoryIndex) ?: return
        val row = directory.data.getOrNull(rowIndex) ?: return
        
        // Create updated row
        val updatedRow = when (colIndex) {
            0 -> return // ID column is not editable
            1 -> row.copy(nameJa = newValue)
            2 -> row.copy(nameEn = newValue)
            3 -> when (directory.type) {
                DirectoryType.ARTICLE -> row.copy(thumbnail = newValue)
                DirectoryType.ENUM -> {
                    // For enum types, column 3 might be an additional field
                    val additionalFields = row.additionalFields.toMutableMap()
                    val fieldKeys = directory.data.firstOrNull()?.additionalFields?.keys?.toList() ?: emptyList()
                    if (fieldKeys.isNotEmpty()) {
                        additionalFields[fieldKeys[0]] = newValue
                    }
                    row.copy(additionalFields = additionalFields)
                }
            }
            4 -> if (directory.type == DirectoryType.ARTICLE) row.copy(descJa = newValue) else {
                val additionalFields = row.additionalFields.toMutableMap()
                val fieldKeys = directory.data.firstOrNull()?.additionalFields?.keys?.toList() ?: emptyList()
                if (fieldKeys.size > 1) {
                    additionalFields[fieldKeys[1]] = newValue
                }
                row.copy(additionalFields = additionalFields)
            }
            5 -> if (directory.type == DirectoryType.ARTICLE) row.copy(descEn = newValue) else {
                val additionalFields = row.additionalFields.toMutableMap()
                val fieldKeys = directory.data.firstOrNull()?.additionalFields?.keys?.toList() ?: emptyList()
                if (fieldKeys.size > 2) {
                    additionalFields[fieldKeys[2]] = newValue
                }
                row.copy(additionalFields = additionalFields)
            }
            else -> {
                // Handle additional fields for article type
                val additionalFields = row.additionalFields.toMutableMap()
                val fieldKeys = directory.data.firstOrNull()?.additionalFields?.keys?.toList() ?: emptyList()
                val additionalFieldIndex = colIndex - (if (directory.type == DirectoryType.ARTICLE) 6 else 3)
                if (additionalFieldIndex >= 0 && additionalFieldIndex < fieldKeys.size) {
                    additionalFields[fieldKeys[additionalFieldIndex]] = newValue
                }
                row.copy(additionalFields = additionalFields)
            }
        }
        
        // Update the directory data
        val updatedData = directory.data.toMutableList()
        updatedData[rowIndex] = updatedRow
        val updatedDirectory = directory.copy(data = updatedData)
        
        // Update the state
        val updatedDirectories = currentState.contentDirectories.toMutableList()
        updatedDirectories[directoryIndex] = updatedDirectory
        
        _state.value = _state.value.copy(contentDirectories = updatedDirectories)
        
        // Save the changes to CSV file
        saveDirectoryToFile(updatedDirectory)
        
        println("DEBUG: Updated cell [$rowIndex,$colIndex] to '$newValue'")
    }
    
    private fun saveDirectoryToFile(directory: ContentDirectory) {
        try {
            FileOperations.writeCsvFile(directory)
            println("DEBUG: Saved changes to ${directory.name}")
        } catch (e: Exception) {
            println("ERROR: Failed to save directory ${directory.name}: ${e.message}")
        }
    }
    
    fun authenticateWithBrowser() {
        scope.launch {
            gitHubApiClient.authenticateWithBrowser()
        }
    }
    
    fun startClone() {
        scope.launch {
            startCloneProcess()
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
} 