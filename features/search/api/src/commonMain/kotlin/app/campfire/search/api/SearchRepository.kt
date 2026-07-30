// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.search.api

import kotlinx.coroutines.flow.Flow

interface SearchRepository {

  fun searchCurrentLibrary(query: String): Flow<SearchResult>
}
