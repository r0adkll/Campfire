// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.offline

sealed interface OfflineStatus {
  data object None : OfflineStatus
  data object Queued : OfflineStatus
  data class Downloading(val progress: Float) : OfflineStatus
  data object Available : OfflineStatus
  data object Failed : OfflineStatus
}
