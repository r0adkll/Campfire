package app.campfire.settings

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.ObservableSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class AppSettingsTest {

  private val settingsScope = kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined + Job())

  private fun createSettings(
    initial: Map<String, Any> = emptyMap(),
  ): TestAppSettings {
    val map = initial.toMutableMap()
    return TestAppSettings(settingsScope, MapSettings(map))
  }

  @AfterTest
  fun tearDown() {
    settingsScope.cancel()
  }

  // region booleanSetting

  @Test
  fun test_booleanSetting_returnsDefaultWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.enabled).isEqualTo(false)
  }

  @Test
  fun test_booleanSetting_getAndSet() {
    val settings = createSettings()
    settings.enabled = true
    assertThat(settings.enabled).isEqualTo(true)
  }

  @Test
  fun test_booleanSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeEnabled().test {
      assertThat(awaitItem()).isEqualTo(false)
      settings.enabled = true
      assertThat(awaitItem()).isEqualTo(true)
    }
  }

  // endregion

  // region longSetting

  @Test
  fun test_longSetting_returnsDefaultWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.count).isEqualTo(42L)
  }

  @Test
  fun test_longSetting_getAndSet() {
    val settings = createSettings()
    settings.count = 100L
    assertThat(settings.count).isEqualTo(100L)
  }

  @Test
  fun test_longSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeCount().test {
      assertThat(awaitItem()).isEqualTo(42L)
      settings.count = 99L
      assertThat(awaitItem()).isEqualTo(99L)
    }
  }

  // endregion

  // region floatSetting

  @Test
  fun test_floatSetting_returnsDefaultWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.speed).isEqualTo(1.0f)
  }

  @Test
  fun test_floatSetting_getAndSet() {
    val settings = createSettings()
    settings.speed = 2.5f
    assertThat(settings.speed).isEqualTo(2.5f)
  }

  @Test
  fun test_floatSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeSpeed().test {
      assertThat(awaitItem()).isEqualTo(1.0f)
      settings.speed = 1.5f
      assertThat(awaitItem()).isEqualTo(1.5f)
    }
  }

  // endregion

  // region durationSetting

  @Test
  fun test_durationSetting_returnsDefaultWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.timeout).isEqualTo(30.seconds)
  }

  @Test
  fun test_durationSetting_getAndSet() {
    val settings = createSettings()
    settings.timeout = 5.minutes
    assertThat(settings.timeout).isEqualTo(5.minutes)
  }

  @Test
  fun test_durationSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeTimeout().test {
      assertThat(awaitItem()).isEqualTo(30.seconds)
      settings.timeout = 2.minutes
      assertThat(awaitItem()).isEqualTo(2.minutes)
    }
  }

  // endregion

  // region stringSetting

  @Test
  fun test_stringSetting_returnsDefaultWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.name).isEqualTo("default")
  }

  @Test
  fun test_stringSetting_getAndSet() {
    val settings = createSettings()
    settings.name = "campfire"
    assertThat(settings.name).isEqualTo("campfire")
  }

  @Test
  fun test_stringSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeName().test {
      assertThat(awaitItem()).isEqualTo("default")
      settings.name = "updated"
      assertThat(awaitItem()).isEqualTo("updated")
    }
  }

  // endregion

  // region stringSetting with initializer

  @Test
  fun test_stringSetting_initializer_generatesValueOnFirstAccess() {
    val settings = createSettings()
    assertThat(settings.generatedId).isEqualTo("generated-123")
  }

  @Test
  fun test_stringSetting_initializer_preservesSetValue() {
    val settings = createSettings()
    settings.generatedId = "overridden"
    assertThat(settings.generatedId).isEqualTo("overridden")
  }

  // endregion

  // region stringOrNullSetting

  @Test
  fun test_stringOrNullSetting_returnsNullWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.userId).isNull()
  }

  @Test
  fun test_stringOrNullSetting_getAndSet() {
    val settings = createSettings()
    settings.userId = "user-1"
    assertThat(settings.userId).isEqualTo("user-1")
  }

  @Test
  fun test_stringOrNullSetting_setNullRemovesValue() {
    val settings = createSettings()
    settings.userId = "user-1"
    settings.userId = null
    assertThat(settings.userId).isNull()
  }

  @Test
  fun test_stringOrNullSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeUserId().test {
      assertThat(awaitItem()).isNull()
      settings.userId = "user-1"
      assertThat(awaitItem()).isEqualTo("user-1")
      settings.userId = null
      assertThat(awaitItem()).isNull()
    }
  }

  // endregion

  // region localTimeSetting

  @Test
  fun test_localTimeSetting_returnsDefaultWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.alarmTime).isEqualTo(LocalTime(22, 0))
  }

  @Test
  fun test_localTimeSetting_getAndSet() {
    val settings = createSettings()
    val newTime = LocalTime(8, 30)
    settings.alarmTime = newTime
    assertThat(settings.alarmTime).isEqualTo(newTime)
  }

  @Test
  fun test_localTimeSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeAlarmTime().test {
      assertThat(awaitItem()).isEqualTo(LocalTime(22, 0))
      val newTime = LocalTime(6, 0)
      settings.alarmTime = newTime
      assertThat(awaitItem()).isEqualTo(newTime)
    }
  }

  // endregion

  // region localDateTimeSetting

  @Test
  fun test_localDateTimeSetting_returnsDefaultWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.lastSync).isEqualTo(DEFAULT_DATE_TIME)
  }

  @Test
  fun test_localDateTimeSetting_getAndSet() {
    val settings = createSettings()
    val newDateTime = LocalDateTime(2025, 6, 15, 12, 0)
    settings.lastSync = newDateTime
    assertThat(settings.lastSync).isEqualTo(newDateTime)
  }

  @Test
  fun test_localDateTimeSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeLastSync().test {
      assertThat(awaitItem()).isEqualTo(DEFAULT_DATE_TIME)
      val newDateTime = LocalDateTime(2025, 12, 25, 0, 0)
      settings.lastSync = newDateTime
      assertThat(awaitItem()).isEqualTo(newDateTime)
    }
  }

  // endregion

  // region enumSetting

  @Test
  fun test_enumSetting_returnsDefaultWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.color).isEqualTo(TestColor.Red)
  }

  @Test
  fun test_enumSetting_getAndSet() {
    val settings = createSettings()
    settings.color = TestColor.Blue
    assertThat(settings.color).isEqualTo(TestColor.Blue)
  }

  @Test
  fun test_enumSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeColor().test {
      assertThat(awaitItem()).isEqualTo(TestColor.Red)
      settings.color = TestColor.Green
      assertThat(awaitItem()).isEqualTo(TestColor.Green)
    }
  }

  // endregion

  // region customSetting

  @Test
  fun test_customSetting_returnsDefaultWhenNotSet() {
    val settings = createSettings()
    assertThat(settings.tags).isEqualTo(emptyList())
  }

  @Test
  fun test_customSetting_getAndSet() {
    val settings = createSettings()
    settings.tags = listOf("a", "b", "c")
    assertThat(settings.tags).isEqualTo(listOf("a", "b", "c"))
  }

  @Test
  fun test_customSetting_observe() = runTest {
    val settings = createSettings()
    settings.observeTags().test {
      assertThat(awaitItem()).isEqualTo(emptyList())
      settings.tags = listOf("x", "y")
      assertThat(awaitItem()).isEqualTo(listOf("x", "y"))
    }
  }

  // endregion

  companion object {
    private val DEFAULT_DATE_TIME = LocalDateTime(2024, 1, 1, 0, 0)
  }
}

private class TestAppSettings(
  override val scope: kotlinx.coroutines.CoroutineScope,
  override val settings: ObservableSettings,
) : AppSettings() {

  private val enabledProperty = booleanSetting("enabled")
  var enabled by enabledProperty
  fun observeEnabled(): StateFlow<Boolean> = enabledProperty.observe()

  private val countProperty = longSetting("count", defaultValue = 42L)
  var count by countProperty
  fun observeCount(): StateFlow<Long> = countProperty.observe()

  private val speedProperty = floatSetting("speed", defaultValue = 1.0f)
  var speed by speedProperty
  fun observeSpeed(): StateFlow<Float> = speedProperty.observe()

  private val timeoutProperty = durationSetting("timeout", defaultValue = 30.seconds)
  var timeout by timeoutProperty
  fun observeTimeout(): StateFlow<kotlin.time.Duration> = timeoutProperty.observe()

  private val nameProperty = stringSetting("name", defaultValue = "default")
  var name by nameProperty
  fun observeName(): StateFlow<String> = nameProperty.observe()

  private val generatedIdProperty = stringSetting("generatedId") { "generated-123" }
  var generatedId by generatedIdProperty

  private val userIdProperty = stringOrNullSetting("userId")
  var userId by userIdProperty
  fun observeUserId(): StateFlow<String?> = userIdProperty.observe()

  private val alarmTimeProperty = localTimeSetting("alarmTime", defaultValue = LocalTime(22, 0))
  var alarmTime by alarmTimeProperty
  fun observeAlarmTime(): StateFlow<LocalTime> = alarmTimeProperty.observe()

  private val lastSyncProperty = localDateTimeSetting("lastSync", defaultValue = LocalDateTime(2024, 1, 1, 0, 0))
  var lastSync by lastSyncProperty
  fun observeLastSync(): StateFlow<LocalDateTime> = lastSyncProperty.observe()

  private val colorProperty = enumSetting("color", TestColor.Companion)
  var color by colorProperty
  fun observeColor(): StateFlow<TestColor> = colorProperty.observe()

  private val tagsProperty = customSetting(
    key = "tags",
    defaultValue = emptyList(),
    getter = { it.split(",") },
    setter = { it.joinToString(",") },
  )
  var tags by tagsProperty
  fun observeTags(): StateFlow<List<String>> = tagsProperty.observe()
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
