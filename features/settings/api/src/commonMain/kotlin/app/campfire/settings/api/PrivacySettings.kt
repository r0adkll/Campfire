// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

/**
 * The user's consent and data-sharing choices for this install.
 */
interface PrivacySettings {

  val hasEverConsented: Boolean
  fun observeHasEverConsented(): StateFlow<Boolean>
  fun setHasEverConsented(value: Boolean)

  val crashReportingEnabled: Boolean
  fun setCrashReportingEnabled(value: Boolean)
  fun observeCrashReportingEnabled(): StateFlow<Boolean>

  /**
   * Keep each account's sign-in where it survives a reinstall, where the platform allows it
   */
  val keepSignedInAfterReinstall: Boolean
  fun setKeepSignedInAfterReinstall(value: Boolean)
  fun observeKeepSignedInAfterReinstall(): StateFlow<Boolean>

  val analyticReportingEnabled: Boolean
  fun setAnalyticReportingEnabled(value: Boolean)
  fun observeAnalyticReportingEnabled(): StateFlow<Boolean>
}
