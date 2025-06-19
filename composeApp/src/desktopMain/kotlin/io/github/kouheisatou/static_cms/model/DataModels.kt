package io.github.kouheisatou.static_cms.model

import kotlinx.serialization.Serializable
import java.io.File

enum class DirectoryType {
    ARTICLE, // 記事タイプ（nameJa, nameEn, thumbnail + 詳細ディレクトリ）
    ENUM     // enumタイプ（nameJa, nameEn のみ）
}

@Serializable
data class CsvRow(
    val id: String,
    val nameJa: String,
    val nameEn: String,
    val thumbnail: String? = null,
    val descJa: String? = null,
    val descEn: String? = null,
    val additionalFields: Map<String, String> = emptyMap()
)

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    val email: String? = null,
    val name: String? = null,
    val avatar_url: String? = null
)

@Serializable
data class GitHubRepository(
    val id: Long,
    val name: String,
    val full_name: String,
    val permissions: GitHubPermissions? = null,
    val clone_url: String? = null,
    val ssh_url: String? = null,
    val html_url: String? = null,
    val description: String? = null,
    val private: Boolean = false,
    val updated_at: String? = null,
    val language: String? = null,
    val stargazers_count: Int = 0,
    val forks_count: Int = 0
)

@Serializable
data class GitHubPermissions(
    val admin: Boolean,
    val push: Boolean,
    val pull: Boolean
)

@Serializable
data class GitHubCommit(
    val sha: String,
    val message: String
)

@Serializable
data class GitHubOAuthTokenResponse(
    val access_token: String,
    val token_type: String,
    val scope: String? = null
)

sealed class AuthState {
    object Idle : AuthState()
    object Starting : AuthState()
    object WaitingForUser : AuthState()
    object Processing : AuthState()
    data class Success(val user: GitHubUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class ContentDirectory(
    val name: String,
    val path: String,
    val type: DirectoryType,
    val csvFile: File,
    val data: List<CsvRow> = emptyList()
)

data class ArticleContent(
    val id: String,
    val markdownFile: File,
    val mediaDirectory: File?,
    val content: String = ""
)

data class AppState(
    val currentScreen: AppScreen = AppScreen.GITHUB_AUTH,
    val githubToken: String = "",
    val repositoryUrl: String = "",
    val selectedRepository: GitHubRepository? = null,
    val availableRepositories: List<GitHubRepository> = emptyList(),
    val isLoadingRepositories: Boolean = false,
    val cloneProgress: Float = 0f,
    val isCloning: Boolean = false,
    val contentDirectories: List<ContentDirectory> = emptyList(),
    val selectedDirectoryIndex: Int = 0,
    val selectedArticle: ArticleContent? = null,
    val rootDirectory: File? = null
)

enum class AppScreen {
    GITHUB_AUTH,        // 画面0: GitHub認証
    REPOSITORY_INPUT,   // 画面1: リポジトリURL入力
    CLONE_PROGRESS,     // 画面2: Clone進行中
    MAIN_VIEW,          // 画面3: メイン画面
    ARTICLE_DETAIL      // 画面4: 詳細画面
} 