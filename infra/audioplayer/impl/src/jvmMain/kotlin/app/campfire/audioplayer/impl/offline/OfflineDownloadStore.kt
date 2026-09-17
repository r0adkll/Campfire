// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownloadKey
import app.campfire.core.logging.Cork
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The on-disk layout of desktop offline downloads:
 *
 * ```
 * <root>/<libraryItemId>/item/manifest.json          a book (or item-level) download
 * <root>/<libraryItemId>/item/<trackIndex>.<ext>     a completed track
 * <root>/<libraryItemId>/item/<trackIndex>.<ext>.part a track still downloading
 * <root>/<libraryItemId>/episodes/<episodeId>/...    a single podcast episode download
 * ```
 */
internal class OfflineDownloadStore(
  val root: File,
) {

  fun directory(key: OfflineDownloadKey): File {
    val itemDir = File(root, key.libraryItemId.asFileName())
    val episodeId = key.episodeId
    return if (episodeId == null) {
      File(itemDir, ITEM_DIR)
    } else {
      File(File(itemDir, EPISODES_DIR), episodeId.asFileName())
    }
  }

  fun completedFile(key: OfflineDownloadKey, file: DownloadManifest.File): File =
    File(directory(key), file.fileName)

  fun partFile(key: OfflineDownloadKey, file: DownloadManifest.File): File =
    File(directory(key), file.fileName + PART_SUFFIX)

  /** Every readable manifest on disk. Unreadable ones are skipped, not deleted. */
  fun readAll(): List<DownloadManifest> {
    val itemDirs = root.listFiles { file -> file.isDirectory } ?: return emptyList()
    return itemDirs.flatMap { itemDir ->
      val episodeDirs = File(itemDir, EPISODES_DIR).listFiles { file -> file.isDirectory }.orEmpty()
      (listOf(File(itemDir, ITEM_DIR)) + episodeDirs)
        .mapNotNull { dir -> read(File(dir, MANIFEST_FILE)) }
    }
  }

  fun write(manifest: DownloadManifest) {
    val dir = directory(manifest.key)
    if (!dir.mkdirs() && !dir.isDirectory) throw IOException("Unable to create $dir")
    val target = File(dir, MANIFEST_FILE)
    val temp = File(dir, "$MANIFEST_FILE$PART_SUFFIX")
    temp.writeText(json.encodeToString(DownloadManifest.serializer(), manifest))
    try {
      Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
      Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
  }

  /** Removes the download's directory, and its item directory once nothing else lives there. */
  fun delete(key: OfflineDownloadKey) {
    directory(key).deleteRecursively()
    val itemDir = File(root, key.libraryItemId.asFileName())
    File(itemDir, EPISODES_DIR).delete()
    itemDir.delete()
  }

  private fun read(file: File): DownloadManifest? {
    if (!file.isFile) return null
    return try {
      json.decodeFromString(DownloadManifest.serializer(), file.readText())
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
