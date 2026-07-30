// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.auto

interface AndroidAuto {
  fun isAvailable(): Boolean
  fun openSettings()
}
