package io.github.kouheisatou.static_cms.util

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    @SerialName("avatar_url") val avatarUrl: String,
    val name: String?,
    val email: String?
)

@Serializable
data class GitHubRepo(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("clone_url") val cloneUrl: String,
    @SerialName("ssh_url") val sshUrl: String,
    val permissions: GitHubPermissions?
)

@Serializable
data class GitHubPermissions(
    val admin: Boolean,
    val maintain: Boolean?,
    val push: Boolean,
    val triage: Boolean?,
    val pull: Boolean
)

@Serializable
data class GitHubCommit(
    val sha: String,
    val message: String,
    val author: GitHubAuthor,
    val committer: GitHubAuthor
)

@Serializable
data class GitHubAuthor(
    val name: String,
    val email: String,
    val date: String
)

@Serializable
data class CreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String>,
    val author: GitHubAuthor,
    val committer: GitHubAuthor
)

@Serializable
data class UpdateRefRequest(
    val sha: String,
    val force: Boolean = false
)

class GitHubApiClient {
    private var httpClient: HttpClient? = null
    private var personalAccessToken: String? = null
    
    private val _authenticationState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authenticationState: StateFlow<AuthState> = _authenticationState
    
    private val _currentUser = MutableStateFlow<GitHubUser?>(null)
    val currentUser: StateFlow<GitHubUser?> = _currentUser

    sealed class AuthState {
        object NotAuthenticated : AuthState()
        object Authenticating : AuthState()
        data class Authenticated(val user: GitHubUser) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    fun initialize(token: String) {
        personalAccessToken = token
        httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(token, "")
                    }
                }
            }
        }
    }

    suspend fun authenticate(): Result<GitHubUser> {
        return try {
            _authenticationState.value = AuthState.Authenticating
            
            val client = httpClient ?: return Result.failure(Exception("Client not initialized"))
            
            val response: HttpResponse = client.get("https://api.github.com/user") {
                header("Accept", "application/vnd.github.v3+json")
                header("User-Agent", "StaticCMS/1.0")
            }
            if (response.status.isSuccess()) {
                val user: GitHubUser = response.body()
                _currentUser.value = user
                _authenticationState.value = AuthState.Authenticated(user)
                Result.success(user)
            } else {
                val error = "Authentication failed: ${response.status}"
                _authenticationState.value = AuthState.Error(error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            val error = "Authentication error: ${e.message}"
            _authenticationState.value = AuthState.Error(error)
            Result.failure(e)
        }
    }

    suspend fun getRepository(owner: String, repo: String): Result<GitHubRepo> {
        return try {
            val client = httpClient ?: return Result.failure(Exception("Client not initialized"))
            
            val response: HttpResponse = client.get("https://api.github.com/repos/$owner/$repo") {
                header("Accept", "application/vnd.github.v3+json")
                header("User-Agent", "StaticCMS/1.0")
            }
            if (response.status.isSuccess()) {
                val repository: GitHubRepo = response.body()
                Result.success(repository)
            } else {
                Result.failure(Exception("Failed to get repository: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasWritePermission(owner: String, repo: String): Result<Boolean> {
        return try {
            val repoResult = getRepository(owner, repo)
            if (repoResult.isSuccess) {
                val repository = repoResult.getOrThrow()
                val hasPermission = repository.permissions?.push == true || repository.permissions?.admin == true
                Result.success(hasPermission)
            } else {
                Result.failure(repoResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCommit(
        owner: String, 
        repo: String, 
        message: String, 
        treeSha: String, 
        parentSha: String,
        authorName: String,
        authorEmail: String
    ): Result<GitHubCommit> {
        return try {
            val client = httpClient ?: return Result.failure(Exception("Client not initialized"))
            
            val currentTime = java.time.Instant.now().toString()
            val author = GitHubAuthor(authorName, authorEmail, currentTime)
            
            val commitRequest = CreateCommitRequest(
                message = message,
                tree = treeSha,
                parents = listOf(parentSha),
                author = author,
                committer = author
            )
            
            val response: HttpResponse = client.post("https://api.github.com/repos/$owner/$repo/git/commits") {
                header("Accept", "application/vnd.github.v3+json")
                header("User-Agent", "StaticCMS/1.0")
                contentType(ContentType.Application.Json)
                setBody(commitRequest)
            }
            
            if (response.status.isSuccess()) {
                val commit: GitHubCommit = response.body()
                Result.success(commit)
            } else {
                Result.failure(Exception("Failed to create commit: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRef(owner: String, repo: String, ref: String, sha: String): Result<Unit> {
        return try {
            val client = httpClient ?: return Result.failure(Exception("Client not initialized"))
            
            val updateRequest = UpdateRefRequest(sha)
            
            val response: HttpResponse = client.patch("https://api.github.com/repos/$owner/$repo/git/refs/$ref") {
                header("Accept", "application/vnd.github.v3+json")
                header("User-Agent", "StaticCMS/1.0")
                contentType(ContentType.Application.Json)
                setBody(updateRequest)
            }
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update ref: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        httpClient?.close()
        httpClient = null
        personalAccessToken = null
        _authenticationState.value = AuthState.NotAuthenticated
        _currentUser.value = null
    }
} 