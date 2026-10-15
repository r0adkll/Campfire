// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.File

@ContributesBinding(AppScope::class)
@Inject
class DesktopEbookStorage : EbookStorage {
  override fun cacheDirectory(): String = File(System.getProperty("java.io.tmpdir"), "campfire-ebooks").absolutePath

  // Beside the audio downloads (see DesktopOfflineDownloadManager)
  override fun downloadDirectory(): String {
    val userRoot = System.getProperty("java.util.prefs.userRoot", System.getProperty("user.home"))
    return File(userRoot, ".config/Campfire/ebooks").absolutePath
  }
}
