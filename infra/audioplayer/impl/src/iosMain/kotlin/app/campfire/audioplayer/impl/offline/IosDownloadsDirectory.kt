// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/**
 * Where offline audio is kept: Application Support, which the system never purges and the Files app
 * doesn't show. Its absolute path changes when the app is updated, so it's looked up rather than
 * stored.
 */
internal fun iosDownloadsRoot(): Path {
  val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
    .first() as String
  return Path(support, "downloads")
}

/**
 * Creates the downloads directory and keeps it, and everything in it, out of iCloud and device
 * backups: the files can be downloaded again, and App Review rejects apps that back them up.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun prepareDownloadsRoot(root: Path) {
  SystemFileSystem.createDirectories(root)
  NSURL.fileURLWithPath(root.toString(), isDirectory = true)
    .setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
}
