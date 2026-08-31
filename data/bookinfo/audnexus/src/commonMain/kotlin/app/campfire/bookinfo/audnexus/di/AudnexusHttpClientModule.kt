// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audnexus.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.network.di.BaseClient
import com.r0adkll.kimchi.annotations.ContributesTo
import io.ktor.client.HttpClient
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier

/** Qualifies the [HttpClient] configured for the Audnexus REST API. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AudnexusClient

@ContributesTo(AppScope::class)
interface AudnexusHttpClientModule {

  @AudnexusClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideAudnexusHttpClient(
    @BaseClient baseClient: HttpClient,
  ): HttpClient = baseClient.config {
    expectSuccess = false
  }
}
