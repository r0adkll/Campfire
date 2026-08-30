// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.di

import me.tatarka.inject.annotations.Qualifier

/** Qualifies the [io.ktor.client.HttpClient] configured for the Hardcover GraphQL API. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class HardcoverClient

/** Qualifies the encrypted [com.russhwolf.settings.Settings] holding Hardcover tokens. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class HardcoverSettings
