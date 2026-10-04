// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.settings.store.PreferencesPlatformComponent
import app.campfire.settings.store.SettingsDispatcher
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@ContributesTo(AppScope::class)
interface SettingsComponent : PreferencesPlatformComponent {

  @Provides
  @SingleIn(AppScope::class)
  @SettingsDispatcher
  fun provideSettingsDispatcher(): CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
}
