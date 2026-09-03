// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.test

import app.campfire.bookinfo.api.BookInfoProviderSettings
import app.campfire.bookinfo.api.ProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBookInfoProviderSettings : BookInfoProviderSettings {

  val enabled = MutableStateFlow(mapOf<ProviderId, Boolean>())
  override fun isEnabled(id: ProviderId): Boolean = enabled.value[id] ?: true
  override fun setEnabled(id: ProviderId, enabled: Boolean) {
    this.enabled.value = this.enabled.value + (id to enabled)
  }

  override fun observeEnabled(id: ProviderId): Flow<Boolean> = enabled.map { it[id] ?: true }

  val preferred = MutableStateFlow<ProviderId?>(null)
  override fun preferredProvider(): ProviderId? = preferred.value
  override fun setPreferredProvider(id: ProviderId?) {
    preferred.value = id
  }

  override fun observePreferredProvider(): Flow<ProviderId?> = preferred

  val seriesMissingBooks = MutableStateFlow(true)
  override fun isSeriesMissingBooksEnabled(): Boolean = seriesMissingBooks.value
  override fun setSeriesMissingBooksEnabled(enabled: Boolean) {
    seriesMissingBooks.value = enabled
  }

  override fun observeSeriesMissingBooksEnabled(): Flow<Boolean> = seriesMissingBooks
}
