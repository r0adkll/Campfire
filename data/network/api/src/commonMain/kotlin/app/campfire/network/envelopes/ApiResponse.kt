// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

/**
 * An API response envelope that contains the server URL that was used for the current request.
 */
class ApiResponse<T>(
  val data: T,
  val serverUrl: String,
)
