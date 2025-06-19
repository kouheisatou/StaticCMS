package io.github.kouheisatou.staticcms.viewmodel

import io.github.kouheisatou.staticcms.model.*
import io.github.kouheisatou.staticcms.util.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.api.Git

class StaticCMSViewModel {
    // Constants
    private companion object {
        const val SUCCESS_DELAY_MS = 1000L
        const val DEFAULT_COMMIT_MESSAGE = "Update content via StaticCMS"
    }

    // Coroutine scope for the ViewModel
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Main app state
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // Dependencies
    private val gitHubApiClient = GitHubApiClient()
    private val gitOperations = GitOperations()

    // Authentication properties
    private var githubToken: String = ""
    private var githubUsername: String = ""
    private var githubEmail: String = ""

    // Authentication state
    private val _gitHubAuthState = MutableStateFlow<AuthState>(AuthState.Idle)
    val gitHubAuthState: StateFlow<AuthState> = _gitHubAuthState

    private val _currentUser = MutableStateFlow<GitHubUser?>(null)
    val currentUser: StateFlow<GitHubUser?> = _currentUser

    // Git operation state
    val gitOperationState = gitOperations.operationState
    val gitOperationProgress = gitOperations.operationProgress

    // Current Git repository instance
    private var currentGitRepository: Git? = null

    init {
        observeAuthenticationState()
    }

    // Authentication methods

    fun authenticateWithBrowser() {
        scope.launch { gitHubApiClient.authenticateWithBrowser() }
    }

    private fun observeAuthenticationState() {
        scope.launch {
            gitHubApiClient.authenticationState.collect { authState ->
                _gitHubAuthState.value = authState
                when (authState) {
                    is AuthState.Success -> handleAuthenticationSuccess(authState)
                    is AuthState.Error -> handleAuthenticationError()
                    else -> {
                        // Keep current state for other states
                    }
                }
            }
        }
    }

    private suspend fun handleAuthenticationSuccess(authState: AuthState.Success) {
        updateAuthenticationData(authState.user)
        delay(SUCCESS_DELAY_MS) // Allow success message to be displayed
        proceedToRepositoryInput()
    }

    private fun handleAuthenticationError() {
        clearAuthenticationData()
    }

    private fun updateAuthenticationData(user: GitHubUser) {
        _currentUser.value = user
        githubToken = gitHubApiClient.getToken() ?: ""
        githubUsername = user.login
        githubEmail = user.email ?: "${user.login}@users.noreply.github.com"
    }

    private fun clearAuthenticationData() {
        _currentUser.value = null
        githubToken = ""
        githubUsername = ""
        githubEmail = ""
    }

    // Repository management methods

    fun updateRepositoryUrl(url: String) {
        updateState { copy(repositoryUrl = url) }
    }

    fun proceedToRepositoryInput() {
        updateState { copy(currentScreen = AppScreen.REPOSITORY_INPUT) }
        loadRepositories()
    }

    fun loadRepositories() {
        scope.launch {
            updateState { copy(isLoadingRepositories = true) }
            try {
                val repositories = gitHubApiClient.getUserRepositories().getOrThrow()
                updateState {
                    copy(availableRepositories = repositories, isLoadingRepositories = false)
                }
            } catch (e: Exception) {
                updateState {
                    copy(availableRepositories = emptyList(), isLoadingRepositories = false)
                }
            }
        }
    }

    fun selectRepository(repository: GitHubRepository) {
        updateState {
            copy(
                selectedRepository = repository,
                repositoryUrl = repository.clone_url ?: repository.html_url ?: "",
                currentScreen = AppScreen.CLONE_PROGRESS,
                isCloning = true,
                cloneProgress = 0f)
        }

        startCloneProcess()
    }

    private fun startCloneProcess() {
        scope.launch(Dispatchers.IO) {
            delay(100) // Small delay to allow UI to update
            performCloneOperation()
        }
    }

    private suspend fun performCloneOperation() {
        val currentState = _state.value
        val repositoryUrl = currentState.repositoryUrl

        if (!validateClonePrerequisites(repositoryUrl)) {
            return
        }

        try {
            val (owner, repo) = parseRepositoryInfo(repositoryUrl)
            val localPath = buildLocalPath(owner, repo)

            gitOperations.reset()
            val progressJob = monitorCloneProgress()

            val cloneResult =
                gitOperations.cloneRepository(
                    repositoryUrl = repositoryUrl,
                    destinationPath = localPath,
                    username = githubUsername,
                    token = githubToken)

            progressJob.cancel()

            if (cloneResult.isSuccess) {
                handleCloneSuccess(cloneResult.getOrThrow(), File(localPath))
            } else {
                throw cloneResult.exceptionOrNull() ?: Exception("Clone failed")
            }
        } catch (e: Exception) {
            handleCloneError(e.message ?: "Unknown error occurred")
        }
    }

    private fun validateClonePrerequisites(repositoryUrl: String): Boolean {
        if (repositoryUrl.isBlank() || githubToken.isEmpty()) {
            handleCloneError("Authentication or repository URL missing")
            return false
        }
        return true
    }

    private fun parseRepositoryInfo(repositoryUrl: String): Pair<String, String> {
        return gitOperations.parseRepositoryUrl(repositoryUrl)
            ?: throw Exception("Invalid repository URL")
    }

    private fun buildLocalPath(owner: String, repo: String): String {
        return "${System.getProperty("user.home")}/.staticcms/repositories/${owner}_$repo"
    }

    private fun monitorCloneProgress() =
        scope.launch(Dispatchers.Main) {
            gitOperations.operationProgress.collect { progress ->
                updateState { copy(cloneProgress = progress) }
            }
        }

    private suspend fun handleCloneSuccess(git: Git, rootDir: File) {
        currentGitRepository = git

        withContext(Dispatchers.Main) { updateState { copy(cloneProgress = 1.0f) } }

        delay(SUCCESS_DELAY_MS)

        val contentDirectories =
            withContext(Dispatchers.IO) { FileOperations.scanContentDirectories(rootDir) }

        withContext(Dispatchers.Main) {
            updateState {
                copy(
                    currentScreen = AppScreen.MAIN_VIEW,
                    isCloning = false,
                    contentDirectories = contentDirectories,
                    rootDirectory = rootDir,
                    cloneProgress = 1.0f)
            }
        }
    }

    private fun handleCloneError(errorMessage: String) {
        scope.launch(Dispatchers.Main) {
            updateState {
                copy(
                    currentScreen = AppScreen.REPOSITORY_INPUT,
                    isCloning = false,
                    cloneProgress = 0f)
            }
        }
    }

    // Content management methods

    fun selectDirectory(index: Int) {
        updateState { copy(selectedDirectoryIndex = index) }
    }

    fun refreshContentDirectories() {
        val rootDir = _state.value.rootDirectory ?: return

        scope.launch(Dispatchers.IO) {
            val contentDirectories = FileOperations.scanContentDirectories(rootDir)

            withContext(Dispatchers.Main) {
                updateState { copy(contentDirectories = contentDirectories) }
            }
        }
    }

    fun updateCellValue(directoryIndex: Int, rowIndex: Int, colIndex: Int, newValue: String) {
        val currentState = _state.value
        val directory = currentState.contentDirectories.getOrNull(directoryIndex) ?: return
        val row = directory.data.getOrNull(rowIndex) ?: return

        val updatedRow = updateRowValue(row, colIndex, newValue, directory.type)
        val updatedDirectory = updateDirectoryData(directory, rowIndex, updatedRow)
        val updatedDirectories =
            updateDirectoriesList(currentState.contentDirectories, directoryIndex, updatedDirectory)

        updateState { copy(contentDirectories = updatedDirectories) }

        scope.launch(Dispatchers.IO) { saveDirectoryToFile(updatedDirectory) }
    }

    /** Thumbnailカラムでの画像選択処理 */
    fun selectThumbnailImage(directoryIndex: Int, rowIndex: Int, colIndex: Int) {
        val currentState = _state.value
        val directory = currentState.contentDirectories.getOrNull(directoryIndex) ?: return
        val row = directory.data.getOrNull(rowIndex) ?: return

        // Thumbnailカラムのみ対応
        if (directory.type != DirectoryType.ARTICLE || colIndex != 3) {
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                // 保存先ディレクトリ（directory内のimageフォルダまたは各行のmediaフォルダ）
                val imageDir = File(directory.path, "images").apply { if (!exists()) mkdirs() }

                // 画像選択と処理
                val savedFileName = FileOperations.selectAndProcessThumbnailImage(row.id, imageDir)

                if (savedFileName != null) {
                    // CSVファイルを更新
                    withContext(Dispatchers.Main) {
                        updateCellValue(directoryIndex, rowIndex, colIndex, savedFileName)
                    }
                }
            } catch (e: Exception) {
                println("Error selecting thumbnail image: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Article management methods

    fun openArticle(rowIndex: Int, colIndex: Int) {
        val currentState = _state.value
        val selectedDirectory =
            currentState.contentDirectories.getOrNull(currentState.selectedDirectoryIndex)

        if (selectedDirectory?.type == DirectoryType.ARTICLE && colIndex == 0) {
            val selectedRow = selectedDirectory.data.getOrNull(rowIndex)
            if (selectedRow != null) {
                val articleDir = File(selectedDirectory.path, selectedRow.id)
                val articleContent = FileOperations.readMarkdownFile(articleDir)

                if (articleContent != null) {
                    updateState {
                        copy(
                            currentScreen = AppScreen.ARTICLE_DETAIL,
                            selectedArticle = articleContent)
                    }
                }
            }
        }
    }

    fun updateArticleContent(content: String) {
        val currentArticle = _state.value.selectedArticle
        if (currentArticle != null) {
            updateState { copy(selectedArticle = currentArticle.copy(content = content)) }
        }
    }

    fun saveArticle() {
        val currentArticle = _state.value.selectedArticle
        if (currentArticle != null) {
            FileOperations.writeMarkdownFile(currentArticle, currentArticle.content)
        }
    }

    fun backToMain() {
        updateState { copy(currentScreen = AppScreen.MAIN_VIEW, selectedArticle = null) }
    }

    // Git operations

    fun commitAndPush(commitMessage: String = DEFAULT_COMMIT_MESSAGE) {
        val git = currentGitRepository
        if (!validateGitOperationPrerequisites(git)) {
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                gitOperations.commitAndPush(
                    git = git!!,
                    commitMessage = commitMessage,
                    username = githubUsername,
                    email = githubEmail,
                    token = githubToken)
            } catch (e: Exception) {
                println("DEBUG: Commit/Push error: ${e.message}")
            }
        }
    }

    private fun validateGitOperationPrerequisites(git: Git?): Boolean {
        return git != null && githubToken.isNotEmpty() && githubUsername.isNotEmpty()
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
                callback(
                    hasPermission, if (hasPermission) null else "No write permission to repository")
            } else {
                callback(false, "Failed to check permissions: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // Helper methods for data updates

    private fun updateRowValue(
        row: CsvRow,
        colIndex: Int,
        newValue: String,
        directoryType: DirectoryType
    ): CsvRow {
        return when (colIndex) {
            0 -> row // ID column is not editable
            1 -> row.copy(nameJa = newValue)
            2 -> row.copy(nameEn = newValue)
            3 ->
                when (directoryType) {
                    DirectoryType.ARTICLE -> row.copy(thumbnail = newValue)
                    DirectoryType.ENUM -> updateAdditionalField(row, 0, newValue)
                }
            4 ->
                if (directoryType == DirectoryType.ARTICLE) {
                    row.copy(descJa = newValue)
                } else {
                    updateAdditionalField(row, 1, newValue)
                }
            5 ->
                if (directoryType == DirectoryType.ARTICLE) {
                    row.copy(descEn = newValue)
                } else {
                    updateAdditionalField(row, 2, newValue)
                }
            else -> {
                val additionalFieldIndex =
                    colIndex - (if (directoryType == DirectoryType.ARTICLE) 6 else 3)
                updateAdditionalField(row, additionalFieldIndex, newValue)
            }
        }
    }

    private fun updateAdditionalField(row: CsvRow, fieldIndex: Int, newValue: String): CsvRow {
        val additionalFields = row.additionalFields.toMutableMap()
        val fieldKeys = row.additionalFields.keys.toList()
        if (fieldIndex >= 0 && fieldIndex < fieldKeys.size) {
            additionalFields[fieldKeys[fieldIndex]] = newValue
        }
        return row.copy(additionalFields = additionalFields)
    }

    private fun updateDirectoryData(
        directory: ContentDirectory,
        rowIndex: Int,
        updatedRow: CsvRow
    ): ContentDirectory {
        val updatedData = directory.data.toMutableList()
        updatedData[rowIndex] = updatedRow
        return directory.copy(data = updatedData)
    }

    private fun updateDirectoriesList(
        directories: List<ContentDirectory>,
        directoryIndex: Int,
        updatedDirectory: ContentDirectory
    ): List<ContentDirectory> {
        val updatedDirectories = directories.toMutableList()
        updatedDirectories[directoryIndex] = updatedDirectory
        return updatedDirectories
    }

    private fun saveDirectoryToFile(directory: ContentDirectory) {
        try {
            FileOperations.writeCsvFile(directory)
        } catch (e: Exception) {
            // Handle error appropriately in production
            println("ERROR: Failed to save CSV file: ${e.message}")
        }
    }

    // State management helper

    private inline fun updateState(update: AppState.() -> AppState) {
        _state.value = _state.value.update()
    }

    // Cleanup

    fun dispose() {
        gitHubApiClient.close()
        gitOperations.reset()
        gitOperations.dispose()
        currentGitRepository?.close()
        scope.cancel()
    }
}
