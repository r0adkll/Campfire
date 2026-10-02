// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.campfire.settings.api.ThemeMode
import app.cash.turbine.test
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DataStoreSettingsTest {

  private class FakeDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    val stored = MutableStateFlow(initial)
    var writes = 0

    override val data = stored

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
      writes++
      return transform(stored.value).also { stored.value = it }
    }
  }

  private suspend fun TestScope.loaded(dataStore: FakeDataStore = FakeDataStore()): DataStoreSettings =
    DataStoreSettings(dataStore, backgroundScope as CoroutineScope).apply { load() }

  @Test
  fun `reading before load fails`() {
    val settings = DataStoreSettings(FakeDataStore(), TestScope())
    assertFailure { settings.getBoolean("key", false) }.isInstanceOf<IllegalStateException>()
  }

  @Test
  fun `load serves the stored values`() = runTest {
    val settings = loaded(
      FakeDataStore(
        preferencesOf(
          booleanPreferencesKey("enabled") to true,
          doublePreferencesKey("seconds") to 1.5,
          stringPreferencesKey("name") to "campfire",
        ),
      ),
    )

    assertThat(settings.getBoolean("enabled", false)).isTrue()
    assertThat(settings.getDouble("seconds", 0.0)).isEqualTo(1.5)
    assertThat(settings.getString("name", "")).isEqualTo("campfire")
    assertThat(settings.keys).containsOnly("enabled", "seconds", "name")
  }

  @Test
  fun `starting the load early reads the file before load is awaited`() = runTest {
    val dataStore = FakeDataStore(preferencesOf(booleanPreferencesKey("enabled") to true))
    val settings = DataStoreSettings(dataStore, backgroundScope)

    settings.startLoading()
    settings.startLoading()
    testScheduler.runCurrent()
    // The read has finished in the background, so changes to the file after it aren't seen
    dataStore.stored.value = emptyPreferences()
    settings.load()

    assertThat(settings.getBoolean("enabled", false)).isTrue()
  }

  @Test
  fun `a value read as another type is missing`() = runTest {
    val settings = loaded(FakeDataStore(preferencesOf(stringPreferencesKey("key") to "text")))

    assertThat(settings.getBooleanOrNull("key")).isNull()
    assertThat(settings.getLong("key", 7L)).isEqualTo(7L)
  }

  @Test
  fun `a write is readable at once and saved in the background`() = runTest {
    val dataStore = FakeDataStore()
    val settings = loaded(dataStore)

    settings.putLong("count", 3L)
    assertThat(settings.getLong("count", 0L)).isEqualTo(3L)

    testScheduler.runCurrent()
    assertThat(dataStore.data.first().asMap().mapKeys { it.key.name }).isEqualTo(mapOf("count" to 3L))
  }

  @Test
  fun `writing another type replaces the old value`() = runTest {
    val dataStore = FakeDataStore()
    val settings = loaded(dataStore)

    settings.putString("key", "text")
    settings.putBoolean("key", true)

    assertThat(settings.getBooleanOrNull("key")).isEqualTo(true)
    assertThat(settings.getStringOrNull("key")).isNull()
    assertThat(settings.size).isEqualTo(1)
  }

  @Test
  fun `writes made while one is saving collapse into one`() = runTest {
    val dataStore = FakeDataStore()
    val settings = loaded(dataStore)

    repeat(50) { settings.putFloat("volume", it / 50f) }
    testScheduler.runCurrent()

    assertThat(dataStore.writes).isLessThanOrEqualTo(2)
    assertThat(settings.getFloat("volume", 0f)).isEqualTo(49 / 50f)
    assertThat(dataStore.data.first().asMap().values.single()).isEqualTo(49 / 50f)
  }

  @Test
  fun `remove, hasKey and clear`() = runTest {
    val settings = loaded()
    settings.putInt("a", 1)
    settings.putInt("b", 2)

    settings.remove("a")
    assertThat(settings.hasKey("a")).isFalse()
    assertThat(settings.hasKey("b")).isTrue()

    settings.clear()
    assertThat(settings.size).isEqualTo(0)
  }

  @Test
  fun `listeners hear changes to their key only, and stop when deactivated`() = runTest {
    val settings = loaded()
    val heard = mutableListOf<Boolean?>()
    val listener = settings.addBooleanOrNullListener("enabled") { heard += it }

    settings.putBoolean("enabled", true)
    settings.putBoolean("enabled", true)
    settings.putBoolean("other", true)
    settings.remove("enabled")
    listener.deactivate()
    settings.putBoolean("enabled", false)

    assertThat(heard).containsExactly(true, null)
  }

  @Test
  fun `the settings delegates observe through it`() = runTest {
    val themeSettings = ThemeSettingsImpl(loaded(), backgroundScope)

    themeSettings.observeTheme().test {
      assertThat(awaitItem()).isEqualTo(ThemeMode.SYSTEM)
      themeSettings.themeMode = ThemeMode.DARK
      assertThat(awaitItem()).isEqualTo(ThemeMode.DARK)
    }
  }
}
