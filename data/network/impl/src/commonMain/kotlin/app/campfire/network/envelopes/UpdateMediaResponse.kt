// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

/**
 * The server's answer to a media update. It also sends the item, but not in the expanded shape the
 * app keeps, so the item is fetched again instead.
 */
@Serializable
data class UpdateMediaResponse(val updated: Boolean)
