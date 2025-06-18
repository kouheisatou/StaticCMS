package io.github.kouheisatou.static_cms.viewmodel

import io.github.kouheisatou.static_cms.model.*
import io.github.kouheisatou.static_cms.util.FileOperations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import java.io.File

class StaticCMSViewModel {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    fun updateRepositoryUrl(url: String) {
        _state.value = _state.value.copy(repositoryUrl = url)
    }
    
    fun startClone() {
        val repositoryUrl = _state.value.repositoryUrl
        if (repositoryUrl.isBlank()) return
        
        _state.value = _state.value.copy(
            currentScreen = AppScreen.CLONE_PROGRESS,
            isCloning = true,
            cloneProgress = 0f
        )
        
        scope.launch {
            try {
                val rootDir = FileOperations.simulateClone(repositoryUrl) { progress ->
                    _state.value = _state.value.copy(cloneProgress = progress)
                }
                
                if (rootDir != null) {
                    val contentDirectories = FileOperations.scanContentDirectories(rootDir)
                    _state.value = _state.value.copy(
                        currentScreen = AppScreen.MAIN_VIEW,
                        isCloning = false,
                        contentDirectories = contentDirectories,
                        rootDirectory = rootDir
                    )
                } else {
                    // Handle clone failure
                    _state.value = _state.value.copy(
                        currentScreen = AppScreen.REPOSITORY_INPUT,
                        isCloning = false
                    )
                }
            } catch (e: Exception) {
                println("Clone failed: ${e.message}")
                _state.value = _state.value.copy(
                    currentScreen = AppScreen.REPOSITORY_INPUT,
                    isCloning = false
                )
            }
        }
    }
    
    fun selectLocalDirectory() {
        val selectedDir = FileOperations.selectDirectory()
        if (selectedDir != null) {
            val contentDirectories = FileOperations.scanContentDirectories(selectedDir)
            _state.value = _state.value.copy(
                currentScreen = AppScreen.MAIN_VIEW,
                contentDirectories = contentDirectories,
                rootDirectory = selectedDir
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
    
    fun exportChanges() {
        // TODO: Implement git commit and push functionality
        println("Export functionality not yet implemented")
    }
    
    fun dispose() {
        scope.cancel()
    }
} 