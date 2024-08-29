// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.bark
import app.campfire.di.DesktopApplicationComponent
import app.campfire.di.WindowComponent
import app.campfire.shared.root.LocalWindowBackEventDispatcher
import app.campfire.shared.root.WindowBackEventDispatcher
import java.awt.Desktop
import java.net.URI
import kimchi.merge.app.campfire.di.createDesktopApplicationComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class DesktopWindowBackEventDispatcher : WindowBackEventDispatcher {
  override val events = MutableSharedFlow<Unit>()
}

@Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST", "USELESS_CAST", "KotlinRedundantDiagnosticSuppress")
fun main() = application {
  val applicationComponent = remember {
    DesktopApplicationComponent.createDesktopApplicationComponent().also {
      ComponentHolder.components += it
    }
  }

//    LaunchedEffect(applicationComponent) {
//        applicationComponent.initializers.initialize()
//    }

  val coroutineScope = rememberCoroutineScope()
  val windowBackEventDispatcher = remember {
    DesktopWindowBackEventDispatcher()
  }

  val windowState = rememberWindowState(
    width = 1080.dp,
    height = 720.dp,
    position = WindowPosition.Aligned(Alignment.Center),
  )
  Window(
    title = "Campfire",
    onCloseRequest = ::exitApplication,
    state = windowState,
    onKeyEvent = {
      if ((it.isCtrlPressed && it.key == Key.D) || it.key == Key.Escape) {
        coroutineScope.launch { windowBackEventDispatcher.events.emit(Unit) }
        true
      } else {
        false
      }
    },
  ) {
    val component: WindowComponent = remember(applicationComponent) {
      ComponentHolder.component<WindowComponent.Factory>().create().also {
        ComponentHolder.components += it
      }
    }

    CompositionLocalProvider(
      LocalWindowBackEventDispatcher provides windowBackEventDispatcher,
    ) {
      component.campfireContent(
        { exitApplication() },
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
}
