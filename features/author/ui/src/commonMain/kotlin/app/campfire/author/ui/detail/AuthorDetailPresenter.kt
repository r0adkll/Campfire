// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.author.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.author.api.AuthorRepository
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.offline.Downloads
import app.campfire.offline.observeByItem
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(AuthorDetailScreen::class, UserScope::class)
@Inject
class AuthorDetailPresenter(
  private val screen: AuthorDetailScreen,
  private val navigator: Navigator,
  private val authorRepository: AuthorRepository,
  private val downloads: Downloads,
  private val analytics: Analytics,
) : NonPausablePresenter<AuthorDetailUiState> {

  @Composable
  override fun present(): AuthorDetailUiState {
    val authorContentState by remember {
      authorRepository.observeAuthor(screen.authorId)
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    val offlineDownloads by remember {
      downloads.observeByItem()
    }.collectAsState(emptyMap())

    return AuthorDetailUiState(
      authorContentState = authorContentState,
      offlineStates = offlineDownloads,
    ) { event ->
      when (event) {
        is AuthorDetailUiEvent.LibraryItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.libraryItem.id))
        }
        AuthorDetailUiEvent.Back -> navigator.pop()
      }
    }
  }
}
