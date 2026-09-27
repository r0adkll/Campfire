// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.di

import app.campfire.core.di.AppScope
import app.campfire.network.di.BaseClient
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

@ContributesTo(AppScope::class)
interface HardcoverHttpClientModule {

  @HardcoverClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideHardcoverHttpClient(
    @BaseClient baseClient: HttpClient,
  ): HttpClient = baseClient.config {
    expectSuccess = false
  }
}
