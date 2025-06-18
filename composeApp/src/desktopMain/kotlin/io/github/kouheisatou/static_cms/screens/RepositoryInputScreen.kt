package io.github.kouheisatou.static_cms.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kouheisatou.static_cms.ui.components.*
import io.github.kouheisatou.static_cms.ui.theme.RetroTypography
import androidx.compose.material3.Text
import io.github.kouheisatou.static_cms.util.FileOperations

@Composable
fun RepositoryInputScreen(
    repositoryUrl: String,
    onRepositoryUrlChange: (String) -> Unit,
    onCloneClick: () -> Unit,
    onSelectLocalDirectory: () -> Unit
) {
    RetroWindow(
        title = "StaticCMS - Repository Selection",
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Welcome to StaticCMS",
                style = RetroTypography.Default.copy(fontSize = 16.sp),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Repository URL input section
            Column(
                modifier = Modifier.fillMaxWidth(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Git Repository URL:",
                    style = RetroTypography.Default,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                
                RetroTextField(
                    value = repositoryUrl,
                    onValueChange = onRepositoryUrlChange,
                    placeholder = "https://github.com/username/repository.git",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                RetroTextButton(
                    text = "Clone Repository",
                    onClick = onCloneClick,
                    enabled = repositoryUrl.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(bottom = 32.dp)
                )
            }
            
            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(2.dp)
                    .background(io.github.kouheisatou.static_cms.ui.theme.RetroColors.ButtonShadow)
                    .padding(vertical = 16.dp)
            )
            
            // Local directory section
            Column(
                modifier = Modifier.fillMaxWidth(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Or select a local directory:",
                    style = RetroTypography.Default,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                RetroTextButton(
                    text = "Browse Local Directory",
                    onClick = onSelectLocalDirectory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Instructions
            Text(
                text = "Select a repository containing a 'contents' or 'sample_contents' directory",
                style = RetroTypography.Default.copy(
                    color = io.github.kouheisatou.static_cms.ui.theme.RetroColors.DisabledText,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
} 