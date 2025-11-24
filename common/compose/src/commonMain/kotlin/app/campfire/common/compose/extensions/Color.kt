package app.campfire.common.compose.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.campfire.core.extensions.fluentIf

fun Color.toHexString(includeAlpha: Boolean = false): String {
  return "#${toArgb().toHexString().fluentIf(!includeAlpha) { drop(2) }}"
}
