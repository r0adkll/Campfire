package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.MarkFinished
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.permission.PermissionState
import app.campfire.common.compose.permission.rememberPostNotificationPermissionState
import app.campfire.common.compose.widgets.EpisodeListItem
import app.campfire.common.compose.widgets.EpisodeListItemDefaults
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.dialog.ConfirmDownloadDialog
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.playlists.api.dialog.PlaylistDialogResult
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_add_episode_to_playlist
import campfire.features.libraries.ui.generated.resources.action_download_episode
import campfire.features.libraries.ui.generated.resources.action_episode_download_in_progress
import campfire.features.libraries.ui.generated.resources.action_episode_download_queued
import campfire.features.libraries.ui.generated.resources.action_mark_episode_finished
import campfire.features.libraries.ui.generated.resources.action_mark_episode_not_finished
import campfire.features.libraries.ui.generated.resources.action_remove_episode_download
import org.jetbrains.compose.resources.stringResource

class EpisodeSlot(
  private val libraryItem: LibraryItem,
  private val media: Media.Podcast,
  private val episode: PodcastEpisode,
  private val progress: MediaProgress?,
  private val isCurrentSession: Boolean,
  private val offlineDownload: OfflineDownload?,
  private val showConfirmDownloadDialog: Boolean,
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

    var showDownloadConfirmation by remember { mutableStateOf(false) }
    var doNotShowDownloadConfirmationAgain by remember { mutableStateOf(false) }
    val postNotificationPermissionState = rememberPostNotificationPermissionState { granted ->
      if (granted) {
        eventSink(LibraryItemUiEvent.DownloadEpisodeClick(episode, doNotShowDownloadConfirmationAgain))
        showDownloadConfirmation = false
      }
    }
    if (showDownloadConfirmation) {
      ConfirmDownloadDialog(
        item = libraryItem,
        episode = episode,
        onConfirm = { doNotShowAgain ->
          if (postNotificationPermissionState is PermissionState.Granted) {
            eventSink(LibraryItemUiEvent.DownloadEpisodeClick(episode, doNotShowAgain))
            showDownloadConfirmation = false
          } else {
            doNotShowDownloadConfirmationAgain = doNotShowAgain
            postNotificationPermissionState.launchPermissionRequest()
          }
        },
        onDismissRequest = { showDownloadConfirmation = false },
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
          if (episode.audioTrack != null) {
            DownloadEpisodeAction(
              offlineDownload = offlineDownload,
              onDownloadClick = {
                if (showConfirmDownloadDialog) {
                  showDownloadConfirmation = true
                } else {
                  eventSink(LibraryItemUiEvent.DownloadEpisodeClick(episode))
                }
              },
              onStopClick = {
                eventSink(LibraryItemUiEvent.StopEpisodeDownloadClick(episode))
              },
              onRemoveClick = {
                eventSink(LibraryItemUiEvent.RemoveEpisodeDownloadClick(episode))
              },
            )
          }

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

@Composable
private fun DownloadEpisodeAction(
  offlineDownload: OfflineDownload?,
  onDownloadClick: () -> Unit,
  onStopClick: () -> Unit,
  onRemoveClick: () -> Unit,
) {
  val state = offlineDownload?.state ?: OfflineDownload.State.None

  val label = stringResource(
    when (state) {
      OfflineDownload.State.Queued -> Res.string.action_episode_download_queued
      OfflineDownload.State.Downloading -> Res.string.action_episode_download_in_progress
      OfflineDownload.State.Completed -> Res.string.action_remove_episode_download
      OfflineDownload.State.None,
      OfflineDownload.State.Failed,
      OfflineDownload.State.Stopped,
      -> Res.string.action_download_episode
    },
  )

  val onClick: () -> Unit = when (state) {
    OfflineDownload.State.Queued,
    OfflineDownload.State.Downloading,
    -> onStopClick

    OfflineDownload.State.Completed -> onRemoveClick

    OfflineDownload.State.None,
    OfflineDownload.State.Failed,
    OfflineDownload.State.Stopped,
    -> onDownloadClick
  }

  IconButtonTooltip(text = label) {
    IconButton(
      onClick = onClick,
      modifier = Modifier
        .size(
          IconButtonDefaults.extraSmallContainerSize(
            IconButtonDefaults.IconButtonWidthOption.Uniform,
          ),
        ),
      shape = IconButtonDefaults.extraSmallSquareShape,
    ) {
      val iconSize = IconButtonDefaults.extraSmallIconSize
      when (state) {
        OfflineDownload.State.Queued -> {
          Box(
            modifier = Modifier.size(iconSize),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              modifier = Modifier.size(iconSize),
            )
          }
        }
        OfflineDownload.State.Downloading -> {
          val downloadProgress = offlineDownload?.progress
          Box(
            modifier = Modifier.size(iconSize),
            contentAlignment = Alignment.Center,
          ) {
            if (downloadProgress == null || downloadProgress.indeterminate) {
              CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(iconSize),
              )
            } else {
              CircularProgressIndicator(
                progress = { downloadProgress.percent },
                strokeWidth = 2.dp,
                modifier = Modifier.size(iconSize),
              )
            }
          }
        }
        OfflineDownload.State.Completed -> {
          Icon(
            Icons.Rounded.DownloadDone,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize),
          )
        }
        OfflineDownload.State.None,
        OfflineDownload.State.Failed,
        OfflineDownload.State.Stopped,
        -> {
          Icon(
            CampfireIcons.Rounded.Download,
            contentDescription = label,
            modifier = Modifier.size(iconSize),
          )
        }
      }
    }
  }
}
