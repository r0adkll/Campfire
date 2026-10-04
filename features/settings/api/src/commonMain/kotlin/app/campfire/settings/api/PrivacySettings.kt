// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.Flow

/**
 * The user's consent and data-sharing choices for this install.
 */
interface PrivacySettings {

  fun observeHasEverConsented(): Flow<Boolean>
  fun setHasEverConsented(value: Boolean)

  fun setCrashReportingEnabled(value: Boolean)
  fun observeCrashReportingEnabled(): Flow<Boolean>

  /**
   * Keep each account's sign-in where it survives a reinstall, where the platform allows it
   */
  fun setKeepSignedInAfterReinstall(value: Boolean)
  fun observeKeepSignedInAfterReinstall(): Flow<Boolean>

  fun setAnalyticReportingEnabled(value: Boolean)
  fun observeAnalyticReportingEnabled(): Flow<Boolean>

  companion object {
    const val DEFAULT_CRASH_REPORTING_ENABLED: Boolean = true
    const val DEFAULT_ANALYTIC_REPORTING_ENABLED: Boolean = false
  }
}
