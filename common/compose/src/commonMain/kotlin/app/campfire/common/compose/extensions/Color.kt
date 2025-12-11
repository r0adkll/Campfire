package app.campfire.common.compose.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.campfire.core.extensions.fluentIf
import com.r0adkll.swatchbuckler.color.hct.Hct

fun Color.toHexString(
  includePrefix: Boolean = true,
  includeAlpha: Boolean = false,
): String {
  return if (includePrefix) "#" else "" + toArgb().toHexString().fluentIf(!includeAlpha) { drop(2) }
}

fun Color.asHct(): Hct = Hct.fromInt(toArgb())

fun String.fromHexCode(): Color {
  var color = substring(1).toLong(16)
  if (length == 6) { // Set the alpha value
    color = color or -0x1000000
  } else {
    require(length == 8) { "Unknown color" }
  }
  return Color(color.toInt())
}

fun String.fromHexCodeOrNull(): Color? {
  return try {
    fromHexCode()
  } catch (e: Exception) {
    null
  }
}
