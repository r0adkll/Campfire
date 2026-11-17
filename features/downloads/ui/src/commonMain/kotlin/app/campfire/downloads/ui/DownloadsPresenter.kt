// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.offline.Downloads
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
  private val analytics: Analytics,
) : Presenter<DownloadsUiState> {

  @Composable
  override fun present(): DownloadsUiState {
    val scope = rememberCoroutineScope()
    val entries by remember {
      downloads.observeAllWithProgress()
        .asDownloadEntries(libraryItemRepository::getLibraryItem)
        .map<List<DownloadEntry>, LoadState<out List<DownloadEntry>>> { LoadState.Loaded(it) }
        .catch { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    return DownloadsUiState(
      downloads = entries,
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

        is DownloadsUiEvent.RetryDownload -> {
          analytics.send(ActionEvent("retry_download", Click))
          downloads.download(listOf(event.entry.download.content))
        }

        is DownloadsUiEvent.DeleteDownload -> {
          analytics.send(ActionEvent("delete_download", Click))
          when (val entry = event.entry) {
            // Deleting one download needs its item, so a missing item's go together by its id
            is DownloadEntry.Unavailable -> scope.launch {
              downloads.deleteAll(entry.download.content.itemId)
            }
            else -> downloads.delete(entry.download.content)
          }
        }
      }
    }
  }
}
