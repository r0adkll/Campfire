// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.MarkFinished
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.icons.rounded.PlaylistAdd
import app.campfire.common.compose.permission.PermissionState
import app.campfire.common.compose.permission.rememberPostNotificationPermissionState
import app.campfire.common.compose.widgets.EpisodeDownloadAction
import app.campfire.common.compose.widgets.EpisodeListItem
import app.campfire.common.compose.widgets.EpisodeListItemDefaults
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.dialog.ConfirmDownloadDialog
import app.campfire.common.compose.widgets.dialog.DeleteEpisodeDialog
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.offline.Download
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
  private val isQueued: Boolean,
  private val offlineDownload: Download?,
  /** Whether the user may start a download; an existing download stays stoppable and removable. */
  private val canDownload: Boolean,
  /** Whether the user may delete the episode from the server, which a swipe offers. */
  private val canDelete: Boolean,
  /** Whether deleting it starts with deleting its file chosen, the last choice confirmed. */
  private val deleteFileByDefault: Boolean,
  private val showConfirmDownloadDialog: Boolean,
  private val addToPlaylistDialog: AddToPlaylistDialog,
  /** Where it sits in its list, which rounds the corners of the first and last rows. */
  private val isFirst: Boolean = true,
  private val isLast: Boolean = true,
) : ContentSlot {

  override val id: String = "episode_${episode.id}"
  override val contentType = ContentSlot.ContentType.Episode

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

    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
      DeleteEpisodeDialog(
        episodeTitle = episode.title,
        deleteFileByDefault = deleteFileByDefault,
        onConfirm = { hardDelete ->
          showDeleteDialog = false
          eventSink(LibraryItemUiEvent.DeleteEpisode(episode, hardDelete))
        },
        onDismissRequest = { showDeleteDialog = false },
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
        isQueued = isQueued,
        onQueueChange = { queued ->
          eventSink(
            if (queued) {
              LibraryItemUiEvent.AddEpisodeToQueue(episode)
            } else {
              LibraryItemUiEvent.RemoveEpisodeFromQueue(episode)
            },
          )
        },
        onDelete = if (canDelete) {
          { showDeleteDialog = true }
        } else {
          null
        },
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
          // Without permission the action only remains to stop or remove a download that exists
          val downloadState = offlineDownload?.state
          val hasActiveOrCompletedDownload = downloadState == Download.State.Queued ||
            downloadState == Download.State.Downloading ||
            downloadState == Download.State.Downloaded
          if (episode.audioTrack != null && (canDownload || hasActiveOrCompletedDownload)) {
            EpisodeDownloadAction(
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
                CampfireIcons.Rounded.PlaylistAdd,
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
                  CampfireIcons.Filled.MarkFinished
                } else {
                  CampfireIcons.Rounded.MarkFinished
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
