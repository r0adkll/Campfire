package app.campfire.common.compose.layout

import androidx.compose.runtime.compositionLocalOf

enum class SupportingContentState {
  Closed,
  Open,
}

val LocalSupportingContentState = compositionLocalOf {
  SupportingContentState.Closed
}
