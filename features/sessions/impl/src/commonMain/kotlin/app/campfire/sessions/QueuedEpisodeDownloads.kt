// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Corked
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloads
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.api.SessionQueue
import app.campfire.settings.api.DownloadSettings
import app.campfire.user.api.UserRepository
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.launch

/**
 * Downloads each podcast episode as it's added to the queue (Up next), while "Download queued
 * episodes" is on and the server lets the user download. Only additions count: what's already
 * queued at sign-in or when the setting is turned on stays as it is, and a queued episode whose
 * download is removed isn't fetched again until it's queued anew.
 */
@ContributesIntoSet(UserScope::class, binding = binding<Scoped>())
@Inject
class QueuedEpisodeDownloads(
  private val sessionQueue: SessionQueue,
  private val downloads: Downloads,
  private val downloadSettings: DownloadSettings,
  private val userRepository: UserRepository,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Scoped {

  override suspend fun onCreate() {
    coroutineScopeHolder.get().launch {
      // Null until the queue is first read, which is where additions are counted from
      var queued: Set<String>? = null
      sessionQueue.observeAll().collect { entries ->
        val episodes = entries.filter { it.episode != null }
        val added = queued?.let { previous -> episodes.filter { it.key !in previous } }.orEmpty()
        queued = episodes.mapTo(HashSet()) { it.key }
        if (added.isNotEmpty()) download(added)
      }
    }
  }

  private fun download(added: List<QueuedEntry>) {
    if (!downloadSettings.downloadQueuedEpisodes) return
    if (!userRepository.userFlow.value.canDownload) return

    ibark { "Downloading ${added.size} queued episode(s)" }
    downloads.download(
      added.mapNotNull { entry ->
        entry.episodeId?.let { DownloadContent.Episode(entry.libraryItemId, it) }
      },
    )
  }

  private companion object : Corked("QueuedEpisodeDownloads")
}
