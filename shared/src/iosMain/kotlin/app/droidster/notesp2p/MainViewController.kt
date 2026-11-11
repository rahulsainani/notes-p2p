package app.droidster.notesp2p

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// Swift calls this by name (MainViewControllerKt.MainViewController()), so it keeps PascalCase.
@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
