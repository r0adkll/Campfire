package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.clockFormat
import kotlin.time.Duration

private val ListItemHeight = 36.dp

@Composable
internal fun DurationListItem(
  title: String,
  duration: Duration,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .defaultMinSize(minHeight = ListItemHeight)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.weight(1f),
    )
    Spacer(Modifier.width(16.dp))
    Text(
      text = duration.clockFormat(),
      style = MaterialTheme.typography.labelSmall,
      fontFamily = FontFamily.Monospace,
    )
  }
}
