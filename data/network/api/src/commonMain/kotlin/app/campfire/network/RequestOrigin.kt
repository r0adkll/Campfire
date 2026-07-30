// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

/**
 * The server origin of the request
 */
sealed interface RequestOrigin {
  data class Url(val serverUrl: String) : RequestOrigin
  data object None : RequestOrigin
}
