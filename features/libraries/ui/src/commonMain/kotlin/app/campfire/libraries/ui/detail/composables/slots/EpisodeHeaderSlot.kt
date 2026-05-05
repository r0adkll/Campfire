package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.header_episodes
import org.jetbrains.compose.resources.stringResource

class EpisodeHeaderSlot : ContentSlot {

  override val id: String = "episode_header"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.extraLarge.copy(
        bottomStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
      ),
      color = ChapterContainerColor,
    ) {
      Column {
        Spacer(Modifier.height(8.dp))

        // Header
        MetadataHeader(
          title = stringResource(Res.string.header_episodes),
          textStyle = MaterialTheme.typography.titleLarge,
          textColor = MaterialTheme.colorScheme.contentColorFor(ChapterContainerColor),
          trailingContent = {
            IconButton(
              onClick = {
              },
            ) {
              Icon(
                Icons.Rounded.Search,
                contentDescription = "Find episodes",
              )
            }
          },
          modifier = Modifier
            .heightIn(min = 48.dp)
            .padding(
              horizontal = 24.dp,
            ),
        )

        Spacer(Modifier.height(8.dp))
      }
    }
  }
}

@Preview
@Composable
fun EpisodeHeaderSlotPreview() {
  CampfireTheme {
    EpisodeHeaderSlot().Content(Modifier) {}
  }
}
