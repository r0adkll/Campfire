package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A common re-usable state composable for showing a full content sized [CircularProgressIndicator]
 * in the middle
 */
@Composable
fun LoadingState(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}
