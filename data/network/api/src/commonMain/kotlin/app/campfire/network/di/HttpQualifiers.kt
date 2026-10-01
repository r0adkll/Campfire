// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import dev.zacsweers.metro.Qualifier

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

/**
 * A client for the audio player to stream and download media. It authenticates as the session
 * user through the same bearer provider as the [UserClient], so a 401 refreshes the shared token
 * rather than a separate copy, but carries no response cache or body-reading inspection.
 */
@Qualifier
annotation class AudioPlayerClient
