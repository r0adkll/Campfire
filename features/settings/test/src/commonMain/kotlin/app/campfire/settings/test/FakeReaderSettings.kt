// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.ReaderLayout
import app.campfire.settings.api.ReaderSettings
import app.campfire.settings.api.ReaderTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeReaderSettings : ReaderSettings {

  private val _theme = MutableStateFlow(ReaderTheme.FollowApp)
  override var theme: ReaderTheme
    get() = _theme.value
    set(value) { _theme.value = value }
  override fun observeTheme(): StateFlow<ReaderTheme> = _theme.asStateFlow()

  private val _layout = MutableStateFlow(ReaderLayout())
  override var layout: ReaderLayout
    get() = _layout.value
    set(value) { _layout.value = value }
  override fun observeLayout(): StateFlow<ReaderLayout> = _layout.asStateFlow()

  private val _volumeKeysTurnPages = MutableStateFlow(true)
  override var volumeKeysTurnPages: Boolean
    get() = _volumeKeysTurnPages.value
    set(value) { _volumeKeysTurnPages.value = value }
  override fun observeVolumeKeysTurnPages(): StateFlow<Boolean> = _volumeKeysTurnPages.asStateFlow()

  private val _keepScreenOn = MutableStateFlow(true)
  override var keepScreenOn: Boolean
    get() = _keepScreenOn.value
    set(value) { _keepScreenOn.value = value }
  override fun observeKeepScreenOn(): StateFlow<Boolean> = _keepScreenOn.asStateFlow()
}
