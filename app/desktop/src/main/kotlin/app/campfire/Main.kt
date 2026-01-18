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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.campfire.common.compose.extensions.area
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.Extras
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.di.DesktopApplicationComponent
import app.campfire.di.WindowComponent
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.net.URI
import kimchi.merge.app.campfire.di.createDesktopApplicationComponent
import kotlinx.coroutines.launch

@Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST", "USELESS_CAST", "KotlinRedundantDiagnosticSuppress")
fun main() = application {
  Heartwood.grow(
    object : Heartwood.Bark {
      override fun log(priority: LogPriority, tag: String?, extras: Extras?, message: () -> String) {
        println("[${priority.name}] ${tag?.let { " ($it) " } ?: ""} ${message()}")
      }
    },
  )

  val applicationComponent = remember {
    DesktopApplicationComponent.createDesktopApplicationComponent().also { component ->
      ComponentHolder.components += component
      component.startupInitializer.initialize()
    }
  }

  val coroutineScope = rememberCoroutineScope()

  val maximumScreenSize = remember {
    GraphicsEnvironment.getLocalGraphicsEnvironment()
      .maximumWindowBounds
      .let { IntSize(it.width, it.height) }
  }

  val windowSize = remember {
    WindowSize.from(maximumScreenSize)
  }

  val windowState = rememberWindowState(
    width = windowSize.width.dp,
    height = windowSize.height.dp,
    position = WindowPosition.Aligned(Alignment.Center),
  )

  Window(
    title = "Campfire",
    onCloseRequest = ::exitApplication,
    state = windowState,
    onKeyEvent = {
      if ((it.isCtrlPressed && it.key == Key.D) || it.key == Key.Escape) {
        coroutineScope.launch {
          /* Replace this with desktop appropriate nav listener */
        }
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

    val uriHandler = remember {
      object : UriHandler {
        override fun openUri(uri: String) {
          try {
            val dt = Desktop.getDesktop()
            dt.browse(URI(uri))
          } catch (ex: Exception) {
            bark(throwable = ex) { "Unable to open URL" }
          }
        }
      }
    }

    CompositionLocalProvider(
      LocalUriHandler provides uriHandler,
    ) {
      component.campfireContent(
        { exitApplication() },
        uriHandler::openUri,
        WindowInsets(
          top = 24.dp,
          bottom = 24.dp,
        ),
        Modifier,
      )
    }
  }
}

sealed class WindowSize private constructor(
  val width: Int,
  val height: Int,
) {
  val area = width * height

  data object Small : WindowSize(1080, 720)
  data object Medium : WindowSize(1440, 960)
  data object Large : WindowSize(1920, 1080)
  class Custom internal constructor(width: Int, height: Int) : WindowSize(width, height)

  companion object {
    fun from(maximumScreenSize: IntSize): WindowSize {
      val screenArea = maximumScreenSize.area
      val largeRatio = screenArea.toFloat() / Large.area.toFloat()
      val mediumRatio = screenArea.toFloat() / Medium.area.toFloat()
      val smallRatio = screenArea.toFloat() / Small.area.toFloat()

      return when {
        largeRatio > 2f -> Large
        mediumRatio > 2f -> Medium
        else -> Small
      }
    }
  }
}
