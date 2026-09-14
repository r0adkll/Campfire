// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPainter
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
import app.campfire.core.logging.LogRedaction
import app.campfire.core.logging.bark
import app.campfire.core.navigation.DeepLink
import app.campfire.di.DesktopApplicationComponent
import app.campfire.di.WindowComponent
import java.awt.Desktop
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Taskbar
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO
import kimchi.merge.app.campfire.di.createDesktopApplicationComponent
import kotlinx.coroutines.launch

@Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST", "USELESS_CAST", "KotlinRedundantDiagnosticSuppress")
fun main() = application {
  // Desktop only logs to local stdout — keep raw URLs readable for development.
  LogRedaction.enabled = false
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

  val appIcon = remember { loadAppIcon(ICON) }

  LaunchedEffect(Unit) { applyTaskbarIcon() }

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
    icon = remember(appIcon) { appIcon.toPainter() },
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
    // Compose's WindowState carries no minimum, so the floor goes on the AWT window itself.
    // Below a compact phone the adaptive layouts have nothing sensible left to do.
    LaunchedEffect(window) {
      window.minimumSize = Dimension(MinWindowWidth, MinWindowHeight)
    }

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
        WindowInsets(top = 12.dp),
        DeepLink.None,
        Modifier,
      )
    }
  }
}

/** Classpath anchor for reading files bundled into this module's jar. */
private object Resources

/** Full-bleed square artwork: what Windows and Linux want for a window and taskbar icon. */
private const val ICON = "/icon.png"

/**
 * The same artwork in the macOS silhouette — inset inside Apple's rounded square, with a shadow.
 * See tools/desktop-icon/generate.sh.
 */
private const val MACOS_ICON = "/icon-macos.png"

/**
 * Loads bundled icon artwork off the classpath rather than through Compose resources, so that the
 * files Conveyor rasterises the packaged icons from are the very same ones running here.
 */
private fun loadAppIcon(resource: String): BufferedImage {
  return requireNotNull(Resources.javaClass.getResourceAsStream(resource)) {
    "$resource is missing from :app:desktop resources"
  }.use(ImageIO::read)
}

/**
 * Sets the Dock icon, which on macOS is a separate thing from the window icon: macOS ignores the
 * per-window one entirely and draws the Dock from the app bundle, so a packaged build is already
 * right and this is what gives an unbundled `:app:desktop:run` the same icon rather than the stock
 * Java one. It has to be the inset macOS artwork — nothing masks what is handed to `Taskbar`, so
 * the square would render as a square, conspicuously larger than every icon beside it.
 *
 * Windows and Linux support no such feature and keep the window icon.
 */
private fun applyTaskbarIcon() {
  if (!Taskbar.isTaskbarSupported()) return
  val taskbar = Taskbar.getTaskbar()
  if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return
  taskbar.iconImage = loadAppIcon(MACOS_ICON)
}

/** The smallest the window may be shrunk to, in AWT units. */
private const val MinWindowWidth = 450
private const val MinWindowHeight = 800

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
