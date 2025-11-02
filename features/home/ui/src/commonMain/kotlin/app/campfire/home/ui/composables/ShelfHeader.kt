package app.campfire.home.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.home.ui.UiShelf

@Composable
fun ShelfHeader(
  shelf: UiShelf<*>,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .height(48.dp)
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = shelf.label,
      style = MaterialTheme.typography.labelLarge,
    )
  }
}
