package app.campfire.author.ui.detail.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.common.compose.widgets.ShowMoreLessButton

@Composable
internal fun AuthorDescription(
  description: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    var isExpandable by remember { mutableStateOf(false) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    // if at ANY point we know this description is expandable, make sure it stays that way
    LaunchedEffect(textLayout, isExpanded) {
      if (!isExpandable) {
        isExpandable = textLayout?.didOverflowHeight == true || isExpanded
      }
    }

    Text(
      text = description,
      style = MaterialTheme.typography.bodyLarge,
      maxLines = if (isExpanded) Int.MAX_VALUE else 5,
      overflow = TextOverflow.Ellipsis,
      onTextLayout = { textLayout = it },
      modifier = Modifier
        .clickable {
          Analytics.send(ActionEvent("author_description", "toggled", if (!isExpandable) "Expand" else "Collapse"))
          isExpanded = !isExpanded
        },
    )

    if (isExpandable) {
      Spacer(Modifier.height(8.dp))
      ShowMoreLessButton(
        expanded = isExpanded,
        onExpandedChange = {
          Analytics.send(ActionEvent("author_description", "toggled", if (it) "Expand" else "Collapse"))
          isExpanded = it
        },
      )
    }
  }
}
