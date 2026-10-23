// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline.impl

import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloader
import app.campfire.offline.Downloads
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Multibinds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Routes each content to the [Downloader] that keeps its kind, and puts every downloader's
 * downloads together when observing. Content no downloader keeps, such as audio on a platform
 * without downloads, is never downloaded.
 */
@ContributesBinding(UserScope::class)
@Inject
class DefaultDownloads(
  private val downloaders: Set<Downloader>,
) : Downloads {

  /**
   * Waits for every downloader to report, so content kept by one that's still reading its
   * downloads from the disk never briefly looks not downloaded.
   */
  override fun observeAll(): Flow<List<Download>> = if (downloaders.isEmpty()) {
    flowOf(emptyList())
  } else {
    combine(downloaders.map { it.observeAll() }) { downloads -> downloads.toList().flatten() }
  }

  override fun observe(contents: Set<DownloadContent>): Flow<Map<DownloadContent, Download>> {
    val routed = contents.groupBy(::downloaderFor)
      .mapNotNull { (downloader, contents) -> downloader?.observe(contents.toSet()) }
    return if (routed.isEmpty()) {
      flowOf(emptyMap())
    } else {
      combine(routed) { downloads -> downloads.fold(emptyMap()) { all, some -> all + some } }
    }
  }

  override fun download(contents: List<DownloadContent>) {
    contents.groupBy(::downloaderFor).forEach { (downloader, contents) -> downloader?.download(contents) }
  }

  override fun stop(content: DownloadContent) {
    downloaderFor(content)?.stop(content)
  }

  override fun delete(content: DownloadContent) {
    downloaderFor(content)?.delete(content)
  }

  override suspend fun deleteAll(itemId: LibraryItemId) {
    downloaders.forEach { it.deleteAll(itemId) }
  }

  private fun downloaderFor(content: DownloadContent): Downloader? {
    val downloader = downloaders.firstOrNull { it.handles(content) }
    if (downloader == null) bark(LogPriority.WARN) { "No downloader keeps $content" }
    return downloader
  }
}

/** A platform may keep no kind of download at all. */
@ContributesTo(UserScope::class)
interface Downloaders {
  @Multibinds(allowEmpty = true)
  val downloaders: Set<Downloader>
}
