// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * The server's answer to a media update. It also sends the item, but not in the expanded shape the
 * app keeps, so the item is fetched again instead.
 */
@Serializable
data class UpdateMediaResponse(
  /** Whether anything changed, or the cover's address when a new cover was all that changed */
  @SerialName("updated") val result: JsonElement,
) {
  val updated: Boolean
    get() = (result as? JsonPrimitive)?.let { it.booleanOrNull ?: (it.isString && it.content.isNotEmpty()) } == true
}
