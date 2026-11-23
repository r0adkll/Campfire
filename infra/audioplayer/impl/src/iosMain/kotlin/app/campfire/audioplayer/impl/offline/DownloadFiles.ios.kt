// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
internal actual fun Path.modifiedAtMs(): Long {
  val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(toString(), error = null) ?: return -1L
  val modified = attributes[NSFileModificationDate] as? NSDate ?: return -1L
  return (modified.timeIntervalSince1970 * 1000).toLong()
}
