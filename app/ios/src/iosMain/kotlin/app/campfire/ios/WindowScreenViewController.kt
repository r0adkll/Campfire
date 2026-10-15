// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ios

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.window.ComposeUIViewController
import app.campfire.common.compose.window.LocalWindowScreenController
import app.campfire.common.compose.window.WindowScreenController
import app.campfire.common.root.WindowScreenContent
import app.campfire.common.screens.WindowScreen
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleDefault
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

/**
 * Shows one [WindowScreen] full screen, presented over the app. It owns the status bar while it's
 * up, which the screen hides through [LocalWindowScreenController]. The home indicator stays: its
 * preference comes from a UIKit category, which a Kotlin view controller can't override.
 */
class WindowScreenViewController(
  private val screen: WindowScreen,
  private val content: WindowScreenContent,
) : UIViewController(nibName = null, bundle = null) {

  private var systemBarsHidden = false
  private var lightSystemBars: Boolean? = null

  // A Kotlin/Native subclass of an Objective-C class can't also implement a Kotlin interface
  private val controller = object : WindowScreenController {
    override fun setSystemBarsVisible(visible: Boolean) {
      if (systemBarsHidden == !visible) return
      systemBarsHidden = !visible
      setNeedsStatusBarAppearanceUpdate()
    }

    override fun setLightSystemBars(light: Boolean) {
      if (lightSystemBars == light) return
      lightSystemBars = light
      setNeedsStatusBarAppearanceUpdate()
    }

    override fun setKeepScreenOn(keepOn: Boolean) {
      UIApplication.sharedApplication.idleTimerDisabled = keepOn
    }

    // iOS doesn't hand the volume buttons to apps
    override val supportsVolumeKeys: Boolean get() = false

    override fun setVolumeKeyHandler(handler: ((up: Boolean) -> Boolean)?) = Unit
  }

  init {
    modalPresentationStyle = UIModalPresentationFullScreen
  }

  @OptIn(ExperimentalForeignApi::class)
  override fun viewDidLoad() {
    super.viewDidLoad()

    val compose = ComposeUIViewController {
      val uiViewController = LocalUIViewController.current
      val uriHandler = remember(uiViewController) {
        object : UriHandler {
          override fun openUri(uri: String) {
            val safari = SFSafariViewController(NSURL(string = uri))
            uiViewController.presentViewController(safari, animated = true, completion = null)
          }
        }
      }

      CompositionLocalProvider(
        LocalWindowScreenController provides controller,
        LocalUriHandler provides uriHandler,
      ) {
        content(screen = screen, onClose = ::close)
      }
    }

    addChildViewController(compose)
    compose.view.setFrame(view.bounds)
    compose.view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
    view.addSubview(compose.view)
    compose.didMoveToParentViewController(this)
  }

  override fun viewDidDisappear(animated: Boolean) {
    super.viewDidDisappear(animated)
    if (isBeingDismissed()) controller.setKeepScreenOn(false)
  }

  override fun prefersStatusBarHidden(): Boolean = systemBarsHidden

  override fun preferredStatusBarStyle(): UIStatusBarStyle = when (lightSystemBars) {
    true -> UIStatusBarStyleDarkContent
    false -> UIStatusBarStyleLightContent
    null -> UIStatusBarStyleDefault
  }

  private fun close() {
    if (presentingViewController != null && !isBeingDismissed()) {
      dismissViewControllerAnimated(true, completion = null)
    }
  }
}
