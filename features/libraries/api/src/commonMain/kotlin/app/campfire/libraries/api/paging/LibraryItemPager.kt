// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api.paging

import androidx.paging.Pager
import app.campfire.core.model.LibraryItem

typealias LibraryItemPager = Pager<Int, LibraryItem>
