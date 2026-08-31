// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audible.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.network.di.BaseClient
import com.r0adkll.kimchi.annotations.ContributesTo
import io.ktor.client.HttpClient
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier

/** Qualifies the [HttpClient] configured for the Audible catalog API. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AudibleClient

@ContributesTo(AppScope::class)
interface AudibleHttpClientModule {

  @AudibleClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideAudibleHttpClient(
    @BaseClient baseClient: HttpClient,
  ): HttpClient = baseClient.config {
    expectSuccess = false
  }
}
