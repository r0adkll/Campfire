// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.network.di.BaseClient
import com.r0adkll.kimchi.annotations.ContributesTo
import io.ktor.client.HttpClient
import me.tatarka.inject.annotations.Provides

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
