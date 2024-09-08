package app.campfire.common.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow

interface WindowBackEventDispatcher {
  val events: Flow<Unit>
}

val LocalWindowBackEventDispatcher = staticCompositionLocalOf<WindowBackEventDispatcher?> { null }

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
  val windowBackEvents by rememberUpdatedState(LocalWindowBackEventDispatcher.current)
  LaunchedEffect(windowBackEvents) {
    windowBackEvents?.events?.collect {
      if (enabled) {
        onBack()
      }
    }
  }
}
