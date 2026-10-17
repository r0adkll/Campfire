// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.impl

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.model.LibraryItemId
import app.campfire.covers.api.OfflineCover
import app.campfire.covers.api.OfflineCovers
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Keeps one image per item in [CoverStorage], named `<itemId>.<timestamp>` so the version it was
 * kept at survives a restart. The directory is read once, then followed in memory, so lookups
 * from image loading don't touch the disk.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class FileOfflineCovers(
  private val storage: CoverStorage,
  private val dispatcherProvider: DispatcherProvider,
) : OfflineCovers {

  private val mutex = Mutex()
  private var covers: MutableMap<LibraryItemId, OfflineCover>? = null

  override suspend fun get(itemId: LibraryItemId): OfflineCover? = withCovers { it[itemId] }

  /** The items with a kept cover. */
  suspend fun itemIds(): Set<LibraryItemId> = withCovers { it.keys.toSet() }

  /** Keep [bytes] as [itemId]'s cover, replacing the one kept before. */
  suspend fun save(itemId: LibraryItemId, timestamp: Long?, bytes: ByteArray) = withCovers { covers ->
    val file = Path(storage.directory(), fileName(itemId, timestamp))
    val partFile = Path("$file$PART_SUFFIX")
    SystemFileSystem.createDirectories(file.parent!!)
    SystemFileSystem.sink(partFile).buffered().use { it.write(bytes) }
    SystemFileSystem.atomicMove(partFile, file)

    val previous = covers.put(itemId, OfflineCover(file.toString(), timestamp))
    if (previous != null && previous.path != file.toString()) {
      SystemFileSystem.delete(Path(previous.path), mustExist = false)
    }
  }

  /** Remove [itemId]'s cover. */
  suspend fun delete(itemId: LibraryItemId) = withCovers { covers ->
    covers.remove(itemId)?.let { SystemFileSystem.delete(Path(it.path), mustExist = false) }
  }

  private suspend fun <T> withCovers(block: (MutableMap<LibraryItemId, OfflineCover>) -> T): T =
    withContext(dispatcherProvider.io) {
      mutex.withLock {
        block(covers ?: load().also { covers = it })
      }
    }

  /** Read what's kept, clearing out what a crash mid-write left behind. */
  private fun load(): MutableMap<LibraryItemId, OfflineCover> {
    val directory = Path(storage.directory())
    if (!SystemFileSystem.exists(directory)) return mutableMapOf()
    val loaded = mutableMapOf<LibraryItemId, OfflineCover>()
    SystemFileSystem.list(directory).forEach { file ->
      val name = file.name
      if (name.endsWith(PART_SUFFIX)) {
        SystemFileSystem.delete(file, mustExist = false)
        return@forEach
      }
      val itemId = name.substringBeforeLast('.')
      val timestamp = name.substringAfterLast('.').toLongOrNull()
      val cover = OfflineCover(file.toString(), timestamp?.takeIf { it > 0 })
      val existing = loaded.put(itemId, cover)
      // Only one should be left, but if a delete was cut short keep the newest
      if (existing != null && (existing.timestamp ?: 0) > (timestamp ?: 0)) {
        loaded[itemId] = existing
        SystemFileSystem.delete(file, mustExist = false)
      } else if (existing != null) {
        SystemFileSystem.delete(Path(existing.path), mustExist = false)
      }
    }
    return loaded
  }
}

private fun fileName(itemId: LibraryItemId, timestamp: Long?) = "$itemId.${timestamp ?: 0}"

private const val PART_SUFFIX = ".part"
