// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.impl

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.image.CoverUrls
import app.campfire.core.logging.Corked
import app.campfire.core.model.LibraryItemId
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.network.di.UserClient
import app.campfire.reader.api.EbookRepository
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps a cover for every item downloaded on this device, audiobook, podcast episode or ebook,
 * and lets it go once nothing of the item is downloaded any more. Each time the downloads change
 * (and at sign-in) covers missing or out of date with their item are fetched; one that can't be
 * fetched now, offline, is tried again the next time.
 */
@ContributesIntoSet(UserScope::class, binding = binding<Scoped>())
@Inject
class OfflineCoverKeeper(
  private val offlineDownloadManager: OfflineDownloadManager,
  private val ebookRepository: EbookRepository,
  private val libraryItemRepository: LibraryItemRepository,
  private val covers: FileOfflineCovers,
  @UserClient private val client: HttpClient,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Scoped {

  override suspend fun onCreate() {
    coroutineScopeHolder.get().launch {
      downloadedItemIds().collectLatest { reconcile(it) }
    }
  }

  /**
   * Every item with something downloaded. Waits for both kinds of download to be read from the
   * disk, so it never looks empty at startup and lets every cover go.
   */
  private fun downloadedItemIds(): Flow<Set<LibraryItemId>> = combine(
    offlineDownloadManager.observeAll().map { downloads ->
      downloads.filter { it.state != OfflineDownload.State.None }.mapTo(HashSet()) { it.libraryItemId }
    },
    ebookRepository.observeDownloads(),
  ) { audio, ebooks -> audio + ebooks }
    .distinctUntilChanged()

  suspend fun reconcile(itemIds: Set<LibraryItemId>) {
    (covers.itemIds() - itemIds).forEach { covers.delete(it) }
    itemIds.forEach { itemId ->
      try {
        keep(itemId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        wbark(e) { "Couldn't keep the cover for $itemId" }
      }
    }
  }

  private suspend fun keep(itemId: LibraryItemId) {
    val item = libraryItemRepository.getLibraryItem(itemId)
    // Without a cover path the server has no cover to give
    if (item.media.coverPath.isNullOrBlank()) return
    val url = item.media.coverImageUrl
    val timestamp = CoverUrls.timestamp(url)
    val kept = covers.get(itemId)
    if (kept != null && kept.timestamp == timestamp) return

    val response = client.get(CoverUrls.sized(url, CoverUrls.ARTWORK_WIDTH)) {
      header(HttpHeaders.Accept, ACCEPT_HEADER)
    }
    if (!response.status.isSuccess()) {
      wbark { "Cover for $itemId unavailable: HTTP ${response.status.value}" }
      return
    }
    covers.save(itemId, timestamp, response.readRawBytes())
    dbark { "Kept the cover for $itemId" }
  }

  private companion object : Corked("OfflineCoverKeeper") {
    const val ACCEPT_HEADER = "image/webp,image/*;q=0.8,*/*;q=0.5"
  }
}
