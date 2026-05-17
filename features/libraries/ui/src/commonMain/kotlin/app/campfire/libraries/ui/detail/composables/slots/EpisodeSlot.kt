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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.filled.MarkFinished
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.widgets.EpisodeListItem
import app.campfire.common.compose.widgets.EpisodeListItemDefaults
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.playlists.api.dialog.PlaylistDialogResult
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_add_episode_to_playlist
import campfire.features.libraries.ui.generated.resources.action_mark_episode_finished
import campfire.features.libraries.ui.generated.resources.action_mark_episode_not_finished
import org.jetbrains.compose.resources.stringResource

class EpisodeSlot(
  private val libraryItem: LibraryItem,
  private val media: Media.Podcast,
  private val episode: PodcastEpisode,
  private val progress: MediaProgress?,
  private val isCurrentSession: Boolean,
  private val addToPlaylistDialog: AddToPlaylistDialog,
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
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    if (showAddToPlaylistDialog) {
      addToPlaylistDialog.Content(
        libraryItemId = libraryItem.id,
        itemTitle = libraryItem.media.metadata.title.orEmpty(),
        episode = episode,
        onDismiss = { _: PlaylistDialogResult ->
          showAddToPlaylistDialog = false
        },
        modifier = Modifier,
      )
    }

    val isFinished = progress?.isFinished == true

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
          val addToPlaylistLabel = stringResource(Res.string.action_add_episode_to_playlist)
          IconButtonTooltip(text = addToPlaylistLabel) {
            IconButton(
              onClick = { showAddToPlaylistDialog = true },
              modifier = Modifier
                .size(
                  IconButtonDefaults.extraSmallContainerSize(
                    IconButtonDefaults.IconButtonWidthOption.Uniform,
                  ),
                ),
              shape = IconButtonDefaults.extraSmallSquareShape,
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = addToPlaylistLabel,
                modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
              )
            }
          }

          val finishedLabel = stringResource(
            if (isFinished) {
              Res.string.action_mark_episode_not_finished
            } else {
              Res.string.action_mark_episode_finished
            },
          )
          IconButtonTooltip(text = finishedLabel) {
            IconButton(
              onClick = {
                if (isFinished) {
                  eventSink(LibraryItemUiEvent.MarkEpisodeNotFinished(episode))
                } else {
                  eventSink(LibraryItemUiEvent.MarkEpisodeFinished(episode))
                }
              },
              modifier = Modifier
                .size(
                  IconButtonDefaults.extraSmallContainerSize(
                    IconButtonDefaults.IconButtonWidthOption.Uniform,
                  ),
                ),
              shape = IconButtonDefaults.extraSmallSquareShape,
            ) {
              Icon(
                if (isFinished) {
                  Icons.Filled.MarkFinished
                } else {
                  Icons.Rounded.MarkFinished
                },
                contentDescription = finishedLabel,
                modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
              )
            }
          }
        },
        modifier = Modifier
          .padding(
            horizontal = 16.dp,
          ),
      )

      if (!isLast) {
        Spacer(Modifier.height(2.dp))
      } else {
        Spacer(Modifier.height(48.dp))
      }
    }
  }
}
