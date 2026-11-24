// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSPropertyListBinaryFormat_v1_0
import platform.Foundation.NSPropertyListSerialization
import platform.Foundation.NSString
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

/**
 * What an interrupted background transfer leaves behind so it can carry on where it stopped:
 * `URLSession` resume data, which points at the bytes iOS kept, and the [TransferTag] offset of the
 * request it continues. A background transfer only hands its body over once it finishes, so without
 * this an interrupted file starts over from its part file's length.
 *
 * Kept beside the part file as `<part>.resume`, so deleting a download removes it too.
 */
@OptIn(ExperimentalForeignApi::class)
internal class SavedResumeData(val offset: Long, val data: NSData) {

  companion object {
    private const val SUFFIX = ".resume"
    private const val KEY_OFFSET = "offset"
    private const val KEY_DATA = "data"

    fun file(partFile: Path): Path = Path("$partFile$SUFFIX")

    /**
     * Saves [data] for [partFile], replacing what was there. Skipped once the download's directory
     * is gone, so a transfer stopped by a delete can't leave a stray file behind.
     */
    fun save(partFile: Path, offset: Long, data: NSData) {
      if (partFile.parent?.isDirectory() != true) return
      // A real NSDictionary: a Kotlin map handed to NSPropertyListSerialization serializes corrupted
      val saved = NSMutableDictionary().apply {
        setObject(NSNumber(longLong = offset), forKey = KEY_OFFSET as NSString)
        setObject(data, forKey = KEY_DATA as NSString)
      }
      NSPropertyListSerialization.dataWithPropertyList(saved, NSPropertyListBinaryFormat_v1_0, 0u, null)
        ?.writeToFile(file(partFile).toString(), atomically = true)
    }

    /** Reads and removes [partFile]'s resume data: it can only be used once. */
    fun take(partFile: Path): SavedResumeData? {
      val file = file(partFile)
      if (!file.isFile()) return null
      val saved = NSData.dataWithContentsOfFile(file.toString())
        ?.let { NSPropertyListSerialization.propertyListWithData(it, 0u, null, null) } as? Map<*, *>
      SystemFileSystem.delete(file, mustExist = false)
      val offset = when (val value = saved?.get(KEY_OFFSET)) {
        is NSNumber -> value.longLongValue
        is Number -> value.toLong()
        else -> return null
      }
      val data = saved[KEY_DATA] as? NSData ?: return null
      return SavedResumeData(offset, data)
    }
  }
}
