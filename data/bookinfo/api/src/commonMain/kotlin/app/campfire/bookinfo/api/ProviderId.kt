// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

/**
 * Stable identifier for a book information provider. [key] is used for
 * persistence (settings keys, database rows) and must never change once shipped.
 */
enum class ProviderId(val key: String) {
  Hardcover("hardcover"),
  OpenLibrary("openlibrary"),
  Audnexus("audnexus"),
}
