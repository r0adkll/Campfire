// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.DownloadSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A simple in-memory [DownloadSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeDownloadSettings : DownloadSettings {

  private val _downloadQueuedEpisodes = MutableStateFlow(false)
  override var downloadQueuedEpisodes: Boolean
    get() = _downloadQueuedEpisodes.value
    set(value) { _downloadQueuedEpisodes.value = value }
  override fun observeDownloadQueuedEpisodes(): StateFlow<Boolean> = _downloadQueuedEpisodes.asStateFlow()

  private val _deleteFinishedEpisodes = MutableStateFlow(false)
  override var deleteFinishedEpisodes: Boolean
    get() = _deleteFinishedEpisodes.value
    set(value) { _deleteFinishedEpisodes.value = value }
  override fun observeDeleteFinishedEpisodes(): StateFlow<Boolean> = _deleteFinishedEpisodes.asStateFlow()
}
