// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

/**
 * The user's consent and data-sharing choices for this install.
 */
interface PrivacySettings {

  var hasEverConsented: Boolean

  var crashReportingEnabled: Boolean
  fun observeCrashReportingEnabled(): StateFlow<Boolean>

  /**
   * Keep each account's sign-in where it survives a reinstall, where the platform allows it
   */
  var keepSignedInAfterReinstall: Boolean
  fun observeKeepSignedInAfterReinstall(): StateFlow<Boolean>

  var analyticReportingEnabled: Boolean
  fun observeAnalyticReportingEnabled(): StateFlow<Boolean>
}
