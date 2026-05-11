package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.widgets.EpisodeListItem
import app.campfire.common.compose.widgets.EpisodeListItemDefaults
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.LibraryItemUiEvent

class EpisodeSlot(
  private val media: Media.Podcast,
  private val episode: PodcastEpisode,
  private val progress: MediaProgress?,
  private val isCurrentSession: Boolean,
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
    Column(
      modifier = modifier
        .background(ChapterContainerColor),
    ) {
      EpisodeListItem(
        episode = episode,
        mediaProgress = progress,
        isCurrentSession = isCurrentSession,
        onClick = {
          eventSink(LibraryItemUiEvent.OpenEpisode(episode))
        },
        onPlayClick = {
          eventSink(LibraryItemUiEvent.PlayEpisodeClick(episode))
        },
        shape = if (isFirst && !isLast) {
          EpisodeListItemDefaults.topItemShape()
        } else if (!isFirst && !isLast) {
          EpisodeListItemDefaults.middleItemShape()
        } else if (!isFirst && isLast) {
          EpisodeListItemDefaults.bottomItemShape()
        } else {
          EpisodeListItemDefaults.singleItemShape()
        },
        actions = {
          IconButton(
            onClick = {},
            modifier = Modifier
//        .minimumInteractiveComponentSize()
              .size(
                IconButtonDefaults.extraSmallContainerSize(
                  IconButtonDefaults.IconButtonWidthOption.Uniform,
                ),
              ),
            shape = IconButtonDefaults.extraSmallSquareShape,
          ) {
            Icon(
              Icons.AutoMirrored.Rounded.PlaylistAdd,
              contentDescription = null,
              modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
            )
          }

          IconButton(
            onClick = {},
            modifier = Modifier
//        .minimumInteractiveComponentSize()
              .size(
                IconButtonDefaults.extraSmallContainerSize(
                  IconButtonDefaults.IconButtonWidthOption.Uniform,
                ),
              ),
            shape = IconButtonDefaults.extraSmallSquareShape,
          ) {
            Icon(
              Icons.Rounded.MarkFinished,
              contentDescription = null,
              modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
            )
          }
        },
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
