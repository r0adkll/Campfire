package app.campfire.common.compose.preview

import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

class DarkModeProvider : PreviewParameterProvider<Boolean> {
  override val values: Sequence<Boolean> = sequenceOf(true, false)
}
