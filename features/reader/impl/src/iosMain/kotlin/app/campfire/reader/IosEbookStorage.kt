// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@ContributesBinding(AppScope::class)
@Inject
class IosEbookStorage : EbookStorage {
  override fun cacheDirectory(): String = "${directory(NSCachesDirectory)}/ebooks"

  // Application Support is kept, and backed up, but not shown in the Files app
  override fun downloadDirectory(): String = "${directory(NSApplicationSupportDirectory)}/ebooks"

  private fun directory(directory: NSSearchPathDirectory): String =
    NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true).first() as String
}
