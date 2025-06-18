# StaticCMS API設計書

## 文書情報
- **機能ID**: API-001
- **機能名**: StaticCMS内部API設計
- **作成日**: 2024-12-19
- **作成者**: AI Assistant
- **関連ファイル**: 
  - ViewModel: [StaticCMSViewModel.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/viewmodel/StaticCMSViewModel.kt)
  - FileOperations: [FileOperations.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/util/FileOperations.kt)

## 変更履歴
| バージョン | 日付 | 変更種別 | 変更内容 | 作成者 |
|-----|---|----|----|-----|
| 1.0.0 | 2024-12-19 | 新規作成 | 初版作成 | AI Assistant |

## 1. API概要

### 1.1 目的
StaticCMSアプリケーション内で使用される内部APIの設計を定義し、各コンポーネント間の連携インターフェースを明確化する。

### 1.2 対象範囲
- ViewModel公開API
- FileOperations API
- データモデルAPI
- UI状態管理API

### 1.3 API設計原則
- **統一性**: 一貫したメソッド命名規則
- **型安全性**: Kotlinの型システムを活用
- **非同期処理**: Coroutinesによる非同期API
- **エラーハンドリング**: Resultパターンによる安全な処理

## 2. ViewModel API

### 2.1 StaticCMSViewModel

#### 基本情報
- **パッケージ**: `io.github.kouheisatou.static_cms.viewmodel`
- **責務**: アプリケーション全体の状態管理
- **ライフサイクル**: アプリケーション全体

#### 公開API

##### 状態管理API

###### appState
```kotlin
val appState: StateFlow<AppState>
```
- **型**: `StateFlow<AppState>`
- **用途**: アプリケーション全体の状態監視
- **読み取り専用**: はい
- **初期値**: `AppState()`

```kotlin
// 使用例
viewModel.appState.collectAsState().value
```

##### ナビゲーション API

###### navigateToCloneProgress
```kotlin
fun navigateToCloneProgress()
```
- **用途**: Clone進行画面への遷移
- **パラメータ**: なし
- **戻り値**: `Unit`
- **副作用**: `currentScreen` を `CLONE_PROGRESS` に変更

```kotlin
// 使用例
viewModel.navigateToCloneProgress()
```

###### navigateToMain
```kotlin
fun navigateToMain()
```
- **用途**: メイン画面への遷移
- **パラメータ**: なし
- **戻り値**: `Unit`
- **副作用**: `currentScreen` を `MAIN` に変更

###### navigateToArticleDetail
```kotlin
fun navigateToArticleDetail(articleId: String)
```
- **用途**: 記事詳細画面への遷移
- **パラメータ**: 
  - `articleId: String` - 記事ID
- **戻り値**: `Unit`
- **副作用**: 
  - `currentScreen` を `ARTICLE_DETAIL` に変更
  - 指定された記事を読み込み

```kotlin
// 使用例
viewModel.navigateToArticleDetail("1")
```

###### navigateBack
```kotlin
fun navigateBack()
```
- **用途**: 前の画面への戻り
- **パラメータ**: なし
- **戻り値**: `Unit`
- **副作用**: `currentScreen` を前の画面に変更

##### リポジトリ操作API

###### setRepositoryUrl
```kotlin
fun setRepositoryUrl(url: String)
```
- **用途**: リポジトリURLの設定
- **パラメータ**: 
  - `url: String` - GitリポジトリURL
- **戻り値**: `Unit`
- **副作用**: `repositoryUrl` を更新

```kotlin
// 使用例
viewModel.setRepositoryUrl("https://github.com/user/repo.git")
```

###### cloneRepository
```kotlin
suspend fun cloneRepository(): Result<Unit>
```
- **用途**: リポジトリのクローン（モック実装）
- **パラメータ**: なし
- **戻り値**: `Result<Unit>`
- **副作用**: 
  - `cloneProgress` を段階的に更新
  - 完了後にメイン画面に遷移

```kotlin
// 使用例
viewLifecycleScope.launch {
    when (val result = viewModel.cloneRepository()) {
        is Result.Success -> { /* 成功処理 */ }
        is Result.Error -> { /* エラー処理 */ }
    }
}
```

###### loadLocalDirectory
```kotlin
suspend fun loadLocalDirectory(directory: File): Result<Unit>
```
- **用途**: ローカルディレクトリの読み込み
- **パラメータ**: 
  - `directory: File` - 対象ディレクトリ
- **戻り値**: `Result<Unit>`
- **副作用**: 
  - `rootDirectory` を設定
  - `contentDirectories` を更新
  - メイン画面に遷移

```kotlin
// 使用例
viewLifecycleScope.launch {
    val directory = File("/path/to/contents")
    when (val result = viewModel.loadLocalDirectory(directory)) {
        is Result.Success -> { /* 成功処理 */ }
        is Result.Error -> { /* エラー処理 */ }
    }
}
```

##### コンテンツ操作API

###### selectDirectory
```kotlin
fun selectDirectory(index: Int)
```
- **用途**: ディレクトリタブの選択
- **パラメータ**: 
  - `index: Int` - ディレクトリインデックス
- **戻り値**: `Unit`
- **副作用**: `selectedDirectoryIndex` を更新

```kotlin
// 使用例
viewModel.selectDirectory(0) // 最初のディレクトリを選択
```

###### selectArticle
```kotlin
suspend fun selectArticle(articleId: String): Result<Unit>
```
- **用途**: 記事の選択と読み込み
- **パラメータ**: 
  - `articleId: String` - 記事ID
- **戻り値**: `Result<Unit>`
- **副作用**: 
  - `selectedArticle` を更新
  - 記事詳細画面に遷移

```kotlin
// 使用例
viewLifecycleScope.launch {
    when (val result = viewModel.selectArticle("1")) {
        is Result.Success -> { /* 成功処理 */ }
        is Result.Error -> { /* エラー処理 */ }
    }
}
```

###### updateArticleContent
```kotlin
fun updateArticleContent(content: String)
```
- **用途**: 記事内容の更新（メモリ内）
- **パラメータ**: 
  - `content: String` - 更新後の内容
- **戻り値**: `Unit`
- **副作用**: `selectedArticle?.content` を更新

```kotlin
// 使用例
viewModel.updateArticleContent("# Updated Content\n\nNew content...")
```

###### saveArticleContent
```kotlin
suspend fun saveArticleContent(): Result<Unit>
```
- **用途**: 記事内容のファイル保存
- **パラメータ**: なし
- **戻り値**: `Result<Unit>`
- **副作用**: ファイルシステムへの書き込み

```kotlin
// 使用例
viewLifecycleScope.launch {
    when (val result = viewModel.saveArticleContent()) {
        is Result.Success -> { /* 保存成功 */ }
        is Result.Error -> { /* 保存失敗 */ }
    }
}
```

### 2.2 状態オブジェクト

#### AppState
```kotlin
data class AppState(
    val currentScreen: AppScreen = AppScreen.REPOSITORY_INPUT,
    val repositoryUrl: String = "",
    val cloneProgress: Float = 0f,
    val rootDirectory: File? = null,
    val contentDirectories: List<ContentDirectory> = emptyList(),
    val selectedDirectoryIndex: Int = 0,
    val selectedArticle: ArticleContent? = null
)
```

#### AppScreen
```kotlin
enum class AppScreen {
    REPOSITORY_INPUT,
    CLONE_PROGRESS,
    MAIN,
    ARTICLE_DETAIL
}
```

## 3. FileOperations API

### 3.1 基本情報
- **パッケージ**: `io.github.kouheisatou.static_cms.util`
- **責務**: ファイルシステム操作
- **実装**: Object（シングルトン）

### 3.2 ディレクトリ操作API

#### scanContentDirectories
```kotlin
fun scanContentDirectories(rootDir: File): List<ContentDirectory>
```
- **用途**: コンテンツディレクトリのスキャン
- **パラメータ**: 
  - `rootDir: File` - ルートディレクトリ
- **戻り値**: `List<ContentDirectory>`
- **例外**: 
  - `SecurityException` - アクセス権限なし
  - `IOException` - ファイルシステムエラー

```kotlin
// 使用例
val rootDir = File("/path/to/contents")
val directories = FileOperations.scanContentDirectories(rootDir)
```

#### findDetailDirectories
```kotlin
fun findDetailDirectories(parentDir: File): List<File>
```
- **用途**: 詳細ディレクトリの検索
- **パラメータ**: 
  - `parentDir: File` - 親ディレクトリ
- **戻り値**: `List<File>`
- **条件**: 数値名のディレクトリのみ対象

```kotlin
// 使用例
val detailDirs = FileOperations.findDetailDirectories(File("/path/to/lecture"))
```

### 3.3 CSV操作API

#### parseCsvFile
```kotlin
fun parseCsvFile(csvFile: File): List<CsvRow>
```
- **用途**: CSVファイルの解析
- **パラメータ**: 
  - `csvFile: File` - CSVファイル
- **戻り値**: `List<CsvRow>`
- **例外**: 
  - `FileNotFoundException` - ファイル不存在
  - `CSVParseException` - 解析エラー

```kotlin
// 使用例
val csvFile = File("/path/to/data.csv")
val rows = FileOperations.parseCsvFile(csvFile)
```

#### writeCsvFile
```kotlin
fun writeCsvFile(csvFile: File, rows: List<CsvRow>)
```
- **用途**: CSVファイルの書き込み
- **パラメータ**: 
  - `csvFile: File` - 出力ファイル
  - `rows: List<CsvRow>` - データ行
- **戻り値**: `Unit`
- **例外**: 
  - `IOException` - 書き込みエラー

```kotlin
// 使用例
val rows = listOf(CsvRow(mapOf("id" to "1", "name" to "Test")))
FileOperations.writeCsvFile(File("/path/to/output.csv"), rows)
```

### 3.4 Markdown操作API

#### readMarkdownFile
```kotlin
fun readMarkdownFile(file: File): String
```
- **用途**: Markdownファイルの読み込み
- **パラメータ**: 
  - `file: File` - Markdownファイル
- **戻り値**: `String` - ファイル内容
- **エンコーディング**: UTF-8

```kotlin
// 使用例
val content = FileOperations.readMarkdownFile(File("/path/to/article.md"))
```

#### writeMarkdownFile
```kotlin
fun writeMarkdownFile(file: File, content: String)
```
- **用途**: Markdownファイルの書き込み
- **パラメータ**: 
  - `file: File` - 出力ファイル
  - `content: String` - 書き込み内容
- **戻り値**: `Unit`
- **副作用**: バックアップファイル作成

```kotlin
// 使用例
FileOperations.writeMarkdownFile(
    File("/path/to/article.md"), 
    "# New Article\n\nContent..."
)
```

### 3.5 画像操作API

#### findMediaFiles
```kotlin
fun findMediaFiles(directory: File): List<File>
```
- **用途**: メディアファイルの検索
- **パラメータ**: 
  - `directory: File` - 検索ディレクトリ
- **戻り値**: `List<File>`
- **対象**: jpg, png, gif, bmp, webp

```kotlin
// 使用例
val mediaFiles = FileOperations.findMediaFiles(File("/path/to/media"))
```

#### isImageFile
```kotlin
fun isImageFile(file: File): Boolean
```
- **用途**: 画像ファイル判定
- **パラメータ**: 
  - `file: File` - 判定ファイル
- **戻り値**: `Boolean`
- **判定基準**: 拡張子による判定

```kotlin
// 使用例
if (FileOperations.isImageFile(file)) {
    // 画像ファイル処理
}
```

## 4. データモデルAPI

### 4.1 CsvRow

#### コンストラクタ
```kotlin
data class CsvRow(
    val values: Map<String, String>
)
```

#### プロパティアクセスAPI

##### 基本プロパティ
```kotlin
val id: String get() = values["id"] ?: ""
val nameJa: String get() = values["nameJa"] ?: ""
val nameEn: String get() = values["nameEn"] ?: ""
val thumbnail: String? get() = values["thumbnail"]
```

##### カスタムアクセス
```kotlin
fun getValue(key: String): String? = values[key]
fun getValueOrDefault(key: String, default: String): String = values[key] ?: default
```

```kotlin
// 使用例
val row = CsvRow(mapOf("id" to "1", "nameJa" to "テスト"))
println(row.id) // "1"
println(row.nameJa) // "テスト"
println(row.getValue("description")) // null
println(row.getValueOrDefault("description", "説明なし")) // "説明なし"
```

### 4.2 ContentDirectory

#### コンストラクタ
```kotlin
data class ContentDirectory(
    val name: String,
    val type: DirectoryType,
    val csvFile: File,
    val rows: List<CsvRow>,
    val detailDirectories: List<File> = emptyList()
)
```

#### ユーティリティメソッド
```kotlin
fun getRowById(id: String): CsvRow? = rows.find { it.id == id }
fun hasDetailDirectory(id: String): Boolean = 
    detailDirectories.any { it.name == id }
fun getDetailDirectory(id: String): File? = 
    detailDirectories.find { it.name == id }
```

```kotlin
// 使用例
val directory = ContentDirectory(...)
val row = directory.getRowById("1")
val hasDetail = directory.hasDetailDirectory("1")
val detailDir = directory.getDetailDirectory("1")
```

### 4.3 ArticleContent

#### コンストラクタ
```kotlin
data class ArticleContent(
    val id: String,
    val markdownFile: File,
    val content: String,
    val mediaFiles: List<File> = emptyList()
)
```

#### ユーティリティメソッド
```kotlin
fun getMediaFile(filename: String): File? = 
    mediaFiles.find { it.name == filename }
fun hasMediaFile(filename: String): Boolean = 
    mediaFiles.any { it.name == filename }
fun getRelativeMediaPath(filename: String): String = "./media/$filename"
```

```kotlin
// 使用例
val article = ArticleContent(...)
val imageFile = article.getMediaFile("image1.png")
val hasImage = article.hasMediaFile("image1.png")
val relativePath = article.getRelativeMediaPath("image1.png")
```

## 5. 結果型API

### 5.1 Result型定義

```kotlin
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val exception: Throwable, val message: String) : Result<T>()
}
```

### 5.2 拡張メソッド

#### isSuccess / isError
```kotlin
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success
fun <T> Result<T>.isError(): Boolean = this is Result.Error
```

#### getOrNull / getOrDefault
```kotlin
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Error -> null
}

fun <T> Result<T>.getOrDefault(default: T): T = when (this) {
    is Result.Success -> data
    is Result.Error -> default
}
```

#### onSuccess / onError
```kotlin
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Throwable, String) -> Unit): Result<T> {
    if (this is Result.Error) action(exception, message)
    return this
}
```

```kotlin
// 使用例
viewModel.cloneRepository()
    .onSuccess { println("Clone successful") }
    .onError { exception, message -> 
        println("Clone failed: $message")
        logger.error("Clone error", exception)
    }
```

## 6. 非同期処理API

### 6.1 Coroutines使用パターン

#### ViewModel内での使用
```kotlin
class StaticCMSViewModel {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    suspend fun heavyOperation(): Result<Unit> = withContext(Dispatchers.IO) {
        // 重い処理をバックグラウンドで実行
        try {
            // ファイル操作等
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Operation failed")
        }
    }
}
```

#### UI側での使用
```kotlin
@Composable
fun SomeScreen(viewModel: StaticCMSViewModel) {
    val scope = rememberCoroutineScope()
    
    Button(
        onClick = {
            scope.launch {
                when (val result = viewModel.heavyOperation()) {
                    is Result.Success -> { /* 成功処理 */ }
                    is Result.Error -> { /* エラー処理 */ }
                }
            }
        }
    ) {
        Text("Execute")
    }
}
```

### 6.2 進捗監視パターン

#### Progress監視
```kotlin
suspend fun longRunningOperation(
    onProgress: (Float) -> Unit
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        repeat(100) { i ->
            // 処理実行
            delay(50)
            onProgress(i / 100f)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, "Operation failed")
    }
}
```

```kotlin
// 使用例
scope.launch {
    viewModel.longRunningOperation { progress ->
        // UI更新
        progressState.value = progress
    }
}
```

## 7. エラーハンドリングAPI

### 7.1 エラー分類

#### AppException階層
```kotlin
sealed class AppException(message: String) : Exception(message) {
    class FileOperationException(message: String) : AppException(message)
    class DataParseException(message: String) : AppException(message)
    class ValidationException(message: String) : AppException(message)
    class NetworkException(message: String) : AppException(message)
}
```

### 7.2 エラーハンドリングユーティリティ

#### safeCall
```kotlin
inline fun <T> safeCall(action: () -> T): Result<T> {
    return try {
        Result.Success(action())
    } catch (e: Exception) {
        Result.Error(e, e.message ?: "Unknown error")
    }
}
```

#### safeFileCall
```kotlin
inline fun <T> safeFileCall(action: () -> T): Result<T> {
    return try {
        Result.Success(action())
    } catch (e: FileNotFoundException) {
        Result.Error(e, "ファイルが見つかりません")
    } catch (e: SecurityException) {
        Result.Error(e, "ファイルアクセス権限がありません")
    } catch (e: IOException) {
        Result.Error(e, "ファイル操作エラーが発生しました")
    } catch (e: Exception) {
        Result.Error(e, "予期しないエラーが発生しました")
    }
}
```

```kotlin
// 使用例
val result = safeFileCall {
    FileOperations.readMarkdownFile(file)
}
```

## 8. ログ出力API

### 8.1 Logger定義

```kotlin
object Logger {
    fun debug(message: String, tag: String = "StaticCMS") {
        println("DEBUG [$tag]: $message")
    }
    
    fun info(message: String, tag: String = "StaticCMS") {
        println("INFO [$tag]: $message")
    }
    
    fun error(message: String, exception: Throwable? = null, tag: String = "StaticCMS") {
        println("ERROR [$tag]: $message")
        exception?.printStackTrace()
    }
}
```

### 8.2 使用例

```kotlin
// 使用例
Logger.debug("Loading directory: ${directory.path}")
Logger.info("Successfully loaded ${rows.size} CSV rows")
Logger.error("Failed to parse CSV file", exception)
```

## 9. API使用例

### 9.1 完全なワークフロー例

```kotlin
// リポジトリ選択からメイン画面まで
suspend fun loadRepository(directoryPath: String) {
    val directory = File(directoryPath)
    
    when (val result = viewModel.loadLocalDirectory(directory)) {
        is Result.Success -> {
            Logger.info("Repository loaded successfully")
            // メイン画面に自動遷移
        }
        is Result.Error -> {
            Logger.error("Failed to load repository", result.exception)
            // エラーダイアログ表示
        }
    }
}

// 記事編集ワークフロー
suspend fun editArticle(articleId: String) {
    when (val result = viewModel.selectArticle(articleId)) {
        is Result.Success -> {
            // 記事詳細画面に遷移
            // エディタにコンテンツ表示
        }
        is Result.Error -> {
            Logger.error("Failed to load article", result.exception)
        }
    }
}

// 記事保存ワークフロー
suspend fun saveCurrentArticle() {
    when (val result = viewModel.saveArticleContent()) {
        is Result.Success -> {
            Logger.info("Article saved successfully")
            // 保存成功メッセージ表示
        }
        is Result.Error -> {
            Logger.error("Failed to save article", result.exception)
            // 保存失敗メッセージ表示
        }
    }
}
```

### 9.2 UI状態監視例

```kotlin
@Composable
fun MainScreen(viewModel: StaticCMSViewModel) {
    val appState by viewModel.appState.collectAsState()
    
    LaunchedEffect(appState.selectedDirectoryIndex) {
        // 選択ディレクトリ変更時の処理
        Logger.debug("Directory changed to index: ${appState.selectedDirectoryIndex}")
    }
    
    LaunchedEffect(appState.currentScreen) {
        // 画面遷移時の処理
        Logger.debug("Screen changed to: ${appState.currentScreen}")
    }
}
```

## 10. GitHub統合API

### 10.1 GitHubApiClient

#### 基本情報
- **パッケージ**: `io.github.kouheisatou.static_cms.util`
- **責務**: GitHub API認証とリポジトリ操作
- **依存関係**: Ktor HTTP Client

#### 公開API

##### 認証API

###### initialize
```kotlin
fun initialize(token: String)
```
- **用途**: Personal Access Tokenによる初期化
- **パラメータ**: 
  - `token: String` - GitHub Personal Access Token
- **戻り値**: `Unit`
- **副作用**: HTTPクライアントを認証情報で初期化

###### authenticate
```kotlin
suspend fun authenticate(): Result<GitHubUser>
```
- **用途**: GitHub認証の実行
- **パラメータ**: なし
- **戻り値**: `Result<GitHubUser>`
- **副作用**: 認証状態を更新

##### リポジトリAPI

###### getRepository
```kotlin
suspend fun getRepository(owner: String, repo: String): Result<GitHubRepo>
```
- **用途**: リポジトリ情報の取得
- **パラメータ**: 
  - `owner: String` - リポジトリオーナー
  - `repo: String` - リポジトリ名
- **戻り値**: `Result<GitHubRepo>`

###### hasWritePermission
```kotlin
suspend fun hasWritePermission(owner: String, repo: String): Result<Boolean>
```
- **用途**: 書き込み権限の確認
- **パラメータ**: 
  - `owner: String` - リポジトリオーナー
  - `repo: String` - リポジトリ名
- **戻り値**: `Result<Boolean>`

### 10.2 GitOperations

#### 基本情報
- **パッケージ**: `io.github.kouheisatou.static_cms.util`
- **責務**: Gitリポジトリ操作
- **依存関係**: JGit Library

#### 公開API

##### Git操作API

###### cloneRepository
```kotlin
suspend fun cloneRepository(
    repositoryUrl: String,
    destinationPath: String,
    username: String,
    token: String
): Result<Git>
```
- **用途**: リポジトリのクローン
- **パラメータ**: 
  - `repositoryUrl: String` - クローン対象URL
  - `destinationPath: String` - ローカル保存先
  - `username: String` - GitHubユーザー名
  - `token: String` - 認証トークン
- **戻り値**: `Result<Git>`
- **副作用**: プログレス状態を更新

###### commitAndPush
```kotlin
suspend fun commitAndPush(
    git: Git,
    commitMessage: String,
    username: String,
    email: String,
    token: String
): Result<Unit>
```
- **用途**: コミットとプッシュの実行
- **パラメータ**: 
  - `git: Git` - Gitリポジトリインスタンス
  - `commitMessage: String` - コミットメッセージ
  - `username: String` - コミッター名
  - `email: String` - コミッターメール
  - `token: String` - 認証トークン
- **戻り値**: `Result<Unit>`
- **副作用**: プログレス状態を更新

### 10.3 統合ViewModelAPI拡張

#### GitHub認証関連API

###### authenticateWithGitHub
```kotlin
fun authenticateWithGitHub(token: String)
```
- **用途**: GitHub認証の実行
- **パラメータ**: 
  - `token: String` - Personal Access Token
- **戻り値**: `Unit`
- **副作用**: 認証状態を更新

###### commitAndPush
```kotlin
fun commitAndPush(commitMessage: String = "Update content via StaticCMS")
```
- **用途**: 変更のコミット・プッシュ
- **パラメータ**: 
  - `commitMessage: String` - コミットメッセージ（デフォルト値あり）
- **戻り値**: `Unit`
- **副作用**: Git操作の実行とプログレス更新

## 関連ファイル
- ViewModel: [StaticCMSViewModel.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/viewmodel/StaticCMSViewModel.kt)
- FileOperations: [FileOperations.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/util/FileOperations.kt)
- GitHubApiClient: [GitHubApiClient.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/util/GitHubApiClient.kt)
- GitOperations: [GitOperations.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/util/GitOperations.kt)
- データモデル: [DataModels.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/model/DataModels.kt)
- 画面実装: [screens/](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/screens/)

## 関連文書
- [アーキテクチャ設計書_StaticCMS_v1.0.md](../architecture/アーキテクチャ設計書_StaticCMS_v1.0.md)
- [設計詳細仕様書_CSV・Markdown管理機能_v1.0.md](../design/設計詳細仕様書_CSV・Markdown管理機能_v1.0.md)
- [設計詳細仕様書_GitHub統合機能_v1.0.md](../design/設計詳細仕様書_GitHub統合機能_v1.0.md)
- [UI設計書_Windows95レトロUI_v1.0.md](../ui/UI設計書_Windows95レトロUI_v1.0.md) 