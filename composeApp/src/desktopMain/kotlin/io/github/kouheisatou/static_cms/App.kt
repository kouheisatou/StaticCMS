package io.github.kouheisatou.static_cms

import androidx.compose.runtime.*
import io.github.kouheisatou.static_cms.screens.*
import io.github.kouheisatou.static_cms.ui.theme.RetroTheme
import io.github.kouheisatou.static_cms.viewmodel.StaticCMSViewModel
import io.github.kouheisatou.static_cms.model.AppScreen

@Composable
fun App() {
    val viewModel = remember { StaticCMSViewModel() }
    val state by viewModel.state.collectAsState()
    val githubAuthState by viewModel.gitHubAuthState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val gitOperationState by viewModel.gitOperationState.collectAsState()
    
    RetroTheme {
        when (state.currentScreen) {
            AppScreen.GITHUB_AUTH -> {
                GitHubAuthScreen(
                    githubToken = "",
                    onGitHubTokenChange = { },
                    onAuthenticateClick = { },
                    onBrowserAuthClick = {
                        viewModel.authenticateWithBrowser()
                    },
                    authState = githubAuthState,
                    currentUser = currentUser,
                    onContinue = { }
                )
            }
            
            AppScreen.REPOSITORY_INPUT -> {
                RepositorySelectionScreen(
                    repositories = state.availableRepositories,
                    isLoading = state.isLoadingRepositories,
                    onRepositorySelected = viewModel::selectRepository,
                    onRefresh = viewModel::loadRepositories
                )
            }
            
            AppScreen.CLONE_PROGRESS -> {
                CloneProgressScreen(
                    repositoryUrl = state.selectedRepository?.full_name ?: state.repositoryUrl,
                    progress = state.cloneProgress
                )
            }
            
            AppScreen.MAIN_VIEW -> {
                MainScreen(
                    contentDirectories = state.contentDirectories,
                    selectedDirectoryIndex = state.selectedDirectoryIndex,
                    onDirectorySelected = viewModel::selectDirectory,
                    onCellClick = viewModel::openArticle,
                    onCellEdit = { directoryIndex, rowIndex, colIndex, newValue ->
                        viewModel.updateCellValue(directoryIndex, rowIndex, colIndex, newValue)
                    },
                    onCommitAndPush = { 
                        viewModel.commitAndPush("Update content via StaticCMS")
                    }
                )
            }
            
            AppScreen.ARTICLE_DETAIL -> {
                state.selectedArticle?.let { article ->
                    ArticleDetailScreen(
                        article = article,
                        onContentChange = { content ->
                            viewModel.updateArticleContent(content)
                        },
                        onSave = viewModel::saveArticle,
                        onBack = viewModel::backToMain
                    )
                }
            }
        }
    }
}