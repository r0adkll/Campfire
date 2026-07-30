// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.core.filter.ContentFilter
import app.campfire.core.parcelize.Parcelize

@Parcelize
data class LibraryScreen(
  val filter: ContentFilter? = null,
) : BaseScreen(name = "Library")
