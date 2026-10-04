// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.ObservableSettings
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsTest {

  private val settings = MapSettings()

  /** Reads and writes run as soon as they're made, like a settings store with nothing queued. */
  private fun TestScope.createSettings() = TestAppSettings(backgroundScope, Dispatchers.Unconfined, settings)

  // region booleanSetting

  @Test
  fun test_booleanSetting_returnsDefaultWhenNotSet() = runTest {
    assertThat(createSettings().enabled.get()).isEqualTo(false)
  }

  @Test
  fun test_booleanSetting_getAndSet() = runTest {
    val settings = createSettings()
    settings.enabled.set(true)
    assertThat(settings.enabled.get()).isEqualTo(true)
  }

  @Test
  fun test_booleanSetting_observe() = runTest {
    val settings = createSettings()
    settings.enabled.observe().test {
      assertThat(awaitItem()).isEqualTo(false)
      settings.enabled.set(true)
      assertThat(awaitItem()).isEqualTo(true)
    }
  }

  // endregion

  // region longSetting

  @Test
  fun test_longSetting_returnsDefaultWhenNotSet() = runTest {
    assertThat(createSettings().count.get()).isEqualTo(42L)
  }

  @Test
  fun test_longSetting_getAndSet() = runTest {
    val settings = createSettings()
    settings.count.set(100L)
    assertThat(settings.count.get()).isEqualTo(100L)
  }

  @Test
  fun test_longSetting_observe() = runTest {
    val settings = createSettings()
    settings.count.observe().test {
      assertThat(awaitItem()).isEqualTo(42L)
      settings.count.set(99L)
      assertThat(awaitItem()).isEqualTo(99L)
    }
  }

  // endregion

  // region floatSetting

  @Test
  fun test_floatSetting_returnsDefaultWhenNotSet() = runTest {
    assertThat(createSettings().speed.get()).isEqualTo(1.0f)
  }

  @Test
  fun test_floatSetting_getAndSet() = runTest {
    val settings = createSettings()
    settings.speed.set(2.5f)
    assertThat(settings.speed.get()).isEqualTo(2.5f)
  }

  @Test
  fun test_floatSetting_observe() = runTest {
    val settings = createSettings()
    settings.speed.observe().test {
      assertThat(awaitItem()).isEqualTo(1.0f)
      settings.speed.set(1.5f)
      assertThat(awaitItem()).isEqualTo(1.5f)
    }
  }

  // endregion

  // region durationSetting

  @Test
  fun test_durationSetting_returnsDefaultWhenNotSet() = runTest {
    assertThat(createSettings().timeout.get()).isEqualTo(30.seconds)
  }

  @Test
  fun test_durationSetting_getAndSet() = runTest {
    val settings = createSettings()
    settings.timeout.set(5.minutes)
    assertThat(settings.timeout.get()).isEqualTo(5.minutes)
  }

  @Test
  fun test_durationSetting_observe() = runTest {
    val settings = createSettings()
    settings.timeout.observe().test {
      assertThat(awaitItem()).isEqualTo(30.seconds)
      settings.timeout.set(2.minutes)
      assertThat(awaitItem()).isEqualTo(2.minutes)
    }
  }

  // endregion

  // region stringSetting

  @Test
  fun test_stringSetting_returnsDefaultWhenNotSet() = runTest {
    assertThat(createSettings().name.get()).isEqualTo("default")
  }

  @Test
  fun test_stringSetting_getAndSet() = runTest {
    val settings = createSettings()
    settings.name.set("campfire")
    assertThat(settings.name.get()).isEqualTo("campfire")
  }

  @Test
  fun test_stringSetting_observe() = runTest {
    val settings = createSettings()
    settings.name.observe().test {
      assertThat(awaitItem()).isEqualTo("default")
      settings.name.set("updated")
      assertThat(awaitItem()).isEqualTo("updated")
    }
  }

  // endregion

  // region stringSetting with initializer

  @Test
  fun test_stringSetting_initializer_generatesAndStoresValueOnFirstRead() = runTest {
    assertThat(createSettings().generatedId.get()).isEqualTo("generated-123")
    assertThat(settings.getStringOrNull("generatedId")).isEqualTo("generated-123")
  }

  @Test
  fun test_stringSetting_initializer_preservesSetValue() = runTest {
    val settings = createSettings()
    settings.generatedId.set("overridden")
    assertThat(settings.generatedId.get()).isEqualTo("overridden")
  }

  // endregion

  // region stringOrNullSetting

  @Test
  fun test_stringOrNullSetting_returnsNullWhenNotSet() = runTest {
    assertThat(createSettings().userId.get()).isNull()
  }

  @Test
  fun test_stringOrNullSetting_getAndSet() = runTest {
    val settings = createSettings()
    settings.userId.set("user-1")
    assertThat(settings.userId.get()).isEqualTo("user-1")
  }

  @Test
  fun test_stringOrNullSetting_setNullRemovesValue() = runTest {
    val settings = createSettings()
    settings.userId.set("user-1")
    settings.userId.set(null)
    assertThat(settings.userId.get()).isNull()
    assertThat(this@AppSettingsTest.settings.hasKey("userId")).isEqualTo(false)
  }

  @Test
  fun test_stringOrNullSetting_observe() = runTest {
    val settings = createSettings()
    settings.userId.observe().test {
      assertThat(awaitItem()).isNull()
      settings.userId.set("user-1")
      assertThat(awaitItem()).isEqualTo("user-1")
      settings.userId.set(null)
      assertThat(awaitItem()).isNull()
    }
  }

  // endregion

  // region localTimeSetting

  @Test
  fun test_localTimeSetting_returnsDefaultWhenNotSet() = runTest {
    assertThat(createSettings().alarmTime.get()).isEqualTo(LocalTime(22, 0))
  }

  @Test
  fun test_localTimeSetting_getAndSet() = runTest {
    val settings = createSettings()
    settings.alarmTime.set(LocalTime(8, 30))
    assertThat(settings.alarmTime.get()).isEqualTo(LocalTime(8, 30))
  }

  @Test
  fun test_localTimeSetting_observe() = runTest {
    val settings = createSettings()
    settings.alarmTime.observe().test {
      assertThat(awaitItem()).isEqualTo(LocalTime(22, 0))
      settings.alarmTime.set(LocalTime(6, 0))
      assertThat(awaitItem()).isEqualTo(LocalTime(6, 0))
    }
  }

  // endregion

  // region localDateTimeSetting

  @Test
  fun test_localDateTimeSetting_returnsDefaultWhenNotSet() = runTest {
    assertThat(createSettings().lastSync.get()).isEqualTo(DEFAULT_DATE_TIME)
  }

  @Test
  fun test_localDateTimeSetting_getAndSet() = runTest {
    val settings = createSettings()
    val newDateTime = LocalDateTime(2025, 6, 15, 12, 0)
    settings.lastSync.set(newDateTime)
    assertThat(settings.lastSync.get()).isEqualTo(newDateTime)
  }

  @Test
  fun test_localDateTimeSetting_observe() = runTest {
    val settings = createSettings()
    settings.lastSync.observe().test {
      assertThat(awaitItem()).isEqualTo(DEFAULT_DATE_TIME)
      val newDateTime = LocalDateTime(2025, 12, 25, 0, 0)
      settings.lastSync.set(newDateTime)
      assertThat(awaitItem()).isEqualTo(newDateTime)
    }
  }

  // endregion

  // region enumSetting

  @Test
  fun test_enumSetting_returnsDefaultWhenNotSet() = runTest {
    assertThat(createSettings().color.get()).isEqualTo(TestColor.Red)
  }

  @Test
  fun test_enumSetting_getAndSet() = runTest {
    val settings = createSettings()
    settings.color.set(TestColor.Blue)
    assertThat(settings.color.get()).isEqualTo(TestColor.Blue)
  }

  @Test
  fun test_enumSetting_observe() = runTest {
    val settings = createSettings()
    settings.color.observe().test {
      assertThat(awaitItem()).isEqualTo(TestColor.Red)
      settings.color.set(TestColor.Green)
      assertThat(awaitItem()).isEqualTo(TestColor.Green)
    }
  }

  // endregion

  // region customSetting

  @Test
  fun test_customSetting_returnsDefaultWhenNotSet() = runTest {
    assertThat(createSettings().tags.get()).isEqualTo(emptyList())
  }

  @Test
  fun test_customSetting_getAndSet() = runTest {
    val settings = createSettings()
    settings.tags.set(listOf("a", "b", "c"))
    assertThat(settings.tags.get()).isEqualTo(listOf("a", "b", "c"))
  }

  @Test
  fun test_customSetting_observe() = runTest {
    val settings = createSettings()
    settings.tags.observe().test {
      assertThat(awaitItem()).isEqualTo(emptyList())
      settings.tags.set(listOf("x", "y"))
      assertThat(awaitItem()).isEqualTo(listOf("x", "y"))
    }
  }

  // endregion

  // region ordering

  @Test
  fun test_set_queuesTheWriteOffTheCaller() = runTest {
    val queued = TestAppSettings(backgroundScope, StandardTestDispatcher(testScheduler), settings)
    queued.count.set(7L)
    assertThat(settings.getLong("count", 42L)).isEqualTo(42L)
  }

  @Test
  fun test_get_seesEarlierWrites() = runTest {
    val queued = TestAppSettings(backgroundScope, StandardTestDispatcher(testScheduler), settings)
    queued.count.set(7L)
    assertThat(queued.count.get()).isEqualTo(7L)
  }

  @Test
  fun test_update_appliesInOrderAfterEarlierWrites() = runTest {
    val queued = TestAppSettings(backgroundScope, StandardTestDispatcher(testScheduler), settings)
    queued.count.set(1L)
    queued.count.update { it + 10 }
    queued.count.update { it * 2 }
    assertThat(queued.count.get()).isEqualTo(22L)
  }

  // endregion

  companion object {
    private val DEFAULT_DATE_TIME = LocalDateTime(2024, 1, 1, 0, 0)
  }
}

private class TestAppSettings(
  override val scope: CoroutineScope,
  override val dispatcher: CoroutineDispatcher,
  override val settings: ObservableSettings,
) : AppSettings() {
  val enabled = booleanSetting("enabled")
  val count = longSetting("count", defaultValue = 42L)
  val speed = floatSetting("speed", defaultValue = 1.0f)
  val timeout = durationSetting("timeout", defaultValue = 30.seconds)
  val name = stringSetting("name", defaultValue = "default")
  val generatedId = stringSetting("generatedId") { "generated-123" }
  val userId = stringOrNullSetting("userId")
  val alarmTime = localTimeSetting("alarmTime", defaultValue = LocalTime(22, 0))
  val lastSync = localDateTimeSetting("lastSync", defaultValue = LocalDateTime(2024, 1, 1, 0, 0))
  val color = enumSetting("color", TestColor.Companion)
  val tags = customSetting(
    key = "tags",
    defaultValue = emptyList(),
    getter = { it.split(",") },
    setter = { it.joinToString(",") },
  )
}

private enum class TestColor(override val storageKey: String) : EnumSetting {
  Red("red"),
  Green("green"),
  Blue("blue"),
  ;

  companion object : EnumSettingProvider<TestColor> {
    override fun fromStorageKey(key: String?): TestColor {
      return entries.find { it.storageKey == key } ?: Red
    }
  }
}
