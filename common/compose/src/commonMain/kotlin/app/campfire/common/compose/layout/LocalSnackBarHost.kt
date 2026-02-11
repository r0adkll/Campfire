package app.campfire.common.compose.layout

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackBarHost = staticCompositionLocalOf {
  SnackbarHostState()
}
