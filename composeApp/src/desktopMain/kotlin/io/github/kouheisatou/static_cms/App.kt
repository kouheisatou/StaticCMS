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
                    githubToken = state.githubToken,
                    onGitHubTokenChange = viewModel::updateGitHubToken,
                    onAuthenticateClick = { token ->
                        viewModel.authenticateWithGitHub(token)
                    },
                    authState = githubAuthState,
                    currentUser = currentUser,
                    onContinue = viewModel::proceedToRepositoryInput
                )
            }
            
            AppScreen.REPOSITORY_INPUT -> {
                RepositoryInputScreen(
                    repositoryUrl = state.repositoryUrl,
                    onRepositoryUrlChange = viewModel::updateRepositoryUrl,
                    onCloneClick = {
                        println("DEBUG: onCloneClick called in App.kt")
                        println("DEBUG: Current state: ${state.currentScreen}")
                        println("DEBUG: Repository URL: '${state.repositoryUrl}'")
                        println("DEBUG: GitHub token available: ${state.githubToken.isNotEmpty()}")
                        viewModel.startClone()
                        println("DEBUG: startClone() call completed")
                    }
                )
            }
            
            AppScreen.CLONE_PROGRESS -> {
                CloneProgressScreen(
                    repositoryUrl = state.repositoryUrl,
                    progress = state.cloneProgress
                )
            }
            
            AppScreen.MAIN_VIEW -> {
                MainScreen(
                    contentDirectories = state.contentDirectories,
                    selectedDirectoryIndex = state.selectedDirectoryIndex,
                    onDirectorySelected = viewModel::selectDirectory,
                    onCellClick = viewModel::openArticle,
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