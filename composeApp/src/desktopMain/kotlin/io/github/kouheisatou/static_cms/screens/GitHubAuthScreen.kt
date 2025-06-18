package io.github.kouheisatou.static_cms.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kouheisatou.static_cms.ui.components.*
import io.github.kouheisatou.static_cms.ui.theme.RetroTypography
import io.github.kouheisatou.static_cms.ui.theme.RetroColors
import androidx.compose.material3.Text
import io.github.kouheisatou.static_cms.util.GitHubApiClient

@Composable
fun GitHubAuthScreen(
    githubToken: String,
    onGitHubTokenChange: (String) -> Unit,
    onAuthenticateClick: (String) -> Unit,
    authState: GitHubApiClient.AuthState,
    currentUser: io.github.kouheisatou.static_cms.util.GitHubUser?,
    onContinue: () -> Unit
) {
    RetroWindow(
        title = "StaticCMS - GitHub Authentication",
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
                text = "GitHub Authentication Required",
                style = RetroTypography.Default.copy(fontSize = 16.sp),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            when (authState) {
                is GitHubApiClient.AuthState.NotAuthenticated -> {
                    GitHubTokenInput(
                        token = githubToken,
                        onTokenChange = onGitHubTokenChange,
                        onAuthenticate = onAuthenticateClick
                    )
                }
                is GitHubApiClient.AuthState.Authenticating -> {
                    AuthenticatingContent()
                }
                is GitHubApiClient.AuthState.Authenticated -> {
                    AuthenticatedContent(
                        user = authState.user,
                        onContinue = onContinue
                    )
                }
                is GitHubApiClient.AuthState.Error -> {
                    ErrorContent(
                        error = authState.message,
                        token = githubToken,
                        onTokenChange = onGitHubTokenChange,
                        onRetry = onAuthenticateClick
                    )
                }
            }
        }
    }
}

@Composable
private fun GitHubTokenInput(
    token: String,
    onTokenChange: (String) -> Unit,
    onAuthenticate: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.6f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GitHub Personal Access Token:",
            style = RetroTypography.Default,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )
        
        RetroPasswordField(
            value = token,
            onValueChange = onTokenChange,
            placeholder = "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        RetroTextButton(
            text = "Authenticate",
            onClick = { onAuthenticate(token) },
            enabled = token.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(bottom = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Instructions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "How to create a Personal Access Token:",
                style = RetroTypography.Default.copy(fontSize = 12.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "1. Go to GitHub Settings > Developer settings > Personal access tokens",
                style = RetroTypography.Default.copy(
                    color = RetroColors.DisabledText,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "2. Generate new token (classic)",
                style = RetroTypography.Default.copy(
                    color = RetroColors.DisabledText,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "3. Select 'repo' scope for full repository access",
                style = RetroTypography.Default.copy(
                    color = RetroColors.DisabledText,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "4. Copy and paste the token here",
                style = RetroTypography.Default.copy(
                    color = RetroColors.DisabledText,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun AuthenticatingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RetroProgressBar(
            progress = 0.5f,
            modifier = Modifier
                .width(200.dp)
                .padding(bottom = 16.dp)
        )
        Text(
            text = "Authenticating with GitHub...",
            style = RetroTypography.Default,
            color = RetroColors.WindowText
        )
    }
}

@Composable
private fun AuthenticatedContent(
    user: io.github.kouheisatou.static_cms.util.GitHubUser,
    onContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "✓ Authentication Successful",
            style = RetroTypography.Default.copy(fontSize = 14.sp),
            color = Color(0xFF008000),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Logged in as: ${user.login}",
            style = RetroTypography.Default,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        user.name?.let { name ->
            Text(
                text = "Name: $name",
                style = RetroTypography.Default,
                color = RetroColors.DisabledText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        user.email?.let { email ->
            Text(
                text = "Email: $email",
                style = RetroTypography.Default,
                color = RetroColors.DisabledText,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        RetroTextButton(
            text = "Continue to Repository",
            onClick = onContinue,
            modifier = Modifier
                .width(200.dp)
                .height(40.dp)
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    token: String,
    onTokenChange: (String) -> Unit,
    onRetry: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠ Authentication Failed",
            style = RetroTypography.Default.copy(fontSize = 14.sp),
            color = Color(0xFFCC0000),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = error,
            style = RetroTypography.Default,
            color = RetroColors.DisabledText,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        GitHubTokenInput(
            token = token,
            onTokenChange = onTokenChange,
            onAuthenticate = onRetry
        )
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