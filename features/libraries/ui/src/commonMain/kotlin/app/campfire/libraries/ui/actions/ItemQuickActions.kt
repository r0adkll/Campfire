// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.actions

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.PlaybackController
import app.campfire.collections.api.ui.AddToCollectionDialog
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.ui.detail.ItemFinisher
import app.campfire.libraries.ui.detail.dates.ListeningDate
import app.campfire.libraries.ui.detail.dates.ListeningDatesEditor
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloads
import app.campfire.offline.download
import app.campfire.offline.observe
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.playlists.api.dialog.PlaylistDialogResult
import app.campfire.playlists.api.screen.PlaylistDetailScreen
import app.campfire.reader.api.screen.ReaderScreen
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.api.observeContains
import app.campfire.settings.api.LibraryViewSettings
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.api.UserRepository
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.item_added_to_playlist
import campfire.features.libraries.ui.generated.resources.item_added_to_playlist_open
import campfire.features.libraries.ui.generated.resources.mark_finished_snackbar
import campfire.features.libraries.ui.generated.resources.mark_finished_snackbar_set_date
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * The [QuickAction]s an item's card offers, as the item's state has them, and taking one. What
 * an action starts runs on past the card, as marking an item finished can take it out of its list.
 */
@Inject
class ItemQuickActions(
  private val userRepository: UserRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val downloads: Downloads,
  private val sessionQueue: SessionQueue,
  private val sessionsRepository: SessionsRepository,
  private val playbackController: PlaybackController,
  private val itemFinisher: ItemFinisher,
  private val libraryViewSettings: LibraryViewSettings,
  private val addToPlaylistDialog: AddToPlaylistDialog,
  private val addToCollectionDialog: AddToCollectionDialog,
  private val listeningDatesEditor: ListeningDatesEditor,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) {

  /** The quick actions for [item], following its progress, download and queue as they change */
  @Composable
  fun actionsFor(item: LibraryItem): List<QuickAction> {
    val user by userRepository.userFlow.collectAsState()
    val progress by remember(item.id) { mediaProgressRepository.observeProgress(item.id) }
      .collectAsState(item.userMediaProgress)
    val download by remember(item.id, item.isEbookOnly) { downloads.observe(item.downloadContent) }
      .collectAsState(null)
    val isQueued by remember(item.id) { sessionQueue.observeContains(item.id) }.collectAsState(false)
    val playing by remember { sessionsRepository.observePlayingItem() }.collectAsState(null)
    return quickActions(
      item = item,
      user = user,
      progress = progress,
      download = download,
      isQueued = isQueued,
      isPlaying = playing?.libraryItemId == item.id,
    )
  }

  /**
   * Takes [action] with [item]: going to its screen with [navigator], asking what's needed over
   * [overlayHost], and telling how it went in [snackbarHost].
   */
  suspend fun take(
    action: QuickAction,
    item: LibraryItem,
    navigator: Navigator,
    overlayHost: OverlayHost,
    snackbarHost: SnackbarHostState,
  ) {
    when (action) {
      is QuickAction.Play -> {
        Analytics.send(ActionEvent("play_item", Click, "card"))
        playbackController.startSession(item.id)
      }

      is QuickAction.Read -> {
        Analytics.send(ActionEvent("read", Click, "card"))
        navigator.goTo(ReaderScreen(item.id))
      }

      QuickAction.Download -> {
        if (!userRepository.userFlow.value.canDownload) return
        if (libraryViewSettings.showConfirmDownload) {
          val doNotShowAgain = overlayHost.showConfirmDownloadDialog(item) ?: return
          libraryViewSettings.showConfirmDownload = !doNotShowAgain
        }
        Analytics.send(ActionEvent("download", Click, "card"))
        downloads.download(item.downloadContent)
      }

      QuickAction.StopDownload -> {
        Analytics.send(ActionEvent("stop_download", Click, "card"))
        downloads.stop(item.downloadContent)
      }

      QuickAction.RemoveDownload -> {
        Analytics.send(ActionEvent("delete_download", Click, "card"))
        downloads.delete(item.downloadContent)
      }

      QuickAction.MarkFinished -> {
        Analytics.send(ActionEvent("mark_finished", Click, "card"))
        val isPlaying = sessionsRepository.observePlayingItem().first()?.libraryItemId == item.id
        val scope = coroutineScopeHolder.get()
        scope.launch { itemFinisher.markFinished(item.id, isPlaying) }
        // Told straight away, as the detail screen does, rather than once the finish is saved,
        // which a slow connection holds up. Marked by hand, the finish date is today's, which is
        // seldom when it was really finished.
        scope.launch {
          val result = snackbarHost.showSnackbar(
            message = getString(Res.string.mark_finished_snackbar),
            actionLabel = getString(Res.string.mark_finished_snackbar_set_date),
            duration = SnackbarDuration.Long,
          )
          if (result == SnackbarResult.ActionPerformed) {
            listeningDatesEditor.edit(item, ListeningDate.Finished, overlayHost, snackbarHost)
          }
        }
      }

      QuickAction.MarkNotFinished -> {
        Analytics.send(ActionEvent("mark_not_finished", Click, "card"))
        coroutineScopeHolder.get().launch { itemFinisher.markNotFinished(item.id) }
      }

      QuickAction.AddToQueue -> {
        Analytics.send(ActionEvent("add_to_queue", Click, "card"))
        coroutineScopeHolder.get().launch { sessionQueue.add(item) }
      }

      QuickAction.RemoveFromQueue -> {
        Analytics.send(ActionEvent("remove_from_queue", Click, "card"))
        coroutineScopeHolder.get().launch { sessionQueue.remove(item.id) }
      }

      QuickAction.AddToPlaylist -> {
        Analytics.send(ActionEvent("add_to_playlist", Click, "card"))
        val added = overlayHost.showAddToPlaylistDialog(item, addToPlaylistDialog)
        val playlistId = when (added) {
          is PlaylistDialogResult.Existing -> added.playlistId
          is PlaylistDialogResult.New -> added.playlistId
          PlaylistDialogResult.None -> return
        }
        coroutineScopeHolder.get().launch {
          val result = snackbarHost.showSnackbar(
            message = getString(Res.string.item_added_to_playlist),
            actionLabel = getString(Res.string.item_added_to_playlist_open),
          )
          if (result == SnackbarResult.ActionPerformed) {
            navigator.goTo(PlaylistDetailScreen(playlistId, null, null, added is PlaylistDialogResult.New))
          }
        }
      }

      QuickAction.AddToCollection -> {
        Analytics.send(ActionEvent("add_to_collection", Click, "card"))
        overlayHost.showAddToCollectionDialog(item, addToCollectionDialog)
      }
    }
  }
}

/** What downloading [this] keeps: its ebook if that's all it has, otherwise its audio */
internal val LibraryItem.downloadContent: DownloadContent
  get() = if (isEbookOnly) DownloadContent.Ebook(id) else DownloadContent.Audiobook(id)
