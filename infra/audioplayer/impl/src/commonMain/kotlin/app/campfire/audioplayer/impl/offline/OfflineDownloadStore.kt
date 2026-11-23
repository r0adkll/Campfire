// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.core.logging.Cork
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The on-disk layout of offline downloads (desktop and iOS):
 *
 * ```
 * <root>/<libraryItemId>/item/manifest.json          a book (or item-level) download
 * <root>/<libraryItemId>/item/<trackIndex>.<ext>     a completed track
 * <root>/<libraryItemId>/item/<trackIndex>.<ext>.part a track still downloading
 * <root>/<libraryItemId>/episodes/<episodeId>/...    a single podcast episode download
 * ```
 */
internal class OfflineDownloadStore(
  /** An absolute path, so the paths handed to the player are too. */
  val root: Path,
) {

  fun directory(key: OfflineDownloadKey): Path {
    val itemDir = Path(root, key.libraryItemId.asFileName())
    val episodeId = key.episodeId
    return if (episodeId == null) {
      Path(itemDir, ITEM_DIR)
    } else {
      Path(itemDir, EPISODES_DIR, episodeId.asFileName())
    }
  }

  fun completedFile(key: OfflineDownloadKey, file: DownloadManifest.File): Path =
    Path(directory(key), file.fileName)

  fun partFile(key: OfflineDownloadKey, file: DownloadManifest.File): Path =
    Path(directory(key), file.fileName + PART_SUFFIX)

  /** Every readable manifest on disk. Unreadable ones are skipped, not deleted. */
  fun readAll(): List<DownloadManifest> = root.childDirectories().flatMap { itemDir ->
    val episodeDirs = Path(itemDir, EPISODES_DIR).childDirectories()
    (listOf(Path(itemDir, ITEM_DIR)) + episodeDirs)
      .mapNotNull { dir -> read(Path(dir, MANIFEST_FILE)) }
  }

  fun write(manifest: DownloadManifest) {
    val dir = directory(manifest.key)
    SystemFileSystem.createDirectories(dir)
    val target = Path(dir, MANIFEST_FILE)
    val temp = Path(dir, "$MANIFEST_FILE$PART_SUFFIX")
    SystemFileSystem.sink(temp).buffered().use { sink ->
      sink.writeString(json.encodeToString(DownloadManifest.serializer(), manifest))
    }
    temp.moveOnto(target)
  }

  /** Removes the download's directory, and its item directory once nothing else lives there. */
  fun delete(key: OfflineDownloadKey) {
    directory(key).deleteRecursively()
    val itemDir = Path(root, key.libraryItemId.asFileName())
    Path(itemDir, EPISODES_DIR).deleteIfEmpty()
    itemDir.deleteIfEmpty()
  }

  private fun read(file: Path): DownloadManifest? {
    if (!file.isFile()) return null
    return try {
      val text = SystemFileSystem.source(file).buffered().use { it.readString() }
      json.decodeFromString(DownloadManifest.serializer(), text)
    } catch (e: IOException) {
      ebark(e) { "Unable to read download manifest $file" }
      null
    } catch (e: SerializationException) {
      ebark(e) { "Corrupt download manifest $file" }
      null
    } catch (e: IllegalArgumentException) {
      ebark(e) { "Corrupt download manifest $file" }
      null
    }
  }

  companion object : Cork {
    override val tag: String = "OfflineDownloadStore"
    override val enabled: Boolean = true

    const val PART_SUFFIX = ".part"
    private const val MANIFEST_FILE = "manifest.json"
    private const val ITEM_DIR = "item"
    private const val EPISODES_DIR = "episodes"

    private val json = Json {
      ignoreUnknownKeys = true
    }

    /** The on-disk name for a downloaded track: its index plus the source file's extension. */
    fun fileName(trackIndex: Int, extension: String): String {
      val ext = extension.removePrefix(".").filter { it.isLetterOrDigit() }.take(8)
      return if (ext.isEmpty()) "$trackIndex" else "$trackIndex.$ext"
    }

    private fun String.asFileName(): String = replace(Regex("[^A-Za-z0-9_-]"), "_")
  }
}
