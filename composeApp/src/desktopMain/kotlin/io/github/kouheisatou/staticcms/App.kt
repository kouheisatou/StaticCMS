package io.github.kouheisatou.staticcms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.github.kouheisatou.staticcms.model.AppScreen
import io.github.kouheisatou.staticcms.screens.ArticleDetailScreen
import io.github.kouheisatou.staticcms.screens.CloneProgressScreen
import io.github.kouheisatou.staticcms.screens.GitHubAuthScreen
import io.github.kouheisatou.staticcms.screens.MainScreen
import io.github.kouheisatou.staticcms.screens.RepositorySelectionScreen
import io.github.kouheisatou.staticcms.ui.theme.RetroTheme
import io.github.kouheisatou.staticcms.viewmodel.StaticCMSViewModel

/** Main application composable function Manages the overall navigation and screen flow */
@Composable
fun app() {
    val viewModel = remember { StaticCMSViewModel() }

    // Collect state flows
    val state by viewModel.state.collectAsState()
    val githubAuthState by viewModel.gitHubAuthState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val gitOperationState by viewModel.gitOperationState.collectAsState()
    val gitOperationProgress by viewModel.gitOperationProgress.collectAsState()

    RetroTheme {
        when (state.currentScreen) {
            AppScreen.GITHUB_AUTH -> {
                GitHubAuthScreen(
                    githubToken = "",
                    onGitHubTokenChange = {},
                    onAuthenticateClick = {},
                    onBrowserAuthClick = viewModel::authenticateWithBrowser,
                    authState = githubAuthState,
                    currentUser = currentUser,
                    onContinue = {},
                )
            }
            AppScreen.REPOSITORY_INPUT -> {
                RepositorySelectionScreen(
                    repositories = state.availableRepositories,
                    isLoading = state.isLoadingRepositories,
                    onRepositorySelected = viewModel::selectRepository,
                    onRefresh = viewModel::loadRepositories,
                )
            }
            AppScreen.CLONE_PROGRESS -> {
                // Use Git operations progress with fallback to state progress
                val actualProgress =
                    if (gitOperationProgress > 0f) {
                        gitOperationProgress
                    } else {
                        state.cloneProgress
                    }

                CloneProgressScreen(
                    repositoryUrl = state.selectedRepository?.full_name ?: state.repositoryUrl,
                    progress = actualProgress,
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
                    onCommitAndPush = { viewModel.commitAndPush("Update content via StaticCMS") },
                )
            }
            AppScreen.ARTICLE_DETAIL -> {
                state.selectedArticle?.let { article ->
                    ArticleDetailScreen(
                        article = article,
                        onContentChange = viewModel::updateArticleContent,
                        onSave = viewModel::saveArticle,
                        onBack = viewModel::backToMain,
                    )
                }
            }
        }
    }
}
