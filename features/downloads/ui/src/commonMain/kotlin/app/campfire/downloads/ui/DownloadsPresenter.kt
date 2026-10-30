// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.offline.Downloads
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject

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
    val entries by remember {
      downloads.observeAll().asDownloadEntries(libraryItemRepository::getLibraryItem)
    }.collectAsState(emptyList())

    return DownloadsUiState(
      downloads = entries,
    ) { event ->
      when (event) {
        DownloadsUiEvent.Back -> navigator.pop()

        is DownloadsUiEvent.DownloadClick -> {
          analytics.send(ActionEvent("download", Click))
          navigator.goTo(
            LibraryItemScreen(
              libraryItemId = event.entry.libraryItem.id,
              episodeId = (event.entry as? DownloadEntry.Episode)?.episode?.id,
            ),
          )
        }

        is DownloadsUiEvent.DeleteDownload -> {
          analytics.send(ActionEvent("delete_download", Click))
          downloads.delete(event.entry.download.content)
        }
      }
    }
  }
}
