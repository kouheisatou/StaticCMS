# StaticCMS

静的サイトコンテンツ管理システム - Windows95風レトロUIデスクトップアプリケーション

## 概要

StaticCMSは、GitHubリポジトリと連携してCSV・Markdownファイルベースの静的サイトコンテンツを管理するデスクトップアプリケーションです。懐かしいWindows95風のUIで、Git操作の知識がなくても簡単にWebサイトコンテンツの更新ができます。

## 主要機能

### 🔐 GitHub統合
- **Personal Access Token認証**: セキュアなGitHub連携
- **自動クローン**: リポジトリの自動取得
- **ワンクリック同期**: 📤 Commit & Pushボタンで即座にサイト更新

### 📁 コンテンツ管理
- **CSV/Markdownファイル管理**: 構造化されたコンテンツ編集
- **プレビュー機能**: リアルタイムMarkdownプレビュー
- **メディアファイル対応**: 画像等のアセット管理

### 🎨 レトロUI
- **Windows95風デザイン**: 懐かしい3Dボタンとウィンドウ
- **直感的操作**: タブ形式での効率的なコンテンツ切り替え
- **プログレスバー**: 操作状況の視覚的フィードバック

## 技術スタック

- **言語**: Kotlin Multiplatform
- **UI**: Compose Desktop
- **HTTP**: Ktor Client
- **Git**: JGit Library
- **アーキテクチャ**: MVVM + Repository Pattern

## セットアップ

### 前提条件
- Java 17以上
- GitHub Personal Access Token（repo権限付き）

### インストール & 実行

```bash
# リポジトリクローン
git clone https://github.com/yourusername/StaticCMS.git
cd StaticCMS

# アプリケーション実行
./gradlew :composeApp:run
```

### GitHub Personal Access Token取得
1. GitHub Settings → Developer settings → Personal access tokens
2. Generate new token (classic)
3. `repo` スコープを選択してトークン生成
4. アプリケーションでトークンを入力

## 使用方法

### 1. GitHub認証
- アプリ起動後、Personal Access Tokenを入力
- 認証成功でユーザー情報が表示

### 2. リポジトリクローン
- GitHubリポジトリURLを入力
- Clone Repositoryボタンでローカルに取得

### 3. コンテンツ編集
- タブでディレクトリ切り替え
- CSVテーブルでメタデータ管理
- Markdownエディタで記事編集

### 4. 変更の公開
- **📤 Commit & Push**ボタンで一括同期
- 自動的にGitコミット・プッシュが実行

## プロジェクト構造

```
StaticCMS/
├── composeApp/
│   └── src/desktopMain/kotlin/
│       └── io/github/kouheisatou/static_cms/
│           ├── App.kt                    # メインアプリケーション
│           ├── model/                    # データモデル
│           ├── screens/                  # UI画面
│           ├── ui/                       # UIコンポーネント・テーマ
│           ├── util/                     # ユーティリティ
│           │   ├── GitHubApiClient.kt    # GitHub API連携
│           │   ├── GitOperations.kt      # Git操作
│           │   └── FileOperations.kt     # ファイル操作
│           └── viewmodel/                # ビジネスロジック
├── doc/                                  # 設計文書
│   ├── design/                          # 設計詳細仕様書
│   ├── architecture/                    # アーキテクチャ設計書
│   ├── api/                             # API設計書
│   └── ui/                              # UI設計書
└── README.md
```

## 設計文書

詳細な設計情報は以下の文書を参照してください：

- [アーキテクチャ設計書](doc/architecture/アーキテクチャ設計書_StaticCMS_v1.0.md)
- [GitHub統合機能設計書](doc/design/設計詳細仕様書_GitHub統合機能_v1.0.md)
- [CSV・Markdown管理機能設計書](doc/design/設計詳細仕様書_CSV・Markdown管理機能_v1.0.md)
- [API設計書](doc/api/API設計書_StaticCMS_v1.0.md)
- [UI設計書](doc/ui/UI設計書_Windows95レトロUI_v1.0.md)

## ライセンス

MIT License

## 貢献

プルリクエストやイシューの報告を歓迎します。設計文書に従って開発にご協力ください。

---

*懐かしいWindows95の世界で、モダンなGit/GitHub連携を楽しみましょう！*
