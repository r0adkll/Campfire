// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.app

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.PrivacySettings
import app.campfire.settings.store.AppSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<PrivacySettings>())
@Inject
class PrivacySettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : PrivacySettings, AppSettings() {

  // These are user opt-in, so default false
  private val hasEverConsentedProperty = booleanSetting(KEY_HAS_CONSENTED, false)
  override val hasEverConsented: Boolean by hasEverConsentedProperty
  override fun setHasEverConsented(value: Boolean) = hasEverConsentedProperty.set(value)
  override fun observeHasEverConsented(): StateFlow<Boolean> = hasEverConsentedProperty.observe()

  private val crashReportingProperty = booleanSetting(KEY_CRASH_REPORTING, true)
  override val crashReportingEnabled: Boolean by crashReportingProperty
  override fun setCrashReportingEnabled(value: Boolean) = crashReportingProperty.set(value)
  override fun observeCrashReportingEnabled(): StateFlow<Boolean> = crashReportingProperty.observe()

  private val keepSignedInAfterReinstallProperty = booleanSetting(KEY_KEEP_SIGNED_IN_AFTER_REINSTALL, true)
  override val keepSignedInAfterReinstall: Boolean by keepSignedInAfterReinstallProperty
  override fun setKeepSignedInAfterReinstall(value: Boolean) = keepSignedInAfterReinstallProperty.set(value)
  override fun observeKeepSignedInAfterReinstall(): StateFlow<Boolean> =
    keepSignedInAfterReinstallProperty.observe()

  private val analyticReportingProperty = booleanSetting(KEY_ANALYTIC_REPORTING, false)
  override val analyticReportingEnabled: Boolean by analyticReportingProperty
  override fun setAnalyticReportingEnabled(value: Boolean) = analyticReportingProperty.set(value)
  override fun observeAnalyticReportingEnabled(): StateFlow<Boolean> = analyticReportingProperty.observe()
}

internal const val KEY_HAS_CONSENTED = "pref_has_consented"
internal const val KEY_CRASH_REPORTING = "pref_crash_reporting"
internal const val KEY_KEEP_SIGNED_IN_AFTER_REINSTALL = "pref_keep_signed_in_after_reinstall"
internal const val KEY_ANALYTIC_REPORTING = "pref_analytic_reporting"
