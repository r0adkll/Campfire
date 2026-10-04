// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.app

import app.campfire.core.di.AppScope
import app.campfire.settings.api.PrivacySettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<PrivacySettings>())
@Inject
class PrivacySettingsImpl(
  override val store: SettingsStore,
) : PrivacySettings, AppSettings() {

  // These are user opt-in, so default false
  private val hasEverConsentedProperty = booleanSetting(KEY_HAS_CONSENTED, false)
  override fun setHasEverConsented(value: Boolean) = hasEverConsentedProperty.set(value)
  override fun observeHasEverConsented(): Flow<Boolean> = hasEverConsentedProperty.observe()

  private val crashReportingProperty =
    booleanSetting(KEY_CRASH_REPORTING, PrivacySettings.DEFAULT_CRASH_REPORTING_ENABLED)
  override fun setCrashReportingEnabled(value: Boolean) = crashReportingProperty.set(value)
  override fun observeCrashReportingEnabled(): Flow<Boolean> = crashReportingProperty.observe()

  private val keepSignedInAfterReinstallProperty = booleanSetting(KEY_KEEP_SIGNED_IN_AFTER_REINSTALL, true)
  override fun setKeepSignedInAfterReinstall(value: Boolean) = keepSignedInAfterReinstallProperty.set(value)
  override fun observeKeepSignedInAfterReinstall(): Flow<Boolean> =
    keepSignedInAfterReinstallProperty.observe()

  private val analyticReportingProperty =
    booleanSetting(KEY_ANALYTIC_REPORTING, PrivacySettings.DEFAULT_ANALYTIC_REPORTING_ENABLED)
  override fun setAnalyticReportingEnabled(value: Boolean) = analyticReportingProperty.set(value)
  override fun observeAnalyticReportingEnabled(): Flow<Boolean> = analyticReportingProperty.observe()
}

internal const val KEY_HAS_CONSENTED = "pref_has_consented"
internal const val KEY_CRASH_REPORTING = "pref_crash_reporting"
internal const val KEY_KEEP_SIGNED_IN_AFTER_REINSTALL = "pref_keep_signed_in_after_reinstall"
internal const val KEY_ANALYTIC_REPORTING = "pref_analytic_reporting"
