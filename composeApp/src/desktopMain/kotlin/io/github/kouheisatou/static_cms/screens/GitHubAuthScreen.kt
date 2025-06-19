package io.github.kouheisatou.static_cms.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kouheisatou.static_cms.model.AuthState
import io.github.kouheisatou.static_cms.model.GitHubUser
import io.github.kouheisatou.static_cms.ui.components.*
import io.github.kouheisatou.static_cms.ui.theme.RetroColors
import io.github.kouheisatou.static_cms.ui.theme.RetroTypography


@Composable
fun GitHubAuthScreen(
    githubToken: String,
    onGitHubTokenChange: (String) -> Unit,
    onAuthenticateClick: (String) -> Unit,
    onBrowserAuthClick: () -> Unit,
    authState: AuthState,
    currentUser: GitHubUser?,
    onContinue: () -> Unit
) {
    RetroWindow(
        title = "StaticCMS - GitHub Authentication",
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
                text = "GitHub Authentication",
                style = RetroTypography.Default.copy(fontSize = 18.sp),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Connect your GitHub account to manage repositories",
                style = RetroTypography.Default.copy(fontSize = 12.sp),
                color = RetroColors.DisabledText,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Authentication content - Browser OAuth only
            Column(
                modifier = Modifier.fillMaxWidth(0.5f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrowserAuthSection(
                    authState = authState,
                    currentUser = currentUser,
                    onBrowserAuthClick = onBrowserAuthClick,
                    onContinue = onContinue
                )
            }
        }
    }
}

@Composable
private fun BrowserAuthSection(
    authState: AuthState,
    currentUser: GitHubUser?,
    onBrowserAuthClick: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (authState) {
            is AuthState.Idle -> {
                Text(
                    text = "🔒 Secure OAuth Authentication",
                    style = RetroTypography.Default.copy(fontSize = 14.sp),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Click the button below to authenticate with GitHub in your browser. This will open a new browser window for secure login.",
                    style = RetroTypography.Default.copy(fontSize = 10.sp),
                    color = RetroColors.DisabledText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                RetroTextButton(
                    text = "🌐 Connect with GitHub",
                    onClick = onBrowserAuthClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
            
            is AuthState.Starting -> {
                Text(
                    text = "🔄 Initializing authentication...",
                    style = RetroTypography.Default,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                RetroProgressBar(
                    progress = 0.3f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            is AuthState.WaitingForUser -> {
                Text(
                    text = "🌐 Browser Authentication",
                    style = RetroTypography.Default.copy(fontSize = 14.sp),
                    color = RetroColors.TitleBarActive,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Please complete the authentication in your browser window.",
                    style = RetroTypography.Default.copy(fontSize = 10.sp),
                    color = RetroColors.DisabledText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "If the browser didn't open automatically, check your browser for the GitHub login page.",
                    style = RetroTypography.Default.copy(fontSize = 9.sp),
                    color = RetroColors.DisabledText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                RetroProgressBar(
                    progress = 0.6f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            is AuthState.Processing -> {
                Text(
                    text = "🔄 Processing authentication...",
                    style = RetroTypography.Default,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Verifying credentials and setting up access...",
                    style = RetroTypography.Default.copy(fontSize = 10.sp),
                    color = RetroColors.DisabledText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                RetroProgressBar(
                    progress = 0.9f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            is AuthState.Success -> {
                currentUser?.let { user ->
                    Text(
                        text = "✅ Authentication Successful!",
                        style = RetroTypography.Default.copy(fontSize = 14.sp),
                        color = RetroColors.TitleBarActive,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = "Welcome, ${user.login}!",
                        style = RetroTypography.Default.copy(fontSize = 12.sp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    user.name?.let { name ->
                        Text(
                            text = name,
                            style = RetroTypography.Default.copy(fontSize = 10.sp),
                            color = RetroColors.DisabledText,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    Text(
                        text = "🌐 Browser window will close automatically...",
                        style = RetroTypography.Default.copy(fontSize = 10.sp),
                        color = RetroColors.DisabledText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Proceeding to repository selection in a moment...",
                        style = RetroTypography.Default.copy(fontSize = 10.sp),
                        color = RetroColors.DisabledText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    RetroProgressBar(
                        progress = 1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            is AuthState.Error -> {
                Text(
                    text = "❌ Authentication Failed",
                    style = RetroTypography.Default.copy(fontSize = 14.sp),
                    color = RetroColors.DisabledText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = authState.message,
                    style = RetroTypography.Default.copy(fontSize = 10.sp),
                    color = RetroColors.DisabledText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                RetroTextButton(
                    text = "🔄 Try Again",
                    onClick = onBrowserAuthClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
        }
    }
}

@Composable
private fun RetroPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    Box(
        modifier = modifier
            .background(Color.White)
            .border(2.dp, RetroColors.ButtonDarkShadow)
            .padding(4.dp)
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = RetroTypography.Default.copy(color = Color.Black),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = RetroTypography.Default.copy(color = RetroColors.DisabledText)
            )
        }
    }
} 