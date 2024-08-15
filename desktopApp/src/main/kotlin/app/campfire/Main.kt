// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.logging.bark
import app.campfire.shared.DesktopApplicationComponent
import app.campfire.shared.WindowComponent
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import java.awt.Desktop
import java.net.URI
import kotlininject.merge.app.campfire.shared.createMergedDesktopApplicationComponent

@Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST", "USELESS_CAST", "KotlinRedundantDiagnosticSuppress")
fun main() = application {
  val applicationComponent = remember {
    DesktopApplicationComponent.createMergedDesktopApplicationComponent()
  }

//    LaunchedEffect(applicationComponent) {
//        applicationComponent.initializers.initialize()
//    }


  val backstack = rememberSaveableBackStack(listOf(WelcomeScreen))
  val navigator = rememberCircuitNavigator(backstack) { /* no-op */ }

  val windowState = rememberWindowState(
    width = 1080.dp,
    height = 720.dp,
    position = WindowPosition.Aligned(Alignment.Center)
  )
  Window(
    title = "Campfire",
    onCloseRequest = ::exitApplication,
    state = windowState,
    onKeyEvent = {
      if ((it.isCtrlPressed && it.key == Key.D) || it.key == Key.Escape) {
        navigator.pop()
        true
      } else  {
        false
      }
    }
  ) {
    val component: WindowComponent = remember(applicationComponent) {
      (applicationComponent.createWindowComponent() as WindowComponent)
    }

    component.campfireContent(
      backstack,
      navigator,
      { url ->
        try {
          val uri = URI(url)
          val dt = Desktop.getDesktop()
          dt.browse(uri)
        } catch (ex: Exception) {
          bark(throwable = ex) { "Unable to open URL" }
        }
      },
      WindowInsets(
        top = 24.dp,
        bottom = 24.dp,
      ),
      Modifier,
    )
  }
}
