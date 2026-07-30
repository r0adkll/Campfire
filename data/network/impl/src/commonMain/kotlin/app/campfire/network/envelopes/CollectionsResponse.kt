// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import app.campfire.network.models.Collection
import kotlinx.serialization.Serializable

@Serializable
class CollectionsResponse(
  val results: List<Collection>,
) : Envelope() {

  override fun applyPostage() {
    results.forEach { collection ->
      collection.applyOrigin(origin)
    }
  }
}
