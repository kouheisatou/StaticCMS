# GitHub統合機能 設計詳細仕様書

## 文書情報
- **機能ID**: F-002
- **機能名**: GitHub統合機能（認証・クローン・コミット・プッシュ）
- **作成日**: 2024-01-19
- **更新日**: 2024-01-19
- **作成者**: AI Assistant
- **バージョン**: 1.0
- **ステータス**: Approved
- **関連ファイル**: 
  - GitHubApiClient: [composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/util/GitHubApiClient.kt](mdc:composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/util/GitHubApiClient.kt)
  - GitOperations: [composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/util/GitOperations.kt](mdc:composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/util/GitOperations.kt)
  - ViewModel: [composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/viewmodel/StaticCMSViewModel.kt](mdc:composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/viewmodel/StaticCMSViewModel.kt)
  - 認証画面: [composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/screens/GitHubAuthScreen.kt](mdc:composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/screens/GitHubAuthScreen.kt)

## 変更履歴
| バージョン | 日付 | 変更種別 | 変更内容 | 作成者 | レビュア |
|-----|---|----|----|-----|-----|
| 1.0.0 | 2024-01-19 | 新規作成 | GitHub統合機能の初版設計書作成 | AI Assistant | - |

## 1. 機能概要

### 1.1 機能の目的
StaticCMSにGitHub連携機能を追加し、リポジトリのクローン、コンテンツ編集、コミット、プッシュを一貫して行える機能を提供する。ユーザーがGitコマンドを直接操作することなく、GUI上でバージョン管理された静的サイトコンテンツを管理できるようにする。

### 1.2 機能の範囲
- GitHub Personal Access Tokenによる認証
- GitHubリポジトリの読み書き権限確認
- JGitを使用した実際のGitリポジトリクローン
- コンテンツ編集後の自動コミット・プッシュ
- 操作プログレスの視覚的フィードバック

### 1.3 前提条件
- インターネット接続が利用可能
- ユーザーがGitHub Personal Access Tokenを事前に取得済み
- 対象リポジトリに対する書き込み権限を保有
- リポジトリが`contents`または`sample_contents`ディレクトリを含む

## 2. 要件定義

### 2.1 機能要件
#### FR-001: GitHub認証機能
- Personal Access Tokenによる認証
- 認証状態の管理（未認証/認証中/認証完了/エラー）
- ユーザー情報の取得・表示

#### FR-002: リポジトリクローン機能
- HTTPSプロトコルによるリポジトリクローン
- プログレス監視機能
- ローカルストレージへの保存

#### FR-003: Git操作機能
- ファイル変更の自動検知
- コミットメッセージの自動生成
- リモートリポジトリへのプッシュ

#### FR-004: 権限管理機能
- リポジトリアクセス権限の事前確認
- エラーハンドリングと適切なフィードバック

### 2.2 非機能要件
#### NFR-001: セキュリティ
- Personal Access Tokenの安全な取り扱い
- トークンの永続化を避ける
- HTTPS通信の強制

#### NFR-002: ユーザビリティ
- 操作プログレスの可視化
- 分かりやすいエラーメッセージ
- Windows95風UIとの統一性

#### NFR-003: パフォーマンス
- クローン操作の非同期実行
- UIのブロッキング回避
- メモリ効率的な実装

### 2.3 制約事項
- GitHubのAPI制限に準拠
- JGitライブラリの機能制限内での実装
- Kotlin Multiplatform + Compose Desktopの制約

## 3. 設計仕様

### 3.1 アーキテクチャ設計

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   UI Layer      │    │ Presentation    │    │   Domain        │
│                 │    │   Layer         │    │   Layer         │
│ GitHubAuthScreen│◄──►│StaticCMSViewModel│◄──►│GitHubApiClient  │
│ MainScreen      │    │                 │    │GitOperations    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                              ┌─────────────────┐
                                              │   Data Layer    │
                                              │                 │
                                              │ GitHub API      │
                                              │ JGit Library    │
                                              │ Local FileSystem│
                                              └─────────────────┘
```

### 3.2 クラス設計

#### GitHubApiClient
```kotlin
class GitHubApiClient {
    // 認証状態管理
    val authenticationState: StateFlow<AuthState>
    val currentUser: StateFlow<GitHubUser?>
    
    // 主要メソッド
    fun initialize(token: String)
    suspend fun authenticate(): Result<GitHubUser>
    suspend fun getRepository(owner: String, repo: String): Result<GitHubRepo>
    suspend fun hasWritePermission(owner: String, repo: String): Result<Boolean>
}
```

#### GitOperations
```kotlin
class GitOperations {
    // 操作状態管理
    val operationState: StateFlow<OperationState>
    val operationProgress: StateFlow<Float>
    
    // 主要メソッド
    suspend fun cloneRepository(...): Result<Git>
    suspend fun commitAndPush(...): Result<Unit>
    fun parseRepositoryUrl(url: String): Pair<String, String>?
}
```

### 3.3 データモデル設計

#### 認証関連
```kotlin
@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    val avatarUrl: String,
    val name: String?,
    val email: String?
)

sealed class AuthState {
    object NotAuthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: GitHubUser) : AuthState()
    data class Error(val message: String) : AuthState()
}
```

#### Git操作関連
```kotlin
sealed class OperationState {
    object Idle : OperationState()
    data class Cloning(val progress: Float) : OperationState()
    data class Committing(val message: String) : OperationState()
    data class Pushing(val progress: Float) : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}
```

### 3.4 API設計

#### GitHub REST API使用箇所
```
GET /user                           # ユーザー情報取得
GET /repos/{owner}/{repo}           # リポジトリ情報取得
POST /repos/{owner}/{repo}/git/commits  # コミット作成
PATCH /repos/{owner}/{repo}/git/refs/{ref}  # ブランチ更新
```

#### HTTPクライアント設定
```kotlin
HttpClient(CIO) {
    install(ContentNegotiation) { json(...) }
    install(Auth) { bearer { ... } }
}
```

## 4. インターフェース設計

### 4.1 UI設計

#### GitHub認証画面
- Personal Access Token入力フィールド（パスワード形式）
- 認証ボタン
- 認証状態表示（プログレスバー/成功/エラー）
- トークン取得手順の説明

#### メイン画面拡張
- 「📤 Commit & Push」ボタンの追加
- Git操作状態の表示
- プログレス表示

### 4.2 データベース設計
本機能ではデータベースは使用せず、メモリ内状態管理とローカルファイルシステムを使用。

## 5. 例外処理設計

### 5.1 例外パターン

#### 認証エラー
- 無効なトークン
- ネットワーク接続エラー
- GitHub API制限

#### Git操作エラー
- クローン失敗（権限不足、ネットワークエラー）
- コミット失敗（競合、権限エラー）
- プッシュ失敗（権限不足、ブランチ保護）

### 5.2 エラーハンドリング

```kotlin
// Result型を使用した統一的エラーハンドリング
suspend fun authenticate(): Result<GitHubUser> {
    return try {
        // 認証処理
        Result.success(user)
    } catch (e: Exception) {
        _authenticationState.value = AuthState.Error(e.message ?: "Unknown error")
        Result.failure(e)
    }
}
```

## 6. テスト設計

### 6.1 テスト観点
- 認証フローの正常/異常パターン
- Git操作の成功/失敗パターン
- ネットワークエラーハンドリング
- UIの状態遷移

### 6.2 テストケース

#### 認証テスト
| Test ID | テスト内容 | 期待結果 |
|---------|------------|----------|
| AUTH-001 | 有効なトークンで認証 | 認証成功、ユーザー情報取得 |
| AUTH-002 | 無効なトークンで認証 | 認証失敗、エラーメッセージ表示 |
| AUTH-003 | ネットワークエラー | 接続エラー、リトライ可能状態 |

#### Git操作テスト
| Test ID | テスト内容 | 期待結果 |
|---------|------------|----------|
| GIT-001 | 正常なリポジトリクローン | クローン成功、ローカルに保存 |
| GIT-002 | 権限のないリポジトリクローン | クローン失敗、権限エラー |
| GIT-003 | コミット・プッシュ | 変更がリモートに反映 |

### 6.3 テストデータ
- テスト用GitHubリポジトリ
- モックAPIレスポンス
- サンプルコンテンツファイル

## 7. 実装時の注意事項

### 7.1 パフォーマンス考慮事項
- Git操作の非同期実行でUIブロッキングを回避
- プログレス監視によるユーザーフィードバック
- メモリ効率的なファイル操作

### 7.2 セキュリティ考慮事項
- Personal Access Tokenの暗号化なしメモリ保存（永続化回避）
- HTTPS通信の強制
- 権限の最小化原則

### 7.3 保守性考慮事項
- 依存関係の明確化（Ktor, JGit）
- エラーハンドリングの統一
- ログ出力の適切な実装

## 8. 依存関係

### 8.1 外部ライブラリ
```kotlin
// HTTP クライアント
implementation("io.ktor:ktor-client-core:2.3.7")
implementation("io.ktor:ktor-client-cio:2.3.7")
implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
implementation("io.ktor:ktor-client-auth:2.3.7")

// Git操作
implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
```

### 8.2 内部依存関係
- StaticCMSViewModel ← GitHubApiClient, GitOperations
- GitHubAuthScreen ← GitHubApiClient.AuthState
- MainScreen ← Git操作状態

## 9. 配置と運用

### 9.1 ローカルストレージ
```
~/.staticcms/
└── repositories/
    └── {owner}_{repo}/
        ├── .git/
        ├── contents/
        └── sample_contents/
```

### 9.2 設定管理
- Personal Access Tokenは実行時のみメモリ保持
- リポジトリ情報の一時キャッシュ
- エラーログの出力

## 10. 今後の拡張予定

### 10.1 機能拡張
- SSH認証サポート
- ブランチ管理機能
- コミット履歴表示
- 差分表示機能

### 10.2 技術拡張
- GitHub Enterprise対応
- GitLab/Bitbucket対応
- オフライン機能

## 11. 関連文書
- [アーキテクチャ設計書_StaticCMS_v1.0.md](doc/architecture/アーキテクチャ設計書_StaticCMS_v1.0.md)
- [API設計書_StaticCMS_v1.0.md](doc/api/API設計書_StaticCMS_v1.0.md)
- [UI設計書_Windows95レトロUI_v1.0.md](doc/ui/UI設計書_Windows95レトロUI_v1.0.md) 