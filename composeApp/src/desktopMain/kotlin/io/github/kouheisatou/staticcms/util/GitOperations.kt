package io.github.kouheisatou.staticcms.util

import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

class GitOperations {
    // Constants for better maintainability
    private object ProgressConstants {
        const val INIT = 0.05f
        const val CONNECT = 0.1f
        const val RECEIVE_END = 0.5f
        const val RESOLVE_END = 0.8f
        const val CHECKOUT_END = 0.95f
        const val COMMIT_STAGE = 0.3f
        const val COMMIT_COMPLETE = 0.6f
        const val PUSH_START = 0.7f
    }

    // Sealed class for operation states
    sealed class OperationState {
        object Idle : OperationState()

        data class Cloning(val progress: Float) : OperationState()

        data class Committing(val message: String) : OperationState()

        data class Pushing(val progress: Float) : OperationState()

        data class Success(val message: String) : OperationState()

        data class Error(val message: String) : OperationState()
    }

    // Thread-safe state management
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private val _operationProgress = MutableStateFlow(0f)
    val operationProgress: StateFlow<Float> = _operationProgress.asStateFlow()

    // Coroutine scopes for different contexts
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Debug logging for progress monitoring
        uiScope.launch {
            operationProgress.collect { progress ->
                println("DEBUG: GitOperations progress: $progress")
            }
        }
    }

    /** Clone a repository with progress tracking */
    suspend fun cloneRepository(
        repositoryUrl: String,
        destinationPath: String,
        username: String,
        token: String,
    ): Result<Git> {
        return withContext(Dispatchers.IO) {
            try {
                updateStateAndProgress(OperationState.Cloning(0f), 0f)

                val destinationDir = prepareDestinationDirectory(destinationPath)
                val credentialsProvider = createCredentialsProvider(username, token)

                val git =
                    Git.cloneRepository()
                        .setURI(repositoryUrl)
                        .setDirectory(destinationDir)
                        .setCredentialsProvider(credentialsProvider)
                        .setProgressMonitor(CloneProgressMonitor())
                        .call()

                updateStateAndProgress(OperationState.Success("Repository cloned successfully"), 1f)
                Result.success(git)
            } catch (e: Exception) {
                updateStateAndProgress(OperationState.Error("Clone failed: ${e.message}"), 0f)
                Result.failure(e)
            }
        }
    }

    /** Commit and push changes with progress tracking */
    suspend fun commitAndPush(
        git: Git,
        commitMessage: String,
        username: String,
        email: String,
        token: String,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                updateStateAndProgress(OperationState.Committing(commitMessage), 0f)

                // Stage changes
                git.add().addFilepattern(".").call()
                updateProgress(ProgressConstants.COMMIT_STAGE)

                // Create commit
                git.commit()
                    .setMessage(commitMessage)
                    .setAuthor(username, email)
                    .setCommitter(username, email)
                    .call()

                updateStateAndProgress(
                    OperationState.Pushing(ProgressConstants.COMMIT_COMPLETE),
                    ProgressConstants.COMMIT_COMPLETE)

                // Push changes
                val credentialsProvider = createCredentialsProvider(username, token)
                git.push()
                    .setCredentialsProvider(credentialsProvider)
                    .setProgressMonitor(PushProgressMonitor())
                    .call()

                updateStateAndProgress(
                    OperationState.Success("Changes committed and pushed successfully"), 1f)
                Result.success(Unit)
            } catch (e: Exception) {
                updateStateAndProgress(OperationState.Error("Commit/Push failed: ${e.message}"), 0f)
                Result.failure(e)
            }
        }
    }

    /** Add a file to the Git repository */
    suspend fun addFile(git: Git, filePath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                git.add().addFilepattern(filePath).call()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** Get repository status */
    suspend fun getStatus(git: Git): Result<org.eclipse.jgit.api.Status> {
        return withContext(Dispatchers.IO) {
            try {
                val status = git.status().call()
                Result.success(status)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** Parse repository URL to extract owner and repo name */
    fun parseRepositoryUrl(url: String): Pair<String, String>? {
        return try {
            val regex = Regex("https://github\\.com/([^/]+)/([^/.]+)(?:\\.git)?/?")
            val matchResult = regex.find(url)
            matchResult?.let {
                val owner = it.groupValues[1]
                val repo = it.groupValues[2]
                Pair(owner, repo)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Check if there are unpushed changes */
    suspend fun hasUnpushedChanges(git: Git): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Get local and remote commit IDs
                val localHead = git.repository.resolve("HEAD")
                val remoteHead =
                    git.repository.resolve("origin/main") ?: git.repository.resolve("origin/master")

                // If remote head is null, assume we have unpushed changes
                if (remoteHead == null) return@withContext true

                // Check if local head is ahead of remote head
                localHead != remoteHead
            } catch (e: Exception) {
                println("Error checking unpushed changes: ${e.message}")
                false
            }
        }
    }

    /** Reset operation state */
    fun reset() {
        uiScope.launch {
            _operationState.value = OperationState.Idle
            _operationProgress.value = 0f
        }
    }

    /** Dispose resources */
    fun dispose() {
        backgroundScope.cancel()
        uiScope.cancel()
    }

    // Private helper methods

    private fun prepareDestinationDirectory(destinationPath: String): File {
        val destinationDir = File(destinationPath)
        if (destinationDir.exists()) {
            destinationDir.deleteRecursively()
        }
        destinationDir.mkdirs()
        return destinationDir
    }

    private fun createCredentialsProvider(username: String, token: String) =
        UsernamePasswordCredentialsProvider(username, token)

    private fun updateStateAndProgress(state: OperationState, progress: Float) {
        uiScope.launch {
            _operationState.value = state
            _operationProgress.value = progress
        }
    }

    private fun updateProgress(progress: Float) {
        uiScope.launch { _operationProgress.value = progress }
    }

    private fun updateState(state: OperationState) {
        uiScope.launch { _operationState.value = state }
    }

    // Progress monitor implementations

    private inner class CloneProgressMonitor : ProgressMonitor {
        private var currentTaskProgress = 0f
        private var currentPhase = ""
        private var totalWork = 0
        private var completedWork = 0

        override fun start(totalTasks: Int) {
            updateProgress(ProgressConstants.INIT)
        }

        override fun beginTask(title: String?, totalWork: Int) {
            currentPhase = title ?: ""
            this.totalWork = totalWork
            this.completedWork = 0

            currentTaskProgress = getPhaseStartProgress(currentPhase)
            updateProgress(currentTaskProgress)

            println("DEBUG: Git task started - '$title', totalWork: $totalWork")
        }

        override fun update(completed: Int) {
            this.completedWork += completed

            val taskProgress =
                if (totalWork > 0) {
                    (completedWork.toFloat() / totalWork.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

            val overallProgress = calculateOverallProgress(currentPhase, taskProgress)
            updateProgress(overallProgress)

            // Throttled logging
            if (completed > 0 && completedWork % 50 == 0) {
                println(
                    "DEBUG: Git progress - Phase: '$currentPhase', completed: $completedWork/$totalWork (${(taskProgress * 100).toInt()}%), overall: ${(overallProgress * 100).toInt()}%")
            }
        }

        override fun endTask() {
            val endProgress = getPhaseEndProgress(currentPhase)
            updateProgress(endProgress)
            println(
                "DEBUG: Git task completed - '$currentPhase', final progress: ${(endProgress * 100).toInt()}%")
        }

        override fun isCancelled(): Boolean = false

        override fun showDuration(enabled: Boolean) {}

        private fun getPhaseStartProgress(phase: String): Float =
            when {
                phase.contains("Receiving objects") -> ProgressConstants.CONNECT
                phase.contains("Resolving deltas") -> ProgressConstants.RECEIVE_END
                phase.contains("Checking out files") -> ProgressConstants.RESOLVE_END
                else -> ProgressConstants.INIT
            }

        private fun getPhaseEndProgress(phase: String): Float =
            when {
                phase.contains("Receiving objects") -> ProgressConstants.RECEIVE_END
                phase.contains("Resolving deltas") -> ProgressConstants.RESOLVE_END
                phase.contains("Checking out files") -> ProgressConstants.CHECKOUT_END
                else -> (currentTaskProgress + 0.1f).coerceIn(0f, ProgressConstants.CHECKOUT_END)
            }

        private fun calculateOverallProgress(phase: String, taskProgress: Float): Float {
            return when {
                phase.contains("Receiving objects") -> {
                    ProgressConstants.CONNECT +
                        (taskProgress * (ProgressConstants.RECEIVE_END - ProgressConstants.CONNECT))
                }
                phase.contains("Resolving deltas") -> {
                    ProgressConstants.RECEIVE_END +
                        (taskProgress *
                            (ProgressConstants.RESOLVE_END - ProgressConstants.RECEIVE_END))
                }
                phase.contains("Checking out files") -> {
                    ProgressConstants.RESOLVE_END +
                        (taskProgress *
                            (ProgressConstants.CHECKOUT_END - ProgressConstants.RESOLVE_END))
                }
                else -> {
                    currentTaskProgress + (taskProgress * 0.05f)
                }
            }.coerceIn(0f, ProgressConstants.CHECKOUT_END)
        }
    }

    private inner class PushProgressMonitor : ProgressMonitor {
        override fun start(totalTasks: Int) {
            updateStateAndProgress(
                OperationState.Pushing(ProgressConstants.PUSH_START), ProgressConstants.PUSH_START)
        }

        override fun beginTask(title: String?, totalWork: Int) {
            // Push operation started
        }

        override fun update(completed: Int) {
            val progress = ProgressConstants.PUSH_START + (completed.toFloat() / 100f * 0.3f)
            updateStateAndProgress(OperationState.Pushing(progress), progress)
        }

        override fun endTask() {
            // Push completed
        }

        override fun isCancelled(): Boolean = false

        override fun showDuration(enabled: Boolean) {}
    }
}
