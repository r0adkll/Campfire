package app.campfire.ui.theming.api

import androidx.compose.ui.graphics.Color
import com.r0adkll.swatchbuckler.compose.Swatch

sealed class SwatchSelector(
  val selector: (Swatch) -> Color,
) {
  abstract val key: String

  object Dominant : SwatchSelector({ it.dominant }) {
    override val key = "dominant"
  }

  data class Vibrant(
    val index: Int = 0,
  ) : SwatchSelector({
    it.vibrant.getOrNull(index)
      ?: it.vibrant.lastOrNull()
      ?: it.dominant
  }) {
    override val key = "vibrant[$index]"
  }
}
