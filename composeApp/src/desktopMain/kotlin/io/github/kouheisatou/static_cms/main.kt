package io.github.kouheisatou.static_cms

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "StaticCMS",
    ) {
        App()
    }
}