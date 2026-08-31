// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo

import app.campfire.bookinfo.api.BookInfoProviderSettings
import app.campfire.bookinfo.api.ProviderId
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultBookInfoProviderSettings(
  private val settings: ObservableSettings,
  private val userSession: UserSession,
) : BookInfoProviderSettings {

  override fun isEnabled(id: ProviderId): Boolean {
    return settings.getBoolean(key(id), defaultValue = true)
  }

  override fun setEnabled(id: ProviderId, enabled: Boolean) {
    settings.putBoolean(key(id), enabled)
  }

  override fun observeEnabled(id: ProviderId): Flow<Boolean> {
    return settings.getBooleanFlow(key(id), defaultValue = true)
  }

  override fun preferredProvider(): ProviderId? {
    return settings.getStringOrNull(preferredKey()).toProviderId()
  }

  override fun setPreferredProvider(id: ProviderId?) {
    if (id == null) {
      settings.remove(preferredKey())
    } else {
      settings.putString(preferredKey(), id.key)
    }
  }

  override fun observePreferredProvider(): Flow<ProviderId?> {
    return settings.getStringOrNullFlow(preferredKey()).map { it.toProviderId() }
  }

  private fun String?.toProviderId(): ProviderId? {
    return this?.let { stored -> ProviderId.entries.firstOrNull { it.key == stored } }
  }

  private fun key(id: ProviderId): String {
    return "bookinfo_enabled_${id.key}_${userSession.userId.orEmpty()}"
  }

  private fun preferredKey(): String {
    return "bookinfo_preferred_${userSession.userId.orEmpty()}"
  }
}
