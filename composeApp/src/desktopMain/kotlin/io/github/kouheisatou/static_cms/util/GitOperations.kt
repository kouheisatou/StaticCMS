package io.github.kouheisatou.static_cms.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitOperations {
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState
    
    private val _operationProgress = MutableStateFlow(0f)
    val operationProgress: StateFlow<Float> = _operationProgress

    sealed class OperationState {
        object Idle : OperationState()
        data class Cloning(val progress: Float) : OperationState()
        data class Committing(val message: String) : OperationState()
        data class Pushing(val progress: Float) : OperationState()
        data class Success(val message: String) : OperationState()
        data class Error(val message: String) : OperationState()
    }

    suspend fun cloneRepository(
        repositoryUrl: String,
        destinationPath: String,
        username: String,
        token: String
    ): Result<Git> {
        return try {
            _operationState.value = OperationState.Cloning(0f)
            _operationProgress.value = 0f
            
            println("DEBUG: Starting clone to $destinationPath")
            
            val destinationDir = File(destinationPath)
            if (destinationDir.exists()) {
                println("DEBUG: Destination directory exists, deleting...")
                destinationDir.deleteRecursively()
            }
            destinationDir.mkdirs()
            println("DEBUG: Created destination directory")

            val credentialsProvider = UsernamePasswordCredentialsProvider(username, token)
            
            var totalWork = 0
            var currentWork = 0
            
            // プログレス監視付きクローン
            val git = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(destinationDir)
                .setCredentialsProvider(credentialsProvider)
                .setProgressMonitor(object : org.eclipse.jgit.lib.ProgressMonitor {
                    private var taskTotalWork = 0
                    private var taskCurrentWork = 0
                    
                    override fun start(totalTasks: Int) {
                        println("DEBUG: Git clone started with $totalTasks tasks")
                        _operationProgress.value = 0.1f
                        _operationState.value = OperationState.Cloning(0.1f)
                    }
                    
                    override fun beginTask(title: String?, totalWork: Int) {
                        println("DEBUG: Git task started: $title (totalWork: $totalWork)")
                        taskTotalWork = totalWork
                        taskCurrentWork = 0
                        if (totalWork > 0) {
                            val baseProgress = when {
                                title?.contains("Receiving objects") == true -> 0.1f
                                title?.contains("Resolving deltas") == true -> 0.6f
                                title?.contains("Checking out files") == true -> 0.8f
                                else -> 0.1f
                            }
                            _operationProgress.value = baseProgress
                            _operationState.value = OperationState.Cloning(baseProgress)
                        }
                    }
                    
                    override fun update(completed: Int) {
                        taskCurrentWork += completed
                        if (taskTotalWork > 0) {
                            val taskProgress = (taskCurrentWork.toFloat() / taskTotalWork.toFloat()).coerceIn(0f, 1f)
                            val overallProgress = when {
                                taskCurrentWork <= taskTotalWork * 0.5 -> 0.1f + (taskProgress * 0.5f) // 10-60%
                                taskCurrentWork <= taskTotalWork * 0.8 -> 0.6f + (taskProgress * 0.2f) // 60-80%
                                else -> 0.8f + (taskProgress * 0.15f) // 80-95%
                            }
                            _operationProgress.value = overallProgress.coerceIn(0f, 0.95f)
                            _operationState.value = OperationState.Cloning(overallProgress)
                            println("DEBUG: Git progress: $taskCurrentWork/$taskTotalWork = ${overallProgress * 100}%")
                        } else {
                            // totalWork不明の場合は段階的に進捗を更新
                            val currentProgress = _operationProgress.value
                            val newProgress = (currentProgress + 0.01f).coerceIn(0f, 0.9f)
                            _operationProgress.value = newProgress
                            _operationState.value = OperationState.Cloning(newProgress)
                            println("DEBUG: Git progress (incremental): ${newProgress * 100}%")
                        }
                    }
                    
                    override fun endTask() {
                        println("DEBUG: Git task ended")
                        val currentProgress = _operationProgress.value
                        val newProgress = (currentProgress + 0.1f).coerceIn(0f, 0.95f)
                        _operationProgress.value = newProgress
                        _operationState.value = OperationState.Cloning(newProgress)
                    }
                    
                    override fun isCancelled(): Boolean = false
                    
                    override fun showDuration(enabled: Boolean) {
                        // JGitのインターフェース要件
                    }
                })
                .call()

            println("DEBUG: Git clone completed successfully")
            _operationProgress.value = 1f
            _operationState.value = OperationState.Success("Repository cloned successfully")
            
            Result.success(git)
        } catch (e: Exception) {
            println("ERROR: Git clone failed: ${e.message}")
            e.printStackTrace()
            _operationState.value = OperationState.Error("Clone failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun commitAndPush(
        git: Git,
        commitMessage: String,
        username: String,
        email: String,
        token: String
    ): Result<Unit> {
        return try {
            _operationState.value = OperationState.Committing(commitMessage)
            _operationProgress.value = 0f

            // 変更をステージング
            git.add().addFilepattern(".").call()
            _operationProgress.value = 0.3f

            // コミット作成
            git.commit()
                .setMessage(commitMessage)
                .setAuthor(username, email)
                .setCommitter(username, email)
                .call()
            
            _operationProgress.value = 0.6f
            _operationState.value = OperationState.Pushing(0.6f)

            // プッシュ
            val credentialsProvider = UsernamePasswordCredentialsProvider(username, token)
            
            git.push()
                .setCredentialsProvider(credentialsProvider)
                .setProgressMonitor(object : org.eclipse.jgit.lib.ProgressMonitor {
                    override fun start(totalTasks: Int) {
                        _operationProgress.value = 0.7f
                        _operationState.value = OperationState.Pushing(0.7f)
                    }
                    
                    override fun beginTask(title: String?, totalWork: Int) {
                        println("Push Operation: $title")
                    }
                    
                    override fun update(completed: Int) {
                        val progress = 0.7f + (completed.toFloat() / 100f * 0.3f)
                        _operationProgress.value = progress
                        _operationState.value = OperationState.Pushing(progress)
                    }
                    
                    override fun endTask() {
                        // プッシュ完了
                    }
                    
                    override fun isCancelled(): Boolean = false
                    
                    override fun showDuration(enabled: Boolean) {
                        // JGitのインターフェース要件
                    }
                })
                .call()

            _operationProgress.value = 1f
            _operationState.value = OperationState.Success("Changes committed and pushed successfully")
            
            Result.success(Unit)
        } catch (e: Exception) {
            _operationState.value = OperationState.Error("Commit/Push failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addFile(git: Git, filePath: String): Result<Unit> {
        return try {
            git.add().addFilepattern(filePath).call()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStatus(git: Git): Result<org.eclipse.jgit.api.Status> {
        return try {
            val status = git.status().call()
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseRepositoryUrl(url: String): Pair<String, String>? {
        return try {
            // https://github.com/owner/repo.git or https://github.com/owner/repo
            val regex = Regex("https://github\\.com/([^/]+)/([^/.]+)(?:\\.git)?/?")
            val matchResult = regex.find(url)
            if (matchResult != null) {
                val owner = matchResult.groupValues[1]
                val repo = matchResult.groupValues[2]
                Pair(owner, repo)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun reset() {
        _operationState.value = OperationState.Idle
        _operationProgress.value = 0f
    }
} 