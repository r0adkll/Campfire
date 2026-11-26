// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.appbar

import androidx.compose.runtime.Immutable
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.Library
import app.campfire.libraries.api.LibraryRepository
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The current library and every library to switch between, in display order. */
@Immutable
data class AppBarLibrariesState(
  val currentLibrary: Library? = null,
  val libraries: List<Library> = emptyList(),
)

/**
 * The libraries behind the app bar's switcher, held for the whole user session.
 *
 * Every top-level screen hosts its own app bar, so each new screen used to start from nothing and
 * pop the switcher in a few frames later, mid-transition. Keeping the state here hands each new app
 * bar the latest libraries on its first frame.
 */
@SingleIn(UserScope::class)
@Inject
class AppBarLibraries(
  private val libraryRepository: LibraryRepository,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) {

  // Cached reads: the drawer and Home already keep these fresh. No placeholder emissions, so a
  // restart after the stop timeout keeps showing the last libraries until fresh ones arrive.
  val state: StateFlow<AppBarLibrariesState> = combine(
    libraryRepository.observeCurrentLibrary(refresh = false)
      .map<Library, Library?> { it }
      .catch { emit(null) },
    libraryRepository.observeAllLibraries(refresh = false)
      .map { all -> all.sortedBy { it.displayOrder } }
      .catch { emit(emptyList()) },
    ::AppBarLibrariesState,
  ).stateIn(
    scope = coroutineScopeHolder.get(),
    started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
    initialValue = AppBarLibrariesState(),
  )

  fun select(library: Library) {
    coroutineScopeHolder.get().launch {
      libraryRepository.setCurrentLibrary(library)
    }
  }

  private companion object {
    // Outlasts the gap between one screen's app bar leaving and the next one arriving
    const val StopTimeoutMillis = 5_000L
  }
}
