// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

import kotlinx.serialization.json.Json

// This must match the JSON configuration in
// HttpClientModule.kt
internal val TestJson = Json {
  isLenient = true
  ignoreUnknownKeys = true
}
