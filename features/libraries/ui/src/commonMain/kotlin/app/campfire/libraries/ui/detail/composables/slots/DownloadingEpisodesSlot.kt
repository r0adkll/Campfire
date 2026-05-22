package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.podcast_downloads_banner_one
import campfire.features.libraries.ui.generated.resources.podcast_downloads_banner_other
import org.jetbrains.compose.resources.stringResource

/**
 * Header banner on the podcast detail screen showing how many episodes are currently in the
 * server's download queue for this podcast. Tapping opens the global Downloads screen.
 *
 * Only rendered when [count] > 0 (PodcastPresenter omits the slot otherwise).
 */
class DownloadingEpisodesSlot(
  private val count: Int,
) : ContentSlot {

  override val id: String = "downloading_episodes"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Surface(
      onClick = { eventSink(LibraryItemUiEvent.OpenDownloads) },
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.5.dp,
        )
        Text(
          text = bannerText(count),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.weight(1f),
        )
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun bannerText(count: Int): String {
  val resource = if (count == 1) {
    Res.string.podcast_downloads_banner_one
  } else {
    Res.string.podcast_downloads_banner_other
  }
  return stringResource(resource, count)
}

@Preview
@Composable
private fun DownloadingEpisodesSlotPreview_Single() = CampfireTheme {
  DownloadingEpisodesSlot(count = 1).Content(Modifier) {}
}

@Preview
@Composable
private fun DownloadingEpisodesSlotPreview_Multiple() = CampfireTheme {
  DownloadingEpisodesSlot(count = 4).Content(Modifier) {}
}
