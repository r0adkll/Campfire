// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import com.r0adkll.flatprefs.doubleKey
import com.r0adkll.flatprefs.floatKey
import com.r0adkll.flatprefs.intKey
import com.r0adkll.flatprefs.longKey
import java.util.prefs.AbstractPreferences
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath

class JavaPreferencesMigrationTest {

  /** A java.util.prefs node in memory, holding text as desktop's FileSystemPreferences does. */
  private class InMemoryPreferences(initial: Map<String, String>) : AbstractPreferences(null, "") {
    val values = initial.toSortedMap()
    var flushes = 0

    override fun putSpi(key: String, value: String) {
      values[key] = value
    }

    override fun getSpi(key: String): String? = values[key]

    override fun removeSpi(key: String) {
      values.remove(key)
    }

    override fun keysSpi(): Array<String> = values.keys.toTypedArray()

    override fun flushSpi() {
      flushes++
    }

    override fun removeNodeSpi() = Unit
    override fun childrenNamesSpi(): Array<String> = emptyArray()
    override fun childSpi(name: String): AbstractPreferences = throw UnsupportedOperationException()
    override fun syncSpi() = Unit
  }

  private val legacy = InMemoryPreferences(
    mapOf(
      KEY_SOCKET_ENABLED to "false",
      PREF_FORWARD_TIME_MS to "45000",
      PREF_PLAYBACK_SPEED to "1.25",
      PREF_SYNC_INTERVAL_METERED to "120.0",
      KEY_THEME to "dark",
      KEY_DEVICE_ID to "device-1",
      "account_restore_snapshot" to """{"accounts":[]}""",
      "discover_scan_at_u1" to "1700000000000",
      "discover_scan_skipped_u1" to "3",
      "bookinfo_enabled_hardcover_u1" to "true",
      // Read from this node by the token, header and Hardcover storage, so they must stay
      "accessToken_u1" to "secret",
      "extraHeaders_u1" to "X-Auth:|:1",
      "hardcoverToken_u1" to "hc",
      // Not a setting this migration knows
      "pref_unknown" to "x",
      // Not a boolean, so java.util.prefs read it as the default
      KEY_DEVELOPER_MODE to "yes",
    ),
  )

  @Test
  fun `settings move over with their types`() = runTest {
    val file = newSettingsFile()
    val store = openSettingsStore(file, listOf(JavaPreferencesMigration(legacy)))
    val settings = FlatPreferencesSettings(store, backgroundScope)

    assertThat(settings.getBooleanOrNull(KEY_SOCKET_ENABLED)).isEqualTo(false)
    assertThat(settings.getLongOrNull(PREF_FORWARD_TIME_MS)).isEqualTo(45_000L)
    assertThat(settings.getFloatOrNull(PREF_PLAYBACK_SPEED)).isEqualTo(1.25f)
    assertThat(settings.getDoubleOrNull(PREF_SYNC_INTERVAL_METERED)).isEqualTo(120.0)
    assertThat(settings.getStringOrNull(KEY_THEME)).isEqualTo("dark")
    assertThat(settings.getStringOrNull(KEY_DEVICE_ID)).isEqualTo("device-1")
    assertThat(settings.getStringOrNull("account_restore_snapshot")).isEqualTo("""{"accounts":[]}""")
    assertThat(settings.getLongOrNull("discover_scan_at_u1")).isEqualTo(1_700_000_000_000L)
    assertThat(settings.getIntOrNull("discover_scan_skipped_u1")).isEqualTo(3)
    assertThat(settings.getBooleanOrNull("bookinfo_enabled_hardcover_u1")).isEqualTo(true)

    // Typed on disk, not just readable through the adapter
    val onDisk = readSettingsFile(file)
    assertThat(onDisk[longKey(PREF_FORWARD_TIME_MS)]).isEqualTo(45_000L)
    assertThat(onDisk[floatKey(PREF_PLAYBACK_SPEED)]).isEqualTo(1.25f)
    assertThat(onDisk[doubleKey(PREF_SYNC_INTERVAL_METERED)]).isEqualTo(120.0)
    assertThat(onDisk[intKey("discover_scan_skipped_u1")]).isEqualTo(3)
  }

  @Test
  fun `credentials and unknown keys stay in the old preferences`() = runTest {
    val store = openSettingsStore(newSettingsFile(), listOf(JavaPreferencesMigration(legacy)))
    val settings = FlatPreferencesSettings(store, backgroundScope)

    assertThat(settings.hasKey("accessToken_u1")).isFalse()
    assertThat(settings.hasKey("pref_unknown")).isFalse()
    assertThat(legacy.values.keys).containsOnly(
      "accessToken_u1",
      "extraHeaders_u1",
      "hardcoverToken_u1",
      "pref_unknown",
    )
    assertThat(legacy.flushes).isGreaterThan(0)
  }

  @Test
  fun `an invalid value is dropped, as it already read as the default`() = runTest {
    val store = openSettingsStore(newSettingsFile(), listOf(JavaPreferencesMigration(legacy)))
    val settings = FlatPreferencesSettings(store, backgroundScope)

    assertThat(settings.getBooleanOrNull(KEY_DEVELOPER_MODE)).isNull()
    assertThat(legacy.values[KEY_DEVELOPER_MODE]).isNull()
  }

  @Test
  fun `nothing is left to migrate afterwards`() = runTest {
    val migration = JavaPreferencesMigration(legacy)
    val store = openSettingsStore(newSettingsFile(), listOf(migration))

    assertThat(migration.shouldMigrate(store.current)).isFalse()
  }

  @Test
  fun `the desktop file sits beside the old preferences`() {
    val previous = System.setProperty("java.util.prefs.userRoot", "/home/listener")
    try {
      assertThat(desktopSettingsFile()).isEqualTo("/home/listener/.config/Campfire/flatprefs/settings.fpb".toPath())
    } finally {
      if (previous == null) {
        System.clearProperty("java.util.prefs.userRoot")
      } else {
        System.setProperty("java.util.prefs.userRoot", previous)
      }
    }
  }
}
