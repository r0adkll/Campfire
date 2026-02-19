package app.campfire.common.compose.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class DarkModeProvider : PreviewParameterProvider<Boolean> {
  override val values: Sequence<Boolean> = sequenceOf(true, false)
}
