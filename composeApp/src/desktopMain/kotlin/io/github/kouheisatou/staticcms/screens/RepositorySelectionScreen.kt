package io.github.kouheisatou.staticcms.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kouheisatou.staticcms.model.GitHubRepository
import io.github.kouheisatou.staticcms.ui.components.*
import io.github.kouheisatou.staticcms.ui.theme.RetroColors
import io.github.kouheisatou.staticcms.ui.theme.RetroTypography
import kotlinx.coroutines.delay

@Composable
fun RepositorySelectionScreen(
    repositories: List<GitHubRepository>,
    isLoading: Boolean,
    onRepositorySelected: (GitHubRepository) -> Unit,
    onRefresh: () -> Unit,
) {
    // Mutable state for animated loading progress
    var animatedProgress by remember { mutableStateOf(0f) }
    var loadingMessage by remember { mutableStateOf("Loading repositories...") }

    // Animate loading progress when isLoading is true
    LaunchedEffect(isLoading) {
        if (isLoading) {
            animatedProgress = 0f
            loadingMessage = "Connecting to GitHub..."

            // Simulate loading phases with different messages
            val phases =
                listOf(
                    "Connecting to GitHub..." to 0.2f,
                    "Fetching repositories..." to 0.6f,
                    "Processing repository data..." to 0.9f,
                    "Almost done..." to 1.0f)

            for ((message, targetProgress) in phases) {
                loadingMessage = message

                // Animate to target progress
                val startProgress = animatedProgress
                val steps = 10
                for (i in 1..steps) {
                    val progress =
                        startProgress + (targetProgress - startProgress) * (i.toFloat() / steps)
                    animatedProgress = progress
                    delay(100)
                }

                delay(300) // Brief pause between phases
            }
        } else {
            animatedProgress = 0f
        }
    }

    RetroWindow(
        title = "StaticCMS - Select Repository",
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Select Repository",
                        style = RetroTypography.Default.copy(fontSize = 16.sp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        text = "Choose a repository to manage with StaticCMS",
                        style = RetroTypography.Default.copy(fontSize = 10.sp),
                        color = RetroColors.DisabledText,
                    )
                }

                RetroTextButton(
                    text = "🔄 Refresh",
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.height(32.dp),
                )
            }

            if (isLoading) {
                // Animated loading state with mutable state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = loadingMessage,
                        style = RetroTypography.Default,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(0.4f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        RetroProgressBar(
                            progress = animatedProgress,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        )

                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = RetroTypography.Default.copy(fontSize = 10.sp),
                            color = RetroColors.DisabledText,
                        )
                    }
                }
            } else if (repositories.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "📁 No repositories found",
                        style = RetroTypography.Default.copy(fontSize = 14.sp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        text = "Create a repository on GitHub first, then refresh the list.",
                        style = RetroTypography.Default.copy(fontSize = 10.sp),
                        color = RetroColors.DisabledText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    RetroTextButton(
                        text = "🔄 Refresh",
                        onClick = onRefresh,
                        modifier = Modifier.height(40.dp),
                    )
                }
            } else {
                // Repository list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(repositories) { repository ->
                        RepositoryCard(
                            repository = repository,
                            onSelect = { onRepositorySelected(repository) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryCard(
    repository: GitHubRepository,
    onSelect: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    RetroCard(
        modifier =
            Modifier.fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            val released = tryAwaitRelease()
                            pressed = false
                            if (released) {
                                onSelect()
                            }
                        },
                    )
                }
                .padding(4.dp),
        pressed = pressed,
    ) {
        Column(
            modifier =
                Modifier.padding(12.dp)
                    .then(if (pressed) Modifier.offset(1.dp, 1.dp) else Modifier),
        ) {
            // Repository name and privacy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (repository.private) "🔒" else "📂",
                        style = RetroTypography.Default.copy(fontSize = 12.sp),
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text(
                        text = repository.name,
                        style = RetroTypography.Default.copy(fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repository.language?.let { language ->
                        Text(
                            text = language,
                            style = RetroTypography.Default.copy(fontSize = 9.sp),
                            color = RetroColors.DisabledText,
                        )
                    }

                    if (repository.stargazers_count > 0) {
                        Text(
                            text = "⭐ ${repository.stargazers_count}",
                            style = RetroTypography.Default.copy(fontSize = 9.sp),
                            color = RetroColors.DisabledText,
                        )
                    }
                }
            }

            // Full name
            Text(
                text = repository.full_name,
                style = RetroTypography.Default.copy(fontSize = 9.sp),
                color = RetroColors.DisabledText,
                modifier = Modifier.padding(top = 2.dp, start = 18.dp),
            )

            // Description
            repository.description?.let { description ->
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = RetroTypography.Default.copy(fontSize = 10.sp),
                        color = RetroColors.DisabledText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            // Updated time and permissions
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repository.updated_at?.let { updatedAt ->
                    Text(
                        text = "Updated: ${formatDate(updatedAt)}",
                        style = RetroTypography.Default.copy(fontSize = 8.sp),
                        color = RetroColors.DisabledText,
                    )
                }

                // Permission indicator
                val hasWritePermission = repository.permissions?.push == true
                Text(
                    text = if (hasWritePermission) "✓ Write" else "👁 Read",
                    style = RetroTypography.Default.copy(fontSize = 8.sp),
                    color =
                        if (hasWritePermission) {
                            RetroColors.TitleBarActive
                        } else {
                            RetroColors.DisabledText
                        },
                )
            }
        }
    }
}

@Composable
private fun RetroCard(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.background(Color.White).border(2.dp, RetroColors.ButtonDarkShadow),
    ) {
        content()
    }
}

private fun formatDate(dateString: String): String {
    // 簡単な日付フォーマット（実際の実装ではより正確なパースが必要）
    return try {
        dateString.substring(0, 10) // "2023-12-01T12:00:00Z" -> "2023-12-01"
    } catch (e: Exception) {
        dateString
    }
}
