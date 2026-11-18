// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.automotive

import android.app.Application
import app.campfire.core.attributions.LicenseAttributionLoader
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withJson
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@Inject
class AutomotiveLicenseAttributionLoader(
  private val application: Application,
  private val dispatcherProvider: DispatcherProvider,
) : LicenseAttributionLoader {

  override suspend fun load(): Libs {
    return withContext(dispatcherProvider.io) {
      Libs.Builder()
        .withJson(application, R.raw.aboutlibraries)
        .build()
    }
  }
}
