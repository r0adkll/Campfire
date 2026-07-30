// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.attributions

import com.mikepenz.aboutlibraries.Libs

interface LicenseAttributionLoader {

  /**
   * Load the library and license attribution used in this app
   */
  suspend fun load(): Libs
}
