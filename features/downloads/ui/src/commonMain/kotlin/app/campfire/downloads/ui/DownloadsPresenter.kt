// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.PlaybackController
import app.campfire.common.screens.DownloadsScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.offline.Downloads
import app.campfire.reader.api.screen.ReaderScreen
import app.campfire.user.api.MediaProgressRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@CircuitInject(DownloadsScreen::class, UserScope::class)
@Inject
class DownloadsPresenter(
  private val navigator: Navigator,
  private val downloads: Downloads,
  private val libraryItemRepository: LibraryItemRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackController: PlaybackController,
  private val analytics: Analytics,
  // Outlives the screen, for a delete still waiting to be undone when it's left
  @ForScope(UserScope::class) private val userScope: CoroutineScopeHolder,
) : Presenter<DownloadsUiState> {

  @Composable
  override fun present(): DownloadsUiState {
    val entries by remember {
      downloads.observeAllWithProgress()
        .asDownloadEntries(libraryItemRepository::getLibraryItem)
        .map<List<DownloadEntry>, LoadState<out List<DownloadEntry>>> { LoadState.Loaded(it) }
        .catch { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    // Looked up apart from the items, which don't carry their progress
    val finishedItemIds by remember {
      mediaProgressRepository.observeAllProgress()
        .map { it.finishedItemIds() }
        .catch { emit(emptySet()) }
    }.collectAsState(emptySet())

    var pendingDelete by remember { mutableStateOf<DownloadEntry?>(null) }

    // Deletes carried out, kept off the list until their downloads are gone from it
    var deleted by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(entries) {
      val listed = entries.dataOrNull?.map { it.key }?.toSet() ?: return@LaunchedEffect
      deleted = deleted intersect listed
    }

    fun commit(entry: DownloadEntry) {
      deleted = deleted + entry.key
      delete(entry)
    }

    // Leaving the screen is the last chance to undo
    val latestPendingDelete by rememberUpdatedState(pendingDelete)
    DisposableEffect(Unit) {
      onDispose { latestPendingDelete?.let(::delete) }
    }

    var filter by remember { mutableStateOf<DownloadKind?>(null) }
    val hidden = deleted + setOfNotNull(pendingDelete?.key)
    val content: LoadState<out DownloadsContent> = remember(entries, hidden, filter, finishedItemIds) {
      when (val loaded = entries) {
        is LoadState.Loaded -> {
          val listed = loaded.data.filterNot { it.key in hidden }
          val summary = listed.summary()
          // A kind whose last download has gone has nothing left to show
          val shownKind = filter.takeIf { kind -> summary.kinds.any { it.kind == kind } }
          LoadState.Loaded(
            DownloadsContent(
              entries = listed,
              summary = summary,
              filter = shownKind,
              sections = listed.asSections(shownKind),
              finishedBooks = listed.finishedBooks(finishedItemIds),
            ),
          )
        }
        LoadState.Loading -> LoadState.Loading
        LoadState.Error -> LoadState.Error
      }
    }

    return DownloadsUiState(
      downloads = content,
      pendingDelete = pendingDelete,
    ) { event ->
      when (event) {
        DownloadsUiEvent.Back -> navigator.pop()

        is DownloadsUiEvent.DownloadClick -> {
          // There's nothing to open for an item that can't be found
          event.entry.libraryItem?.let { item ->
            analytics.send(ActionEvent("download", Click))
            navigator.goTo(
              LibraryItemScreen(
                libraryItemId = item.id,
                episodeId = (event.entry as? DownloadEntry.Episode)?.episode?.id,
              ),
            )
          }
        }

        is DownloadsUiEvent.ShowClick -> {
          analytics.send(ActionEvent("download_show", Click))
          navigator.goTo(LibraryItemScreen(libraryItemId = event.libraryItem.id))
        }

        is DownloadsUiEvent.FilterChange -> {
          analytics.send(ActionEvent("download_filter", Click))
          filter = event.kind
        }

        is DownloadsUiEvent.PlayClick -> when (val entry = event.entry) {
          is DownloadEntry.Book -> {
            analytics.send(ActionEvent("download", "play"))
            playbackController.startSession(itemId = entry.libraryItem.id)
          }

          is DownloadEntry.Episode -> {
            analytics.send(ActionEvent("download", "play"))
            playbackController.startSession(itemId = entry.libraryItem.id, episodeId = entry.episode.id)
          }

          is DownloadEntry.Ebook -> {
            analytics.send(ActionEvent("download", "read"))
            navigator.goTo(ReaderScreen(entry.libraryItem.id))
          }

          is DownloadEntry.Unavailable -> Unit
        }

        is DownloadsUiEvent.RetryDownload -> {
          analytics.send(ActionEvent("retry_download", Click))
          downloads.download(listOf(event.entry.download.content))
        }

        is DownloadsUiEvent.DeleteDownload -> {
          // Only the latest delete can be undone
          pendingDelete?.let(::commit)
          pendingDelete = event.entry
        }

        is DownloadsUiEvent.DeleteDownloads -> {
          // Asked about first, so there's no undoing these, nor the delete that was waiting
          pendingDelete?.let(::commit)
          pendingDelete = null
          event.entries.forEach(::commit)
        }

        DownloadsUiEvent.OpenSettings -> {
          analytics.send(ActionEvent("download_settings", Click))
          navigator.goTo(SettingsScreen(SettingsScreen.Page.Downloads))
        }

        is DownloadsUiEvent.UndoDelete -> {
          if (pendingDelete?.key == event.entry.key) pendingDelete = null
        }

        is DownloadsUiEvent.CommitDelete -> {
          if (pendingDelete?.key == event.entry.key) {
            pendingDelete?.let(::commit)
            pendingDelete = null
          }
        }
      }
    }
  }

  private fun delete(entry: DownloadEntry) {
    analytics.send(ActionEvent("delete_download", Click))
    when (entry) {
      // Deleting one download needs its item, so a missing item's go together by its id
      is DownloadEntry.Unavailable -> userScope.get().launch {
        downloads.deleteAll(entry.download.content.itemId)
      }

      else -> downloads.delete(entry.download.content)
    }
  }
}
