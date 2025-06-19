package io.github.kouheisatou.staticcms.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kouheisatou.staticcms.ui.components.*
import io.github.kouheisatou.staticcms.ui.theme.RetroTypography
import kotlinx.coroutines.delay

@Composable
fun CloneProgressScreen(
    repositoryUrl: String,
    progress: Float,
) {
    // Mutable state for animated progress and status messages
    var animatedProgress by remember { mutableStateOf(0f) }
    var statusMessage by remember { mutableStateOf("Initializing clone...") }
    var isCompleted by remember { mutableStateOf(false) }

    // Animate progress updates
    LaunchedEffect(progress) {
        // Smooth animation of progress
        val startProgress = animatedProgress
        val targetProgress = progress.coerceIn(0f, 1f)
        
        if (targetProgress > startProgress) {
            val steps = 20
            val stepDuration = 50L
            
            for (i in 1..steps) {
                val currentProgress = startProgress + (targetProgress - startProgress) * (i.toFloat() / steps)
                animatedProgress = currentProgress
                
                // Update status message based on progress
                statusMessage = when {
                    currentProgress <= 0.05f -> "Initializing clone..."
                    currentProgress <= 0.1f -> "Connecting to remote repository..."
                    currentProgress <= 0.5f -> "Receiving objects..."
                    currentProgress <= 0.8f -> "Resolving deltas..."
                    currentProgress < 1.0f -> "Checking out files..."
                    else -> "Clone completed!"
                }
                
                delay(stepDuration)
            }
        } else {
            animatedProgress = targetProgress
        }
        
        isCompleted = targetProgress >= 1.0f
    }

    RetroWindow(
        title = "StaticCMS - Cloning Repository",
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Title
            Text(
                text = if (isCompleted) "Clone Completed!" else "Cloning Repository...",
                style = RetroTypography.Default.copy(fontSize = 16.sp),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Repository URL
            Text(
                text = repositoryUrl,
                style = RetroTypography.Default.copy(fontSize = 12.sp),
                modifier = Modifier.padding(bottom = 32.dp),
            )

            // Progress bar with animated progress
            Column(
                modifier = Modifier.fillMaxWidth(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RetroProgressBar(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )

                // Progress percentage
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = RetroTypography.Default,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status message with animation
            Text(
                text = statusMessage,
                style =
                    RetroTypography.Default.copy(
                        color = io.github.kouheisatou.staticcms.ui.theme.RetroColors.DisabledText,
                    ),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
