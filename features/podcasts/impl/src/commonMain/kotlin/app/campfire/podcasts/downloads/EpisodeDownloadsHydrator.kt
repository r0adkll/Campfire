// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.downloads

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Corked
import app.campfire.core.model.MediaType
import app.campfire.core.session.UserSession
import app.campfire.core.session.isLoggedIn
import app.campfire.libraries.api.LibraryRepository
import app.campfire.podcasts.api.PodcastsRepository
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Loads the server's podcast download queue whenever a podcast library becomes the current one,
 * so downloads already running at sign-in or on switching libraries show without opening the
 * download queue first. Socket events keep it current from there.
 */
@ContributesIntoSet(UserScope::class, binding = binding<Scoped>())
@Inject
class EpisodeDownloadsHydrator(
  private val userSession: UserSession,
  private val libraryRepository: LibraryRepository,
  private val podcastsRepository: PodcastsRepository,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Scoped {

  override suspend fun onCreate() {
    // The user scope also exists signed out, where there's no current library to read
    if (!userSession.isLoggedIn) return

    coroutineScopeHolder.get().launch {
      libraryRepository.observeCurrentLibrary(refresh = false)
        .filter { it.mediaType == MediaType.Podcast }
        .map { it.id }
        .distinctUntilChanged()
        .collect { libraryId ->
          podcastsRepository.fetchEpisodeDownloads(libraryId)
            .onFailure { e -> wbark(e) { "Couldn't load the server's podcast downloads" } }
        }
    }
  }

  private companion object : Corked("EpisodeDownloadsHydrator")
}
