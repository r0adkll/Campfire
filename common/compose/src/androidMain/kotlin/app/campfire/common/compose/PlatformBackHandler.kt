package app.campfire.common.compose

import androidx.activity.compose.BackHandler as AndroidBackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
  AndroidBackHandler(enabled, onBack)
}
