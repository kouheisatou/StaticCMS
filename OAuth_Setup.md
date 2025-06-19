# StaticCMS GitHub OAuth設定ガイド

## OAuth App作成手順

### 1. GitHub OAuth Appの作成

1. GitHubにログインし、[Settings](https://github.com/settings) → [Developer settings](https://github.com/settings/developers) → [OAuth Apps](https://github.com/settings/developers) に移動
2. "New OAuth App"をクリック
3. 以下の情報を入力：
   - **Application name**: `StaticCMS` (お好みの名前)
   - **Homepage URL**: `https://github.com/your-username/StaticCMS` (任意のURL)
   - **Application description**: `Static Site Content Management System` (任意)
   - **Authorization callback URL**: `http://localhost:8080/callback` ⚠️**重要：正確に入力**

### 2. 認証情報の取得

OAuth Appを作成後、以下の情報を取得：

- **Client ID**: OAuth Appページに表示される
- **Client Secret**: "Generate a new client secret"をクリックして生成

### 3. コードへの設定

`composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/util/GitHubApiClient.kt`の以下の行を更新：

```kotlin
// TODO: 以下の値をあなたのOAuth App情報に置き換えてください
private val clientId = "あなたのClient ID"        // Client IDに置き換え
private val clientSecret = "あなたのClient Secret"  // Client Secretに置き換え
```

## セキュリティに関する注意事項

⚠️ **Client Secretは秘密情報です**
- GitHubにコードを公開する際は、Client Secretを含めないでください
- 本番環境では環境変数や設定ファイルから読み取ることを推奨します

## 使用方法

1. OAuth設定完了後、アプリケーションを起動
2. "🌐 Connect with GitHub"ボタンをクリック
3. ブラウザでGitHub認証を完了
4. StaticCMSに戻って、リポジトリ管理を開始

## トラブルシューティング

### ブラウザが開かない場合
- 手動でブラウザを開き、GitHubログインページにアクセス
- 認証完了後、自動的にStaticCMSに戻ります

### 認証エラーの場合
- Client IDとClient Secretが正しく設定されているか確認
- Callback URLが `http://localhost:8080/callback` と正確に設定されているか確認
- ファイアウォールがポート8080をブロックしていないか確認

### 権限エラーの場合
- OAuth App設定で「repo」スコープが有効になっているか確認
- 対象リポジトリへの書き込み権限があるか確認 