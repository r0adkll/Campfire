// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.di.AppScope
import app.campfire.core.files.applicationSupportDirectory
import app.campfire.core.files.createExcludedFromBackup
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@ContributesBinding(AppScope::class)
@Inject
class IosEbookStorage : EbookStorage {

  private val downloads by lazy {
    "${applicationSupportDirectory()}/ebooks".also(::createExcludedFromBackup)
  }

  override fun cacheDirectory(): String =
    "${NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first()}/ebooks"

  // Application Support is kept, and not shown in the Files app. Downloads can be fetched again,
  // so they stay out of backups.
  override fun downloadDirectory(): String = downloads
}
