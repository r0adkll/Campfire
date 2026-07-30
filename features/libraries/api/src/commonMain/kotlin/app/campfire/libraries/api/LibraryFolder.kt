// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api

import app.campfire.core.parcelize.Parcelable
import app.campfire.core.parcelize.Parcelize

/**
 * A folder configured on a library. Carries enough information for the "Add podcast" flow to
 * compute the new library item's path (`${fullPath}/${sanitized title}`).
 */
@Parcelize
data class LibraryFolder(
  val id: String,
  val fullPath: String,
) : Parcelable
