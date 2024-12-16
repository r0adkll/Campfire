package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import app.campfire.core.model.Media
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.by_author_line
import campfire.features.libraries.ui.generated.resources.by_narrator_line
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AuthorNarratorBar(
  metadata: Media.Metadata,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ByLine(
      title = { Text(stringResource(Res.string.by_author_line)) },
      content = { Text(metadata.authorName ?: "--") },
      modifier = Modifier
        .align(Alignment.Top)
        .weight(1f)
        .padding(horizontal = 8.dp),
    )

    VerticalDivider(
      modifier = Modifier.height(32.dp),
    )

    var maxLines by remember { mutableStateOf(DefaultMaxLines) }
    ByLine(
      title = { Text(stringResource(Res.string.by_narrator_line)) },
      content = {
        Text(
          text = metadata.narratorName ?: "--",
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
          ) {
            maxLines = if (maxLines == DefaultMaxLines) Int.MAX_VALUE else DefaultMaxLines
          },
        )
      },
      modifier = Modifier
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
