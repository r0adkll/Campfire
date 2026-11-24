// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.files

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/**
 * The app's Application Support directory: kept, unlike Caches, and not shown in the Files app.
 * Its absolute path changes when the app is updated, so look it up rather than store it.
 */
fun applicationSupportDirectory(): String =
  NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String

/**
 * Creates [directory] if needed and keeps it, with everything in it, out of iCloud and device
 * backups. Application Support is backed up by default, and App Review rejects apps that back up
 * files they can download again, so every directory of downloaded server content goes through here.
 */
@OptIn(ExperimentalForeignApi::class)
fun createExcludedFromBackup(directory: String) {
  NSFileManager.defaultManager.createDirectoryAtPath(
    directory,
    withIntermediateDirectories = true,
    attributes = null,
    error = null,
  )
  NSURL.fileURLWithPath(directory, isDirectory = true)
    .setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
}
