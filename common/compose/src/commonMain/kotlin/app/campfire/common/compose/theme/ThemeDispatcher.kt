package app.campfire.common.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.launch

/**
 * An interface for receiving image dispatches to be processed upstream
 * by our quantizer and cached into memory
 */
fun interface ThemeDispatcher {
  suspend fun enqueue(
    key: String,
    imageBitmap: ImageBitmap,
  )
}

/**
 * A CompositionLocal to access the global theme dispatcher for processing image bitmaps
 * into color swatches and themes
 */
val LocalThemeDispatcher = compositionLocalOf {
  ThemeDispatcher { _, _ ->
    // Do nothing
  }
}

@Composable
fun rememberThemeDispatcherListener(
  key: String,
): (ImageBitmap) -> Unit {
  val scope = rememberCoroutineScope()
  val dispatcher by rememberUpdatedState(LocalThemeDispatcher.current)
  return remember(key, dispatcher) {
    { imageBitmap ->
      scope.launch {
        dispatcher.enqueue(key, imageBitmap)
      }
    }
  }
}
