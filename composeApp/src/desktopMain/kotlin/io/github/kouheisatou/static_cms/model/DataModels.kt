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