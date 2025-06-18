package io.github.kouheisatou.static_cms.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kouheisatou.static_cms.ui.components.*
import io.github.kouheisatou.static_cms.ui.theme.RetroTypography
import androidx.compose.material3.Text

@Composable
fun CloneProgressScreen(
    repositoryUrl: String,
    progress: Float
) {
    RetroWindow(
        title = "StaticCMS - Cloning Repository",
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Cloning Repository...",
                style = RetroTypography.Default.copy(fontSize = 16.sp),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Repository URL
            Text(
                text = repositoryUrl,
                style = RetroTypography.Default.copy(fontSize = 12.sp),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Progress bar
            Column(
                modifier = Modifier.fillMaxWidth(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RetroProgressBar(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                // Progress percentage
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = RetroTypography.Default
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Status messages based on progress
            val statusMessage = when {
                progress < 0.2f -> "Connecting to repository..."
                progress < 0.4f -> "Downloading objects..."
                progress < 0.6f -> "Receiving objects..."
                progress < 0.8f -> "Resolving deltas..."
                progress < 0.95f -> "Checking out files..."
                else -> "Almost done..."
            }
            
            Text(
                text = statusMessage,
                style = RetroTypography.Default.copy(
                    color = io.github.kouheisatou.static_cms.ui.theme.RetroColors.DisabledText
                ),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
} 