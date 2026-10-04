// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.settings.api.PerBookSettings
import app.campfire.settings.api.UserThemeSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(UserScope::class)
interface UserSettingsComponent {

  /** Reached through [ComponentHolderSignedInSettings] by app-wide code. */
  val perBookSettings: PerBookSettings
  val userThemeSettings: UserThemeSettings

  @UserSettings
  @Provides
  fun provideUserSettingsStore(
    userSession: UserSession,
    stores: DefaultUserSettingsStores,
  ): SettingsStore = stores.storeFor(userSession.userId)
}
