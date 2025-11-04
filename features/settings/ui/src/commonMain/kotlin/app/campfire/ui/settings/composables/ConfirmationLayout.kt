package app.campfire.ui.settings.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import app.campfire.common.compose.widgets.circularReveal

@OptIn(ExperimentalIndirectTouchTypeApi::class)
@Composable
internal fun ConfirmationLayout(
  showConfirmation: Boolean,
  modifier: Modifier = Modifier,
  confirm: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = modifier,
  ) {
    content()

    var animateShowConfirmation by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    if (showConfirmation || animateShowConfirmation || isAnimating) {
      LaunchedEffect(showConfirmation) {
        if (!showConfirmation && animateShowConfirmation) {
          isAnimating = true
          animateShowConfirmation = false
        } else if (showConfirmation && !animateShowConfirmation) {
          isAnimating = true
          animateShowConfirmation = true
        }
      }

      Box(
        modifier = Modifier
          .zIndex(1f)
          .matchParentSize()
          .circularReveal(
            isVisible = animateShowConfirmation,
            revealFrom = Offset(0.95f, 0.5f),
            onAnimationFinish = {
              isAnimating = false
            },
          ),
      ) {
        confirm()
      }
    }
  }
}
