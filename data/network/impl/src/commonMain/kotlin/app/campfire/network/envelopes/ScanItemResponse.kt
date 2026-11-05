// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

/** What the server found scanning an item's files again: `UPDATED`, `UPTODATE`, or `NOTHING`. */
@Serializable
data class ScanItemResponse(val result: String)
