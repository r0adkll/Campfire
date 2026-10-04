// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.PerBookSettings
import app.campfire.settings.api.SignedInSettings
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.UserThemeSettings
import kotlinx.coroutines.flow.Flow

/** A [SignedInSettings] fake for one account that never changes. */
class FakeSignedInSettings(
  val perBookSettings: PerBookSettings = FakePerBookSettings(),
  val userThemeSettings: UserThemeSettings = TestUserThemeSettings(),
) : SignedInSettings {
  override fun perBook(): PerBookSettings = perBookSettings
  override fun theme(): UserThemeSettings = userThemeSettings
  override fun observeThemeId(): Flow<ThemeKey> = userThemeSettings.observeThemeId()
}
