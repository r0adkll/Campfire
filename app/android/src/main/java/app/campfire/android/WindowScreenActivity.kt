// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.campfire.common.compose.window.LocalWindowScreenController
import app.campfire.common.compose.window.WindowScreenController
import app.campfire.common.root.WindowScreenContent
import app.campfire.common.screens.WindowScreen
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import dev.zacsweers.metro.ContributesTo

/**
 * Shows one [WindowScreen] full screen, above [MainActivity] in the same task. Back finishes it
 * and returns to wherever it was opened from.
 */
class WindowScreenActivity : ComponentActivity() {

  private var controller: ActivityWindowScreenController? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    val screen = IntentCompat.getParcelableExtra(intent, EXTRA_SCREEN, WindowScreen::class.java)
    if (screen == null) {
      finish()
      return
    }

    val content = ComponentHolder.component<WindowScreenComponent>().windowScreenContent
    val controller = ActivityWindowScreenController(this).also { controller = it }

    setContent {
      val uriHandler = remember {
        object : UriHandler {
          override fun openUri(uri: String) {
            CustomTabsIntent.Builder().build().launchUrl(this@WindowScreenActivity, uri.toUri())
          }
        }
      }

      CompositionLocalProvider(
        LocalWindowScreenController provides controller,
        LocalUriHandler provides uriHandler,
      ) {
        content(screen = screen, onClose = ::finish)
      }
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val up = when (event.keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> true
      KeyEvent.KEYCODE_VOLUME_DOWN -> false
      else -> null
    }
    val handler = controller?.volumeKeyHandler
    if (up != null && handler != null) {
      // The press is used on key down; its key up is swallowed too, so the volume doesn't change
      if (event.action == KeyEvent.ACTION_UP) return true
      if (handler(up)) return true
    }
    return super.dispatchKeyEvent(event)
  }

  companion object {
    private const val EXTRA_SCREEN = "app.campfire.android.WINDOW_SCREEN"

    fun intent(context: Context, screen: WindowScreen): Intent =
      Intent(context, WindowScreenActivity::class.java)
        .putExtra(EXTRA_SCREEN, screen)
  }
}

@ContributesTo(AppScope::class)
interface WindowScreenComponent {
  val windowScreenContent: WindowScreenContent
}

private class ActivityWindowScreenController(
  private val activity: Activity,
) : WindowScreenController {

  private val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    .apply { systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }

  var volumeKeyHandler: ((up: Boolean) -> Boolean)? = null
    private set

  override val supportsVolumeKeys: Boolean get() = true

  override fun setSystemBarsVisible(visible: Boolean) {
    if (visible) {
      insetsController.show(WindowInsetsCompat.Type.systemBars())
    } else {
      insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
  }

  override fun setLightSystemBars(light: Boolean) {
    insetsController.isAppearanceLightStatusBars = light
    insetsController.isAppearanceLightNavigationBars = light
  }

  override fun setKeepScreenOn(keepOn: Boolean) {
    if (keepOn) {
      activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  override fun setVolumeKeyHandler(handler: ((up: Boolean) -> Boolean)?) {
    volumeKeyHandler = handler
  }
}
