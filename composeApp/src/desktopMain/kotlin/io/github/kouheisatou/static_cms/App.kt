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
    
    RetroTheme {
        when (state.currentScreen) {
            AppScreen.REPOSITORY_INPUT -> {
                RepositoryInputScreen(
                    repositoryUrl = state.repositoryUrl,
                    onRepositoryUrlChange = viewModel::updateRepositoryUrl,
                    onCloneClick = viewModel::startClone,
                    onSelectLocalDirectory = viewModel::selectLocalDirectory
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
                    onCellClick = viewModel::openArticle
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