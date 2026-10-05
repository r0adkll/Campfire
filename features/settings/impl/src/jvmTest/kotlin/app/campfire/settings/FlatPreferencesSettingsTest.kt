// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.r0adkll.flatprefs.FlatPreferencesStore
import com.r0adkll.flatprefs.booleanKey
import com.r0adkll.flatprefs.doubleKey
import com.r0adkll.flatprefs.stringKey
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalSettingsApi::class, ExperimentalCoroutinesApi::class)
class FlatPreferencesSettingsTest {

  private val file = newSettingsFile()
  private val store: FlatPreferencesStore = openSettingsStore(file, migrations = emptyList())

  private fun TestScope.settings(listenerScope: CoroutineScope = backgroundScope) =
    FlatPreferencesSettings(store, listenerScope)

  @Test
  fun `each type reads back what was put`() = runTest {
    val settings = settings()

    settings.putInt("int", 7)
    settings.putLong("long", 1L shl 40)
    settings.putFloat("float", 1.5f)
    settings.putDouble("double", 90.25)
    settings.putBoolean("boolean", true)
    settings.putString("string", "dark")

    assertThat(settings.getInt("int", 0)).isEqualTo(7)
    assertThat(settings.getLong("long", 0L)).isEqualTo(1L shl 40)
    assertThat(settings.getFloat("float", 0f)).isEqualTo(1.5f)
    assertThat(settings.getDouble("double", 0.0)).isEqualTo(90.25)
    assertThat(settings.getBoolean("boolean", false)).isTrue()
    assertThat(settings.getString("string", "")).isEqualTo("dark")
  }

  @Test
  fun `absent keys read as the default or null`() = runTest {
    val settings = settings()

    assertThat(settings.getInt("missing", 3)).isEqualTo(3)
    assertThat(settings.getIntOrNull("missing")).isNull()
    assertThat(settings.getDoubleOrNull("missing")).isNull()
    assertThat(settings.getStringOrNull("missing")).isNull()
    assertThat(settings.hasKey("missing")).isFalse()
  }

  @Test
  fun `doubles are stored as doubles, not as long bits`() = runTest {
    settings().putDouble("timeout", 30.0)

    assertThat(store[doubleKey("timeout")]).isEqualTo(30.0)
  }

  @Test
  fun `a value of another type reads as unset instead of throwing`() = runTest {
    val settings = settings()
    settings.putString("speed", "fast")

    assertThat(settings.getFloat("speed", 1f)).isEqualTo(1f)
    assertThat(settings.getFloatOrNull("speed")).isNull()
    assertThat(settings.hasKey("speed")).isTrue()
  }

  @Test
  fun `putting another type replaces the value`() = runTest {
    val settings = settings()
    settings.putString("count", "3")
    settings.putInt("count", 3)

    assertThat(settings.getInt("count", 0)).isEqualTo(3)
    assertThat(settings.getStringOrNull("count")).isNull()
  }

  @Test
  fun `remove, keys, size and clear match by name whatever the type`() = runTest {
    val settings = settings()
    settings.putBoolean("a", true)
    settings.putDouble("b", 2.0)
    settings.putString("c", "x")

    assertThat(settings.keys).containsOnly("a", "b", "c")
    assertThat(settings.size).isEqualTo(3)

    settings.remove("b")
    assertThat(settings.keys).containsOnly("a", "c")

    settings.clear()
    assertThat(settings.keys).isEmpty()
    assertThat(settings.size).isEqualTo(0)
  }

  @Test
  fun `writes reach the file in the background`() = runTest {
    settings().putBoolean("synced", true)
    store.flush()

    assertThat(readSettingsFile(file)[booleanKey("synced")]).isEqualTo(true)
  }

  @Test
  fun `listeners fire only when their key's value changes`() = runTest {
    val settings = settings()
    val seen = mutableListOf<Boolean>()
    settings.addBooleanListener("enabled", defaultValue = false) { seen += it }

    settings.putBoolean("enabled", false) // same as the default it already reads as
    runCurrent()
    settings.putString("other", "x")
    runCurrent()
    settings.putBoolean("enabled", true)
    runCurrent()
    settings.remove("enabled")
    runCurrent()

    assertThat(seen).containsExactly(true, false)
  }

  @Test
  fun `nullable listeners see removals as null`() = runTest {
    val settings = settings()
    val seen = mutableListOf<String?>()
    settings.addStringOrNullListener("user") { seen += it }

    settings.putString("user", "u1")
    runCurrent()
    settings.remove("user")
    runCurrent()

    assertThat(seen).containsExactly("u1", null)
  }

  @Test
  fun `a deactivated listener stops firing`() = runTest {
    val settings = settings()
    val seen = mutableListOf<Long>()
    val listener = settings.addLongListener("at", defaultValue = 0L) { seen += it }

    settings.putLong("at", 1L)
    runCurrent()
    listener.deactivate()
    settings.putLong("at", 2L)
    runCurrent()

    assertThat(seen).containsExactly(1L)
  }

  @Test
  fun `clear notifies every listener`() = runTest {
    val settings = settings()
    settings.putBoolean("a", true)
    settings.putString("b", "x")
    val seen = mutableListOf<Any?>()
    settings.addBooleanListener("a", defaultValue = false) { seen += it }
    settings.addStringOrNullListener("b") { seen += it }

    settings.clear()
    runCurrent()

    assertThat(seen).containsOnly(false, null)
  }

  @Test
  fun `multiplatform-settings flows emit the current value then each change`() = runTest {
    val settings = settings()

    settings.getBooleanFlow("enabled", defaultValue = false).test {
      assertThat(awaitItem()).isFalse()
      settings.putBoolean("enabled", true)
      assertThat(awaitItem()).isTrue()
      settings.putBoolean("enabled", true)
      settings.putBoolean("enabled", false)
      assertThat(awaitItem()).isFalse()
    }

    settings.getStringOrNullFlow("theme").test {
      assertThat(awaitItem()).isNull()
      store.edit { it[stringKey("theme")] = "dark" } // a write that didn't go through the adapter
      assertThat(awaitItem()).isEqualTo("dark")
    }
  }
}
