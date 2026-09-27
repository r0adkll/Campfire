// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.di

import app.campfire.core.di.AppScope
import app.campfire.ui.theming.cache.Cache
import app.campfire.ui.theming.cache.DiskCache
import app.campfire.ui.theming.cache.DiskSwatchCache
import app.campfire.ui.theming.cache.DiskThemeCache
import app.campfire.ui.theming.cache.InMemoryCache
import app.campfire.ui.theming.theme.ComputedTheme
import com.r0adkll.swatchbuckler.compose.Swatch
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface ThemeManagerComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSwatchMemoryCache(): Cache<Swatch> = InMemoryCache()

  @Provides
  fun provideSwatchDiskCache(diskCache: DiskSwatchCache): DiskCache<Swatch> = diskCache

  @SingleIn(AppScope::class)
  @Provides
  fun provideThemeMemoryCache(): Cache<ComputedTheme> = InMemoryCache()

  @Provides
  fun provideThemeDiskCache(diskCache: DiskThemeCache): DiskCache<ComputedTheme> = diskCache
}
