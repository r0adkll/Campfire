// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.impl

import android.app.Application
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.File

@ContributesBinding(AppScope::class)
@Inject
class AndroidCoverStorage(
  private val application: Application,
) : CoverStorage {
  override fun directory(): String = File(application.filesDir, "covers").absolutePath
}
