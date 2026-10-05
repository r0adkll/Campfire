// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import com.r0adkll.flatprefs.FlatPreferencesMigration
import com.r0adkll.flatprefs.MutableFlatPreferences
import com.r0adkll.flatprefs.doubleKey
import com.r0adkll.flatprefs.longKey

/**
 * Runs [delegate], then turns the [doubleKeys] it copied as longs back into doubles.
 *
 * multiplatform-settings kept doubles in SharedPreferences as a long holding the double's bits, and
 * FlatPrefs' `SharedPreferencesMigration` copies each value as the type SharedPreferences holds it.
 */
internal class DoubleBitsMigration(
  private val delegate: FlatPreferencesMigration,
  private val doubleKeys: Set<String> = LegacySettingTypes.doubleKeys,
) : FlatPreferencesMigration by delegate {

  override fun migrate(prefs: MutableFlatPreferences) {
    delegate.migrate(prefs)
    for (key in doubleKeys) {
      val bits = try {
        prefs[longKey(key)]
      } catch (_: ClassCastException) {
        // Already a double: the store had it and the old preferences didn't
        null
      } ?: continue
      prefs[doubleKey(key)] = Double.fromBits(bits)
    }
  }
}
