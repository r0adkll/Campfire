package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val ItemDetailHeight = 24.dp

@Composable
internal fun ItemDetailItem(
  icon: ImageVector,
  text: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .defaultMinSize(minHeight = ItemDetailHeight),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(icon, contentDescription = null)
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
    )
  }
}
