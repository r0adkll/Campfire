// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ios

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.window.ComposeUIViewController
import app.campfire.common.compose.window.LocalWindowScreenLauncher
import app.campfire.common.compose.window.WindowScreenLauncher
import app.campfire.common.root.CampfireContent
import app.campfire.common.root.WindowScreenContent
import app.campfire.core.navigation.DeepLink
import dev.zacsweers.metro.Inject
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIViewController

@Inject
fun CampfireUiViewController(
  campfireContent: CampfireContent,
  windowScreenContent: WindowScreenContent,
): UIViewController = ComposeUIViewController {
  val uiViewController = LocalUIViewController.current

  // URLs open in an in-app Safari sheet rather than handing off to the Safari app, which is
  // what the platform's default handler would do.
  val uriHandler = remember(uiViewController) {
    object : UriHandler {
      override fun openUri(uri: String) {
        val safari = SFSafariViewController(NSURL(string = uri))
        uiViewController.presentViewController(safari, animated = true, completion = null)
      }
    }
  }

  val windowScreenLauncher = remember(uiViewController) {
    WindowScreenLauncher { screen ->
      val window = WindowScreenViewController(screen, windowScreenContent)
      uiViewController.presentViewController(window, animated = true, completion = null)
    }
  }

  CompositionLocalProvider(
    LocalUriHandler provides uriHandler,
    LocalWindowScreenLauncher provides windowScreenLauncher,
  ) {
    campfireContent(
      { /* No-Op */ },
      DeepLink.None,
      Modifier,
    )
  }
}

private fun ViewConfiguration.withTouchSlop(
  touchSlop: Float,
): ViewConfiguration = object : ViewConfiguration by this {
  override val touchSlop: Float = touchSlop
}
