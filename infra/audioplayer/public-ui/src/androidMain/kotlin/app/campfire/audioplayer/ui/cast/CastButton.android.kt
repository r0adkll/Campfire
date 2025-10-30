package app.campfire.audioplayer.ui.cast

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext

@Composable
actual fun PlatformCastButton(modifier: Modifier) {
  val context = LocalContext.current
  var mediaRouteButton by remember { mutableStateOf<CustomMediaRouteButton?>(null) }

  LaunchedEffect(Unit) {
    try {
      val button = CustomMediaRouteButton(context)

      CastContext.getSharedInstance(context)
      CastButtonFactory.setUpMediaRouteButton(context, button)
      mediaRouteButton = button
    } catch (e: Exception) {
      // Do Nothing
      e.printStackTrace()
    }
  }

  mediaRouteButton?.let { button ->
    AndroidView(
      factory = { _ -> button },
      update = { updatedButton: CustomMediaRouteButton ->
        if (updatedButton.isAttachedToWindow) {
          MediaRouteButtonManager.add(updatedButton)
        }
      },
      onRelease = { MediaRouteButtonManager.remove(it) },
      onReset = {},
      modifier = modifier,
    )
  }
}

/**
 * An extended version of the cast framework's built-in [MediaRouteButton] that allows us to keep
 * track of the valid instances. The idea is to bind the addition and removal of the button to the
 * event when the button is attached and detached from the window respectively.
 */
class CustomMediaRouteButton(context: Context): MediaRouteButton(context) {
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    MediaRouteButtonManager.add(this)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    MediaRouteButtonManager.remove(this)
  }
}

/**
 * A helper singleton to store the valid [CustomMediaRouteButton] instances. Since these buttons can be
 * created and destroyed dynamically, we need to keep track of the valid instances.
 */
object MediaRouteButtonManager {
  private val buttons = mutableListOf<CustomMediaRouteButton>()

  fun add(button: CustomMediaRouteButton) {
    buttons.add(button)
  }

  fun remove(button: CustomMediaRouteButton) {
    buttons.remove(button)
  }

  // Since we only want to fake a click on the button, it's enough to always interact with the last one
  val current: CustomMediaRouteButton?
    get() = buttons.lastOrNull()
}
