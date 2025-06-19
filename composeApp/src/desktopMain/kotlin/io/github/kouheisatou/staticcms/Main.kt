package io.github.kouheisatou.staticcms

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    application {
        val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)
        var shouldExit by remember { mutableStateOf(false) }
        
        Window(
            onCloseRequest = { 
                shouldExit = true 
            },
            title = "StaticCMS",
            state = windowState,
        ) {
            app(
                windowState = windowState,
                onCloseRequest = { 
                    shouldExit = true 
                }
            )
        }
        
        // Exit application when shouldExit is true
        LaunchedEffect(shouldExit) {
            if (shouldExit) {
                exitApplication()
            }
        }
    }
}
