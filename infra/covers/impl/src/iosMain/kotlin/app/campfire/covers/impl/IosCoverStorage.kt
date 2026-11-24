// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.impl

import app.campfire.core.di.AppScope
import app.campfire.core.files.applicationSupportDirectory
import app.campfire.core.files.createExcludedFromBackup
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class IosCoverStorage : CoverStorage {

  // Application Support is kept, unlike Caches, and not shown in the Files app. Covers can be
  // fetched again, so they stay out of backups.
  private val directory by lazy {
    "${applicationSupportDirectory()}/covers".also(::createExcludedFromBackup)
  }

  override fun directory(): String = directory
}
