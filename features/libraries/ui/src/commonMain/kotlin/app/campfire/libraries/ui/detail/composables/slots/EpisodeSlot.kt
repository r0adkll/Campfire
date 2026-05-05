package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.EpisodeListItem

class EpisodeSlot(
  private val media: Media.Podcast,
  private val episode: PodcastEpisode,
) : ContentSlot {

  override val id: String = "episode_${episode.id}"
  override val contentType = ContentSlot.ContentType.Episode

  private val isFirst = media.episodes.indexOf(episode) == 0
  private val isLast = media.episodes.run {
    indexOf(episode) == lastIndex
  }

  @Composable
  override fun Content(
    modifier: Modifier,
    eventSink: (LibraryItemUiEvent) -> Unit,
  ) {
    val topCornerSize = if (isFirst) CornerSize(20.dp) else CornerSize(4.dp)
    val bottomCornerSize = if (isLast) CornerSize(20.dp) else CornerSize(4.dp)
    Column(
      modifier = modifier
        .background(ChapterContainerColor),
    ) {
      EpisodeListItem(
        episode = episode,
        onClick = {
          eventSink(LibraryItemUiEvent.OpenEpisode(episode))
        },
        onPlayClick = {
          eventSink(LibraryItemUiEvent.PlayEpisodeClick(episode))
        },
        shape = RoundedCornerShape(
          topStart = topCornerSize,
          topEnd = topCornerSize,
          bottomStart = bottomCornerSize,
          bottomEnd = bottomCornerSize,
        ),
        modifier = Modifier
          .padding(
            horizontal = 16.dp,
          ),
      )

      if (!isLast) {
        Spacer(Modifier.height(2.dp))
      }
    }
  }
}
