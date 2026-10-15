// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

/**
 * Where ebook files live: [cacheDirectory] for books that were only opened, which the OS may
 * clear (a missing one is fetched again), and [downloadDirectory] for books downloaded to read
 * offline, kept until they're deleted.
 */
interface EbookStorage {
  fun cacheDirectory(): String

  fun downloadDirectory(): String
}
