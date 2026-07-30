// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

interface ThemeSettings {

  var dynamicallyThemeItemDetail: Boolean
  fun observeDynamicallyThemeItemDetail(): StateFlow<Boolean>

  var dynamicallyThemePlayback: Boolean
  fun observeDynamicallyThemePlayback(): StateFlow<Boolean>
}
