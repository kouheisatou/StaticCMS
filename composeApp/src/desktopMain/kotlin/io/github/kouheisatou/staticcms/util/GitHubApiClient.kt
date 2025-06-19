package io.github.kouheisatou.staticcms.util

import io.github.kouheisatou.staticcms.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.awt.Desktop
import java.net.URI
import java.util.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.html.*
import kotlinx.serialization.json.Json

class GitHubApiClient {
    /*
     * OAuth Setup Instructions:
     *
     * 1. GitHub OAuth Appの設定:
     *    - GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
     *    - Application name: StaticCMS (お好みの名前)
     *    - Homepage URL: https://github.com/your-username/StaticCMS (または任意のURL)
     *    - Authorization callback URL: http://localhost:8080/callback
     *
     * 2. OAuth App情報の設定:
     *    以下のいずれかの方法でOAuth情報を設定してください:
     *
     *    方法A) 環境変数で設定（推奨）:
     *    - GITHUB_CLIENT_ID=あなたのClient ID
     *    - GITHUB_CLIENT_SECRET=あなたのClient Secret
     *
     *    方法B) oauth.propertiesファイル（プロジェクトルートに配置）:
     *    github.client.id=あなたのClient ID
     *    github.client.secret=あなたのClient Secret
     *
     *    ※ oauth.propertiesファイルは.gitignoreに追加してください
     *
     * 3. セキュリティ注意事項:
     *    - Client Secretは秘密情報です。絶対にソースコードに直接書かないでください
     *    - oauth.propertiesファイルをGitにコミットしないよう注意してください
     */

    private var httpClient: HttpClient? = null
    private var personalAccessToken: String? = null

    private val _authenticationState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authenticationState: StateFlow<AuthState> = _authenticationState

    private val _currentUser = MutableStateFlow<GitHubUser?>(null)
    val currentUser: StateFlow<GitHubUser?> = _currentUser

    // OAuth設定を安全に取得
    private val clientId: String by lazy {
        getOAuthConfig("CLIENT_ID")
            ?: error(
                "GitHub Client ID not configured. Please set GITHUB_CLIENT_ID environment variable or create oauth.properties file.",
            )
    }
    private val clientSecret: String by lazy {
        getOAuthConfig("CLIENT_SECRET")
            ?: error(
                "GitHub Client Secret not configured. Please set GITHUB_CLIENT_SECRET environment variable or create oauth.properties file.",
            )
    }
    private val redirectUri = "http://localhost:8080/callback"
    private val scope = "repo"

    private var callbackServer: EmbeddedServer<*, *>? = null
    private var authResultDeferred: CompletableDeferred<String>? = null

    /** OAuth設定を環境変数またはプロパティファイルから取得 */
    private fun getOAuthConfig(key: String): String? {
        println("DEBUG: Looking for OAuth config key: $key")
        println("DEBUG: Current working directory: ${System.getProperty("user.dir")}")
        println("DEBUG: User home directory: ${System.getProperty("user.home")}")

        // 環境変数から取得を試行
        val envKey = "GITHUB_$key"
        val envValue = System.getenv(envKey)
        if (!envValue.isNullOrBlank()) {
            println("DEBUG: Found $envKey in environment variables")
            return envValue
        }

        // oauth.propertiesファイルを複数の場所で検索
        val possiblePaths =
            listOf(
                "oauth.properties",
                "./oauth.properties",
                "../oauth.properties",
                "../../oauth.properties",
                System.getProperty("user.dir") + "/oauth.properties",
                System.getProperty("user.home") + "/.staticcms/oauth.properties",
            )

        for (path in possiblePaths) {
            try {
                val propertiesFile = java.io.File(path)
                println("DEBUG: Checking for oauth.properties at: ${propertiesFile.absolutePath}")

                if (propertiesFile.exists()) {
                    println("DEBUG: Found oauth.properties at: ${propertiesFile.absolutePath}")
                    val properties = java.util.Properties()
                    propertiesFile.inputStream().use { properties.load(it) }

                    val propKey =
                        when (key) {
                            "CLIENT_ID" -> "github.client.id"
                            "CLIENT_SECRET" -> "github.client.secret"
                            else -> null
                        }

                    val propValue = propKey?.let { properties.getProperty(it) }
                    if (!propValue.isNullOrBlank()) {
                        println("DEBUG: Found $propKey = $propValue (first 10 chars)")
                        return propValue
                    } else {
                        println("DEBUG: Property $propKey not found or empty in file")
                    }
                } else {
                    println("DEBUG: File not found at: ${propertiesFile.absolutePath}")
                }
            } catch (e: Exception) {
                println("DEBUG: Error reading $path: ${e.message}")
            }
        }

        // リソースとしてクラスパスから読み込みを試行
        try {
            val resourceStream =
                this::class.java.classLoader.getResourceAsStream("oauth.properties")
            if (resourceStream != null) {
                println("DEBUG: Found oauth.properties in classpath resources")
                val properties = java.util.Properties()
                resourceStream.use { properties.load(it) }

                val propKey =
                    when (key) {
                        "CLIENT_ID" -> "github.client.id"
                        "CLIENT_SECRET" -> "github.client.secret"
                        else -> null
                    }

                val propValue = propKey?.let { properties.getProperty(it) }
                if (!propValue.isNullOrBlank()) {
                    println("DEBUG: Found $propKey in classpath resources")
                    return propValue
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error reading oauth.properties from classpath: ${e.message}")
        }

        println("DEBUG: OAuth config $key not found anywhere")
        return null
    }

    suspend fun authenticateWithBrowser(): Result<GitHubUser> {
        return try {
            _authenticationState.value = AuthState.Starting

            // HTTPクライアントを初期化（OAuth用）
            if (httpClient == null) {
                httpClient =
                    HttpClient(io.ktor.client.engine.cio.CIO) {
                        install(ContentNegotiation) {
                            json(
                                Json {
                                    ignoreUnknownKeys = true
                                    coerceInputValues = true
                                },
                            )
                        }
                    }
            }

            // コールバックサーバーを開始
            startCallbackServer()

            // ブラウザでOAuth認証を開始
            val authUrl = buildAuthUrl()
            openBrowser(authUrl)

            _authenticationState.value = AuthState.WaitingForUser

            // コールバックを待機
            authResultDeferred = CompletableDeferred()
            val authCode = authResultDeferred!!.await()

            _authenticationState.value = AuthState.Processing

            // 認証コードをアクセストークンに交換
            val tokenResponse = exchangeCodeForToken(authCode)
            personalAccessToken = tokenResponse.access_token

            // HTTPクライアントを再初期化（新しいトークンで）
            httpClient?.close()
            httpClient =
                HttpClient(io.ktor.client.engine.cio.CIO) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                coerceInputValues = true
                            },
                        )
                    }
                    install(Auth) {
                        bearer {
                            loadTokens {
                                BearerTokens(personalAccessToken!!, personalAccessToken!!)
                            }
                        }
                    }
                }

            // ユーザー情報を取得
            val user = getCurrentUser()
            _currentUser.value = user

            _authenticationState.value = AuthState.Success(user)
            stopCallbackServer()

            Result.success(user)
        } catch (e: Exception) {
            _authenticationState.value = AuthState.Error("Authentication failed: ${e.message}")
            stopCallbackServer()
            Result.failure(e)
        }
    }

    private fun buildAuthUrl(): String {
        val state = UUID.randomUUID().toString()
        return "https://github.com/login/oauth/authorize" +
            "?client_id=$clientId" +
            "&redirect_uri=$redirectUri" +
            "&scope=$scope" +
            "&state=$state"
    }

    private fun openBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                // フォールバック: URLを出力
                println("Please open this URL in your browser: $url")
            }
        } catch (e: Exception) {
            println("Failed to open browser. Please manually open: $url")
        }
    }

    private suspend fun startCallbackServer() {
        callbackServer =
            embeddedServer(io.ktor.server.cio.CIO, port = 8080) {
                    routing {
                        get("/callback") {
                            val code = call.request.queryParameters["code"]
                            val error = call.request.queryParameters["error"]

                            if (error != null) {
                                call.respondHtml {
                                    head {
                                        title { +"StaticCMS - Authentication Error" }
                                        style {
                                            unsafe {
                                                raw(
                                                    """
                                                body { font-family: 'MS Sans Serif', sans-serif; background: #c0c0c0; margin: 40px; }
                                                .window { background: #c0c0c0; border: 2px outset #c0c0c0; padding: 20px; max-width: 400px; margin: 0 auto; }
                                                .error { color: red; font-weight: bold; }
                                                .close-btn { margin-top: 20px; padding: 5px 15px; border: 2px outset #c0c0c0; background: #c0c0c0; }
                                                """
                                                        .trimIndent(),
                                                )
                                            }
                                        }
                                    }
                                    body {
                                        div("window") {
                                            h2 { +"Authentication Error" }
                                            p("error") { +"Error: $error" }
                                            p { +"You can close this window and try again." }
                                            button {
                                                classes = setOf("close-btn")
                                                +"Close Window"
                                            }
                                        }
                                    }
                                }
                                authResultDeferred?.completeExceptionally(
                                    Exception("OAuth error: $error"),
                                )
                            } else if (code != null) {
                                call.respondHtml {
                                    head {
                                        title { +"StaticCMS - Authentication Success" }
                                        style {
                                            unsafe {
                                                raw(
                                                    """
                                                body { font-family: 'MS Sans Serif', sans-serif; background: #c0c0c0; margin: 40px; }
                                                .window { background: #c0c0c0; border: 2px outset #c0c0c0; padding: 20px; max-width: 400px; margin: 0 auto; }
                                                .success { color: green; font-weight: bold; }
                                                .close-btn { margin-top: 20px; padding: 5px 15px; border: 2px outset #c0c0c0; background: #c0c0c0; }
                                                """
                                                        .trimIndent(),
                                                )
                                            }
                                        }
                                        script {
                                            unsafe {
                                                raw(
                                                    """
                                                // 3秒後に自動的にウィンドウを閉じる
                                                setTimeout(function() {
                                                    window.close();
                                                }, 3000);

                                                // 手動で閉じるボタンの機能
                                                function closeWindow() {
                                                    window.close();
                                                }
                                                """
                                                        .trimIndent(),
                                                )
                                            }
                                        }
                                    }
                                    body {
                                        div("window") {
                                            h2 { +"Authentication Successful" }
                                            p("success") {
                                                +"✓ GitHub authentication completed successfully!"
                                            }
                                            p {
                                                +"This window will close automatically in 3 seconds..."
                                            }
                                            p { +"Please return to StaticCMS." }
                                            button {
                                                classes = setOf("close-btn")
                                                onClick = "closeWindow()"
                                                +"Close Window Now"
                                            }
                                        }
                                    }
                                }
                                authResultDeferred?.complete(code)
                            } else {
                                call.respondText("Invalid callback - missing code parameter")
                                authResultDeferred?.completeExceptionally(
                                    Exception("Invalid callback"),
                                )
                            }
                        }
                    }
                }
                .start(wait = false)
    }

    private fun stopCallbackServer() {
        callbackServer?.stop(1000, 2000)
        callbackServer = null
    }

    private suspend fun exchangeCodeForToken(code: String): GitHubOAuthTokenResponse {
        val response =
            httpClient?.post("https://github.com/login/oauth/access_token") {
                headers {
                    append("Accept", "application/json")
                    append("Content-Type", "application/json")
                }
                setBody(
                    """
                    {
                        "client_id": "$clientId",
                        "client_secret": "$clientSecret",
                        "code": "$code",
                        "redirect_uri": "$redirectUri"
                    }
                    """
                        .trimIndent(),
                )
            } ?: throw Exception("HttpClient not initialized")
        return response.body()
    }

    suspend fun getCurrentUser(): GitHubUser {
        val response: HttpResponse =
            httpClient?.get("https://api.github.com/user") {
                headers {
                    append("Authorization", "Bearer $personalAccessToken")
                    append("Accept", "application/vnd.github.v3+json")
                    append("User-Agent", "StaticCMS/1.0")
                }
            } ?: throw Exception("HttpClient not initialized")
        return response.body()
    }

    suspend fun getUserRepositories(perPage: Int = 100): Result<List<GitHubRepository>> {
        return try {
            val response =
                httpClient?.get("https://api.github.com/user/repos") {
                    headers {
                        append("Authorization", "Bearer $personalAccessToken")
                        append("Accept", "application/vnd.github.v3+json")
                        append("User-Agent", "StaticCMS/1.0")
                    }
                    parameter("type", "all") // all, owner, public, private, member
                    parameter("sort", "updated") // created, updated, pushed, full_name
                    parameter("direction", "desc") // asc, desc
                    parameter("per_page", perPage)
                } ?: throw Exception("HttpClient not initialized")

            val repositories: List<GitHubRepository> = response.body()
            Result.success(repositories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasWritePermission(
        owner: String,
        repo: String,
    ): Result<Boolean> {
        return try {
            val response =
                httpClient?.get("https://api.github.com/repos/$owner/$repo") {
                    headers {
                        append("Authorization", "Bearer $personalAccessToken")
                        append("Accept", "application/vnd.github.v3+json")
                        append("User-Agent", "StaticCMS/1.0")
                    }
                } ?: throw Exception("HttpClient not initialized")
            val repository: GitHubRepository = response.body()
            val hasPermission = repository.permissions?.push == true
            Result.success(hasPermission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCommit(
        owner: String,
        repo: String,
        message: String,
        tree: String,
        parents: List<String>,
    ): Result<GitHubCommit> {
        return try {
            val response =
                httpClient?.post("https://api.github.com/repos/$owner/$repo/git/commits") {
                    headers {
                        append("Authorization", "Bearer $personalAccessToken")
                        append("Accept", "application/vnd.github.v3+json")
                        append("Content-Type", "application/json")
                        append("User-Agent", "StaticCMS/1.0")
                    }
                    setBody(
                        """
                        {
                            "message": "$message",
                            "tree": "$tree",
                            "parents": [${parents.joinToString(",") { "\"$it\"" }}]
                        }
                        """
                            .trimIndent(),
                    )
                } ?: throw Exception("HttpClient not initialized")
            val commit: GitHubCommit = response.body()
            Result.success(commit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getToken(): String? = personalAccessToken

    fun isAuthenticated(): Boolean = personalAccessToken != null

    fun close() {
        stopCallbackServer()
        httpClient?.close()
        httpClient = null
        personalAccessToken = null
        _authenticationState.value = AuthState.Idle
        _currentUser.value = null
    }
}
