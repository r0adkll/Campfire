package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.by_author_line
import campfire.features.libraries.ui.generated.resources.by_narrator_line
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun AuthorNarratorBar(
  author: String?,
  narrator: String?,
  onAuthorClick: () -> Unit,
  onNarratorClick: () -> Unit,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ByLine(
      title = { Text(stringResource(Res.string.by_author_line)) },
      content = { Text(author ?: "--") },
      modifier = Modifier
        .clickable(onClick = onAuthorClick)
        .align(Alignment.Top)
        .weight(1f)
        .padding(horizontal = 8.dp),
    )

    VerticalDivider(
      modifier = Modifier.height(32.dp),
    )

    var isOverflowed by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(false) }
    var maxLines by remember { mutableStateOf(DefaultMaxLines) }
    ByLine(
      title = { Text(stringResource(Res.string.by_narrator_line)) },
      content = {
        Text(
          text = narrator ?: "--",
          maxLines = if (isExpanded) Int.MAX_VALUE else maxLines,
          overflow = TextOverflow.Ellipsis,
          onTextLayout = { result ->
            isOverflowed = result.didOverflowHeight || isExpanded
          },
        )
      },
      modifier = Modifier
        .combinedClickable(
          onClick = onNarratorClick,
          onLongClick = {
            isExpanded = !isExpanded
          },
        )
        .align(Alignment.Top)
        .weight(1f)
        .padding(horizontal = 8.dp),
    )
  }
}

@Composable
private fun ByLine(
  title: @Composable () -> Unit,
  content: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    ProvideTextStyle(
      MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
      ),
    ) {
      title()
    }
    Spacer(Modifier.height(4.dp))
    ProvideTextStyle(
      MaterialTheme.typography.bodyMedium.copy(
        textAlign = TextAlign.Center,
      ),
    ) {
      content()
    }
  }
}

private const val DefaultMaxLines = 2
