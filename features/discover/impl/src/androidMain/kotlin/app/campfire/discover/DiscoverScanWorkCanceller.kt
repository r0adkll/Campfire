// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import android.app.Application
import androidx.work.WorkManager
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

/**
 * Cancels the unique scan work when the user scope tears down (logout or
 * account switch), so [DiscoverScanWorker] never races a torn-down user graph.
 */
@ContributesMultibinding(UserScope::class, boundType = Scoped::class)
@Inject
class DiscoverScanWorkCanceller(
  private val application: Application,
) : Scoped {

  override suspend fun onDestroy() {
    WorkManager.getInstance(application).cancelUniqueWork(DiscoverScanWorker.UNIQUE_WORK_NAME)
  }
}
