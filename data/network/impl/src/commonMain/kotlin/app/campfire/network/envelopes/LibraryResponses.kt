// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import app.campfire.network.models.Library
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MediaType
import kotlinx.serialization.Serializable

@Serializable
class AllLibrariesResponse(
  val libraries: List<Library>,
) : Envelope() {

  override fun applyPostage() {
    libraries.forEach { it.applyOrigin(origin) }
  }
}

@Serializable
class LibraryItemsResponse(
  val results: List<LibraryItemExpanded>,
  val total: Int,
  val limit: Int,
  val page: Int,
  val sortDesc: Boolean,
  val mediaType: MediaType,
  val minified: Boolean,
  val collapseseries: Boolean,
  val include: String,
  val offset: Int,
) : Envelope() {

  override fun applyPostage() {
    results.forEach { it.applyOrigin(origin) }
  }
}

@Serializable
class MinifiedLibraryItemsResponse(
  val results: List<LibraryItemMinified>,
  val total: Int,
  val limit: Int,
  val page: Int,
  val sortDesc: Boolean,
  val mediaType: MediaType,
  val minified: Boolean,
  val collapseseries: Boolean,
  val include: String,
  val offset: Int,
) : Envelope() {

  override fun applyPostage() {
    results.forEach { it.applyOrigin(origin) }
  }
}
