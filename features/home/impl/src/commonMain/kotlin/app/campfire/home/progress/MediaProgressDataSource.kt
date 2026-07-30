// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.progress

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import kotlinx.coroutines.flow.Flow

interface MediaProgressDataSource {

  fun observeMediaProgress(ids: Set<LibraryItemId>): Flow<Map<LibraryItemId, MediaProgress>>
}
