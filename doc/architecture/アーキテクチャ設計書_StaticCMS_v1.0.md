# StaticCMS アーキテクチャ設計書

## 文書情報
- **作成日**: 2024-12-19
- **更新日**: 2024-12-19
- **作成者**: AI Assistant
- **バージョン**: 1.0
- **ステータス**: Approved

## 変更履歴
| バージョン | 日付 | 変更内容 | 作成者 |
|-----|---|----|-----|
| 1.0 | 2024-12-19 | 初版作成 | AI Assistant |

## 1. 概要

### 1.1 目的
StaticCMSは、CSV形式のデータとMarkdownファイルを管理するためのデスクトップアプリケーションです。Windows95風のレトロUIを採用し、Notion風の直感的な操作性を提供します。

### 1.2 対象範囲
- デスクトップアプリケーション全体のアーキテクチャ設計
- UI/UXアーキテクチャ
- データ管理アーキテクチャ
- ファイル操作アーキテクチャ

### 1.3 前提条件
- Kotlin Multiplatform + Compose Desktop環境
- JVM 11以上
- Git リポジトリ管理環境

### 1.4 制約事項
- デスクトップ専用（Web/Mobile非対応）
- ローカルファイルシステムベース
- Git操作は将来実装予定

## 2. システムアーキテクチャ

### 2.1 全体アーキテクチャ
```
┌─────────────────────────────────────┐
│           UI Layer                  │
│  ┌─────────────────────────────────┐│
│  │     Compose Desktop UI          ││
│  │  - RetroTheme                   ││
│  │  - RetroComponents              ││
│  │  - Screen Components            ││
│  └─────────────────────────────────┘│
└─────────────────┬───────────────────┘
                  │
┌─────────────────┴───────────────────┐
│        Presentation Layer           │
│  ┌─────────────────────────────────┐│
│  │      StaticCMSViewModel         ││
│  │  - State Management             ││
│  │  - Navigation Logic             ││
│  │  - Business Logic               ││
│  └─────────────────────────────────┘│
└─────────────────┬───────────────────┘
                  │
┌─────────────────┴───────────────────┐
│         Domain Layer                │
│  ┌─────────────────────────────────┐│
│  │       Data Models               ││
│  │  - AppState                     ││
│  │  - ContentDirectory             ││
│  │  - ArticleContent               ││
│  │  - CsvRow                       ││
│  └─────────────────────────────────┘│
└─────────────────┬───────────────────┘
                  │
┌─────────────────┴───────────────────┐
│         Data Layer                  │
│  ┌─────────────────────────────────┐│
│  │      FileOperations             ││
│  │  - CSV Reading/Writing          ││
│  │  - Markdown Processing          ││
│  │  - Directory Scanning           ││
│  │  - File Management              ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### 2.2 技術スタック
- **フレームワーク**: Kotlin Multiplatform
- **UI**: Compose Desktop
- **状態管理**: StateFlow + Coroutines
- **CSV処理**: kotlin-csv-jvm
- **Markdown処理**: CommonMark（将来実装）
- **ビルドシステム**: Gradle with Kotlin DSL

### 2.3 レイヤー構成

#### UI Layer
- **責務**: ユーザーインターフェースの表示と操作
- **主要コンポーネント**:
  - RetroTheme: Windows95風テーマ定義
  - RetroComponents: カスタムUIコンポーネント
  - Screen Components: 各画面のComposable

#### Presentation Layer
- **責務**: UI状態管理とビジネスロジック
- **主要コンポーネント**:
  - StaticCMSViewModel: アプリ全体の状態管理
  - Navigation Logic: 画面遷移制御

#### Domain Layer
- **責務**: ビジネスロジックとデータモデル定義
- **主要コンポーネント**:
  - Data Models: アプリケーションで使用するデータ構造
  - Business Rules: CSV/Markdownファイルの管理ルール

#### Data Layer
- **責務**: データアクセスとファイル操作
- **主要コンポーネント**:
  - FileOperations: ファイルシステムとの相互作用

## 3. 詳細設計

### 3.1 画面構成アーキテクチャ
```
App (Root Composable)
├── RepositoryInputScreen
│   └── RetroWindow
│       ├── RetroTextField
│       └── RetroTextButton
├── CloneProgressScreen
│   └── RetroWindow
│       └── RetroProgressBar
├── MainScreen
│   └── RetroWindow
│       ├── RetroTab (Multiple)
│       └── RetroTable
└── ArticleDetailScreen
    └── RetroWindow
        ├── Toolbar (RetroTextButton)
        ├── Editor Panel (BasicTextField)
        └── Preview Panel (MarkdownPreview)
```

### 3.2 データフローアーキテクチャ
```
User Input → ViewModel → FileOperations → File System
    ↓            ↑            ↓              ↓
UI State ← StateFlow ← Data Models ← Parsed Data
```

### 3.3 状態管理アーキテクチャ
```kotlin
AppState
├── currentScreen: AppScreen
├── repositoryUrl: String
├── cloneProgress: Float
├── contentDirectories: List<ContentDirectory>
├── selectedDirectoryIndex: Int
├── selectedArticle: ArticleContent?
└── rootDirectory: File?
```

## 4. 設計パターン

### 4.1 採用したパターン
- **MVVM (Model-View-ViewModel)**: UI状態管理
- **Repository Pattern**: データアクセス抽象化（FileOperations）
- **State Pattern**: 画面遷移管理
- **Observer Pattern**: StateFlow による状態監視

### 4.2 コンポーネント設計原則
- **Single Responsibility**: 各コンポーネントは単一の責務
- **Composability**: 小さなコンポーネントの組み合わせ
- **Reusability**: RetroComponentsの再利用性
- **Testability**: 各レイヤーの独立性

## 5. 非機能要件

### 5.1 パフォーマンス
- **起動時間**: 3秒以内
- **ファイル読み込み**: 100ファイル以下では1秒以内
- **UI応答性**: 100ms以内のレスポンス

### 5.2 拡張性
- **新しいファイル形式**: プラグイン形式で追加可能
- **新しいUI要素**: RetroComponentsに追加可能
- **新しい画面**: Screen追加で対応可能

### 5.3 保守性
- **コード分離**: レイヤー間の明確な分離
- **テスタビリティ**: 各レイヤーの単体テスト可能
- **ドキュメント**: 各コンポーネントの責務明確化

## 6. 技術的考慮事項

### 6.1 セキュリティ
- **ファイルアクセス**: ユーザー指定ディレクトリのみアクセス
- **入力検証**: ファイルパス、URL の妥当性チェック

### 6.2 エラーハンドリング
- **ファイル操作エラー**: 適切なエラーメッセージ表示
- **ネットワークエラー**: Git操作時のタイムアウト処理

### 6.3 ログ出力
- **デバッグログ**: 開発時のトラブルシューティング
- **エラーログ**: 本番環境でのエラー追跡

## 7. 将来拡張

### 7.1 Git統合
- **Clone機能**: 実際のGitリポジトリClone
- **Push/Pull機能**: 変更の同期
- **ブランチ管理**: 複数ブランチの切り替え

### 7.2 プラグインシステム
- **カスタムエディタ**: 特定ファイル形式用エディタ
- **テーマシステム**: 複数のレトロテーマ
- **外部ツール連携**: 画像編集ツール等

### 7.3 協働機能
- **リアルタイム編集**: 複数ユーザーでの同時編集
- **コメント機能**: ファイルへのコメント追加
- **レビュー機能**: 変更内容のレビューワークフロー

## 8. 実装時の注意事項

### 8.1 パフォーマンス考慮事項
- **大容量ファイル**: 段階的読み込み実装
- **メモリ使用量**: 大量データ処理時の最適化
- **UI更新**: 頻繁な状態更新の最適化

### 8.2 ユーザビリティ考慮事項
- **レスポンシブ**: ウィンドウサイズ変更への対応
- **キーボードショートカット**: 効率的な操作
- **エラーメッセージ**: ユーザーフレンドリーなメッセージ

### 8.3 保守性考慮事項
- **コードの可読性**: 命名規則の統一
- **テストコード**: 十分なテストカバレッジ
- **ドキュメント**: コードコメントとドキュメントの同期

## 関連ファイル
- メインアプリケーション: [App.kt](../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/App.kt)
- エントリーポイント: [main.kt](../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/main.kt)
- ビルド設定: [build.gradle.kts](../composeApp/build.gradle.kts)
- ViewModel: [StaticCMSViewModel.kt](../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/viewmodel/StaticCMSViewModel.kt)
- UIコンポーネント: [RetroComponents.kt](../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/ui/components/RetroComponents.kt) 