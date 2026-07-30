// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api

/**
 * One-shot bundle of library state the "Add podcast" flow needs: the [folders] available as a
 * destination, and the library's configured iTunes [searchRegion] (which the search screen passes
 * to the server's podcast-search endpoint).
 */
data class AddPodcastContext(
  val folders: List<LibraryFolder>,
  val searchRegion: String?,
)
