package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_show_less
import campfire.features.libraries.ui.generated.resources.action_show_more
import org.jetbrains.compose.resources.stringResource

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

    TextButton(
      onClick = { isExpanded = !isExpanded },
    ) {
      Icon(
        if (isExpanded) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore,
        contentDescription = null,
      )
      Text(
        text = if (isExpanded) {
          stringResource(Res.string.action_show_less)
        } else {
          stringResource(Res.string.action_show_more)
        },
      )
    }
  }
}
