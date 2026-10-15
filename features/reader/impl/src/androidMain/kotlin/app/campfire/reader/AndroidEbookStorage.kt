// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import android.app.Application
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.File

@ContributesBinding(AppScope::class)
@Inject
class AndroidEbookStorage(
  private val application: Application,
) : EbookStorage {
  override fun cacheDirectory(): String = File(application.cacheDir, "ebooks").absolutePath

  override fun downloadDirectory(): String = File(application.filesDir, "ebooks").absolutePath
}
