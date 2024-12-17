package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.ShowMoreLessButton

@Composable
internal fun ItemDescription(
  description: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    var isExpanded by remember { mutableStateOf(false) }
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = if (isExpanded) Int.MAX_VALUE else 5,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .clickable { isExpanded = !isExpanded }
        .padding(horizontal = 16.dp),
    )

    Spacer(Modifier.height(8.dp))

    ShowMoreLessButton(
      expanded = isExpanded,
      onExpandedChange = { isExpanded = it },
      modifier = Modifier.padding(horizontal = 16.dp),
    )
  }
}
