package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.ShowMoreLessButton
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@Composable
internal fun ItemDescription(
  description: String,
  modifier: Modifier = Modifier,
  maxLines: Int = 5,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    var isOverflowed by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(false) }

    RichText(
      state = rememberRichTextState(
        description.trim()
          .replace(LineBreakStartRegex, "")
          .replace("\n", "<br>"),
      ),
      style = MaterialTheme.typography.bodyMedium,
      maxLines = if (isExpanded) Int.MAX_VALUE else maxLines,
      overflow = TextOverflow.Ellipsis,
      onTextLayout = { result ->
        isOverflowed = result.didOverflowHeight || isExpanded
      },
      modifier = Modifier
        .clickable(
          enabled = isOverflowed && !isExpanded,
          onClick = { isExpanded = !isExpanded },
        )
        .padding(horizontal = 16.dp),
    )

    Spacer(Modifier.height(8.dp))

    AnimatedVisibility(
      visible = isOverflowed,
    ) {
      ShowMoreLessButton(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = Modifier.padding(horizontal = 16.dp),
      )
    }
  }
}

@Composable
private fun rememberRichTextState(
  html: String,
): RichTextState {
  val state = rememberRichTextState()

  LaunchedEffect(html) {
    state.setHtml(html)
  }

  return state
}

private val LineBreakStartRegex = "^(\\\\n)+".toRegex()
