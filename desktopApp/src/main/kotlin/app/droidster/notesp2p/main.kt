package app.droidster.notesp2p

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Notes p2p",
    ) {
        App()
    }
}