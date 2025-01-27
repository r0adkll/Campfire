// Copyright 2020, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.ios

import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.window.ComposeUIViewController
import app.campfire.common.root.CampfireContent
import me.tatarka.inject.annotations.Inject
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIViewController

typealias CampfireUiViewController = () -> UIViewController

@Inject
fun CampfireUiViewController(
  campfireContent: CampfireContent,
): UIViewController = ComposeUIViewController {
  val uiViewController = LocalUIViewController.current

  campfireContent(
    { /* No-Op */ },
    { url ->
      val safari = SFSafariViewController(NSURL(string = url))
      uiViewController.presentViewController(safari, animated = true, completion = null)
    },
    Modifier,
  )
}

private fun ViewConfiguration.withTouchSlop(
  touchSlop: Float,
): ViewConfiguration = object : ViewConfiguration by this {
  override val touchSlop: Float = touchSlop
}
