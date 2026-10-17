// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.impl

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.File

@ContributesBinding(AppScope::class)
@Inject
class DesktopCoverStorage : CoverStorage {
  // Beside the audio and ebook downloads (see DesktopOfflineDownloadManager)
  override fun directory(): String {
    val userRoot = System.getProperty("java.util.prefs.userRoot", System.getProperty("user.home"))
    return File(userRoot, ".config/Campfire/covers").absolutePath
  }
}
