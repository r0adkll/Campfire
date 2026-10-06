// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.UserScope
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.settings.api.UserSettingsStore
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/** Qualifies the signed-in account's [ObservableSettings], in the user graph. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserSettings

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<UserSettingsStore>())
@Inject
class DefaultUserSettingsStore(
  private val settings: ObservableSettings,
) : UserSettingsStore {

  /**
   * [userId]'s settings. An account starts from the values the app kept for everyone before they moved to each
   * account, copied the first time its settings are opened, so nobody's library layout resets; accounts added
   * later start from the same values. Signed out, there's no account, so these are the app-wide values.
   */
  fun settingsFor(userId: UserId?): ObservableSettings {
    if (userId == null) return settings
    return UserPrefixedSettings(settings, userId).also(::seed)
  }

  override fun clear(userId: UserId) = UserPrefixedSettings(settings, userId).clear()

  private fun seed(user: ObservableSettings) {
    if (user.getBoolean(KEY_SEEDED, false)) return
    LibraryViewStringKeys.filterNot(user::hasKey).forEach { key ->
      settings.getStringOrNull(key)?.let { user.putString(key, it) }
    }
    LibraryViewBooleanKeys.filterNot(user::hasKey).forEach { key ->
      settings.getBooleanOrNull(key)?.let { user.putBoolean(key, it) }
    }
    user.putBoolean(KEY_SEEDED, true)
  }
}

@ContributesTo(UserScope::class)
interface UserSettingsComponent {

  @UserSettings
  @SingleIn(UserScope::class)
  @Provides
  fun provideUserSettings(
    userSession: UserSession,
    store: DefaultUserSettingsStore,
  ): ObservableSettings = store.settingsFor(userSession.userId)
}

internal const val KEY_SEEDED = "settings_seeded"
