// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.openlibrary.di

import app.campfire.core.di.AppScope
import app.campfire.network.di.BaseClient
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

/** Qualifies the [HttpClient] configured for the Open Library REST API. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class OpenLibraryClient

@ContributesTo(AppScope::class)
interface OpenLibraryHttpClientModule {

  // The base client already sends the identifying app User-Agent that Open
  // Library's API policy asks for.
  @OpenLibraryClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideOpenLibraryHttpClient(
    @BaseClient baseClient: HttpClient,
  ): HttpClient = baseClient.config {
    expectSuccess = false
  }
}
