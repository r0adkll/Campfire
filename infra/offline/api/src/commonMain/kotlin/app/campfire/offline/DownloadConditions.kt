// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Whether downloads may use the network right now: not while working offline, and only on Wi-Fi
 * while "Download on Wi-Fi only" is on. A [Downloader] whose platform doesn't already hold its
 * downloads to these rules waits for them before fetching.
 */
interface DownloadConditions {

  /** Whether downloads may fetch, as it changes. */
  fun observeAllowed(): Flow<Boolean>
}

/** Whether downloads may fetch right now. */
suspend fun DownloadConditions.isAllowed(): Boolean = observeAllowed().first()

/** Suspends until downloads may fetch. */
suspend fun DownloadConditions.awaitAllowed() {
  observeAllowed().first { it }
}
