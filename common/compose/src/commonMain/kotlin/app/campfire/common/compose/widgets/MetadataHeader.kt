package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MetadataHeader(
  title: String,
  trailingContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
  textStyle: TextStyle = MaterialTheme.typography.titleLarge,
  fontWeight: FontWeight = FontWeight.SemiBold,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Row(
    modifier = modifier.heightIn(min = 56.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = textStyle,
      fontWeight = fontWeight,
      color = textColor,
      modifier = Modifier.weight(1f),
    )
    trailingContent?.let { trailing ->
      Spacer(Modifier.width(16.dp))
      trailing()
    }
  }
}
