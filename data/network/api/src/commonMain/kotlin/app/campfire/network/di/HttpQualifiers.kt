// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import me.tatarka.inject.annotations.Qualifier

@Qualifier
annotation class BaseClient

@Qualifier
annotation class UserClient

/**
 * A client for streaming large files (offline downloads) to disk. It carries no response cache
 * or body-reading inspection, which would hold whole files in memory, and no auth: callers
 * attach the account's credentials themselves so the [UserClient] stays the only token refresher.
 */
@Qualifier
annotation class DownloadClient
