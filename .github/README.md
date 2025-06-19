# GitHub Actions Workflows

このプロジェクトでは、以下のGitHub Actionsワークフローが設定されています。

## ワークフローの概要

### 🔄 [build.yml](.github/workflows/build.yml)
**トリガー**: `main`ブランチへのpush、Pull Request

- **Lint**: コードフォーマットとlintチェック
- **ビルド**: Linux、Windows、macOS向けのビルド
- **自動リリース**: mainブランチへのpushで自動リリース作成

### 🚀 [release.yml](.github/workflows/release.yml)
**トリガー**: `v*`タグのpush

- **完全テスト**: lint、format、テスト実行
- **リリースビルド**: 各プラットフォーム向けのリリースパッケージ作成
- **GitHub Release**: 詳細なリリースノートとアセット付きでリリース作成

### ✅ [pr.yml](.github/workflows/pr.yml)
**トリガー**: Pull Request

- **軽量チェック**: 基本的なlintとビルドの確認
- **クロスプラットフォームテスト**: 各OSでのビルド確認
- **PRコメント**: ビルド結果の自動コメント

## ビルドステータス

[![Build and Package](../../actions/workflows/build.yml/badge.svg)](../../actions/workflows/build.yml)
[![Release](../../actions/workflows/release.yml/badge.svg)](../../actions/workflows/release.yml)
[![Pull Request Check](../../actions/workflows/pr.yml/badge.svg)](../../actions/workflows/pr.yml)

## 使用方法

### 開発時
1. Pull Requestを作成すると、`pr.yml`が実行されます
2. lintエラーがあれば修正してください
3. 全てのチェックが通ったらマージできます

### リリース時
1. バージョンタグを作成してpushします：
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. `release.yml`が実行され、GitHub Releaseが作成されます
3. 各プラットフォーム向けのインストーラーがアップロードされます

## 生成される成果物

### 開発ビルド
- **JAR**: 実行可能JARファイル
- **配布可能ファイル**: 各プラットフォーム向けの配布可能ディレクトリ

### リリースビルド
- **Windows**: `.msi`インストーラー
- **macOS**: `.dmg`ディスクイメージ
- **Linux**: `.deb`パッケージ

## ローカルでのビルド

```bash
# Lint & Format
./gradlew format
./gradlew lintCheck

# ビルド
./gradlew desktopJar

# パッケージ作成
./gradlew createDistributable
./gradlew packageDmg        # macOS
./gradlew packageMsi        # Windows
./gradlew packageDeb        # Linux
```

## 必要な環境

- **Java**: JDK 17
- **Gradle**: プロジェクトに含まれるGradle Wrapper使用
- **OS**: Windows、macOS、Linux対応

## トラブルシューティング

### ビルドが失敗する場合
1. lintエラーを確認: `./gradlew lintCheck`
2. フォーマットを修正: `./gradlew format`
3. テストを実行: `./gradlew desktopTest`

### リリースが作成されない場合
1. タグの形式を確認（`v*`形式）
2. GitHub Actionsの権限を確認
3. `GITHUB_TOKEN`の設定を確認 