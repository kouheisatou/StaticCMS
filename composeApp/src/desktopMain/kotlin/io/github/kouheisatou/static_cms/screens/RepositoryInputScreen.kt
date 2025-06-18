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
    onCloneClick: () -> Unit
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
                
                // デバッグ情報追加
                val isEnabled = repositoryUrl.isNotBlank()
                println("DEBUG: RepositoryInputScreen - repositoryUrl='$repositoryUrl', isEnabled=$isEnabled")
                
                // テスト用ボタン
                RetroTextButton(
                    text = "Use Test URL",
                    onClick = {
                        println("DEBUG: Test URL button clicked")
                        onRepositoryUrlChange("https://github.com/octocat/Hello-World.git")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(bottom = 8.dp)
                )
                
                RetroTextButton(
                    text = "Clone Repository",
                    onClick = {
                        println("DEBUG: RepositoryInputScreen - Clone Repository button clicked!")
                        println("DEBUG: About to call onCloneClick()")
                        onCloneClick()
                        println("DEBUG: onCloneClick() returned")
                    },
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(bottom = 16.dp)
                )
                
                // デバッグ用の状態表示
                Text(
                    text = "Button enabled: $isEnabled | URL length: ${repositoryUrl.length}",
                    style = RetroTypography.Default.copy(fontSize = 8.sp),
                    color = io.github.kouheisatou.static_cms.ui.theme.RetroColors.DisabledText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Instructions
            Text(
                text = "Enter a repository URL containing a 'contents' or 'sample_contents' directory",
                style = RetroTypography.Default.copy(
                    color = io.github.kouheisatou.static_cms.ui.theme.RetroColors.DisabledText,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
} 