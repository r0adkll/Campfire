// Copyright 2020, Google LLC, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.ios

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import app.campfire.common.root.CampfireContentProvider
import dev.zacsweers.metro.Inject
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIViewController

@Inject
class CampfireUiViewControllerFactory(
  private val campfireContentProvider: CampfireContentProvider,
) {

  fun create(): UIViewController = ComposeUIViewController {
    val uiViewController = LocalUIViewController.current

    campfireContentProvider.Content(
      { /* No-Op */ },
      { url ->
        val safari = SFSafariViewController(NSURL(string = url))
        uiViewController.presentViewController(safari, animated = true, completion = null)
      },
      Modifier,
    )
  }
}

private fun ViewConfiguration.withTouchSlop(
  touchSlop: Float,
): ViewConfiguration = object : ViewConfiguration by this {
  override val touchSlop: Float = touchSlop
}
