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
import kotlinx.coroutines.flow.Flow
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

  private fun key(id: ProviderId): String {
    return "bookinfo_enabled_${id.key}_${userSession.userId.orEmpty()}"
  }
}
