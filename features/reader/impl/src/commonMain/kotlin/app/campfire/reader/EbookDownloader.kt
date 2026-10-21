// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloader
import app.campfire.reader.api.EbookDownload
import app.campfire.reader.api.EbookRepository
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** The ebooks the reader keeps to read offline. */
@ContributesIntoSet(UserScope::class, binding = binding<Downloader>())
@Inject
class EbookDownloader(
  private val ebookRepository: EbookRepository,
) : Downloader {

  override fun handles(content: DownloadContent): Boolean = content is DownloadContent.Ebook

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAll(): Flow<List<Download>> = ebookRepository.observeDownloads()
    .flatMapLatest { itemIds -> observe(itemIds.map { DownloadContent.Ebook(it) }.toSet()) }
    .map { it.values.toList() }

  override fun observe(contents: Set<DownloadContent>): Flow<Map<DownloadContent, Download>> {
    val ebooks = contents.filterIsInstance<DownloadContent.Ebook>()
    if (ebooks.isEmpty()) return flowOf(emptyMap())
    return combine(
      ebooks.map { ebook -> ebookRepository.observeDownload(ebook.itemId).map { it.asDownload(ebook.itemId) } },
    ) { downloads -> downloads.filterNotNull().associateBy { it.content } }
  }

  override fun download(contents: List<DownloadContent>) {
    contents.forEach { ebookRepository.download(it.itemId) }
  }

  /** An ebook's single file can't be stopped part way. */
  override fun stop(content: DownloadContent) = Unit

  override fun delete(content: DownloadContent) = ebookRepository.deleteDownload(content.itemId)

  override suspend fun deleteAll(itemId: LibraryItemId) = ebookRepository.deleteFiles(itemId)
}

/** The ebook download as every kind of download shows, or null if there isn't one. */
internal fun EbookDownload.asDownload(itemId: LibraryItemId): Download? {
  val state = when (state) {
    EbookDownload.State.None -> return null
    EbookDownload.State.Downloading -> Download.State.Downloading
    EbookDownload.State.Downloaded -> Download.State.Downloaded
    EbookDownload.State.Failed -> Download.State.Failed
  }
  return Download(
    content = DownloadContent.Ebook(itemId),
    state = state,
    bytes = bytes,
    totalBytes = totalBytes,
    progress = if (totalBytes > 0) bytes.toFloat() / totalBytes else null,
  )
}
