package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun MetadataHeader(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    textAlign = TextAlign.Center,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier
      .padding(
        horizontal = 16.dp,
      ),
  )
}
