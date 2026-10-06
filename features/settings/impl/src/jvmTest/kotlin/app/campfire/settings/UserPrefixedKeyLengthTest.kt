// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import assertk.assertThat
import assertk.assertions.isLessThanOrEqualTo
import com.russhwolf.settings.MapSettings
import java.util.prefs.Preferences
import kotlin.test.Test

class UserPrefixedKeyLengthTest {

  @Test
  fun `prefixed keys fit desktop's preferences`() {
    val app = MapSettings()
    // Audiobookshelf user ids are UUIDs
    val user = UserPrefixedSettings(app, "8a4e58d0-2e6e-4766-becc-965718dd9853")

    (LibraryViewStringKeys + LibraryViewBooleanKeys + KEY_SEEDED).forEach { key -> user.putBoolean(key, true) }

    app.keys.forEach { key -> assertThat(key.length, key).isLessThanOrEqualTo(Preferences.MAX_KEY_LENGTH) }
  }
}
