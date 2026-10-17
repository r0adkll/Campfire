// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import app.campfire.core.time.FatherTime
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateSource
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private class FakeFatherTime(var nowMillis: Long = 0L) : FatherTime {
  override fun now(): LocalDateTime =
    Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(TimeZone.UTC)

  override fun today(): LocalDate = now().date
  override fun nowInEpochMillis(): Long = nowMillis
}

private class FakeAppLifecycleObserver(
  initial: AppLifecycleState = AppLifecycleState.Background,
) : AppLifecycleObserver {
  override val state = MutableStateFlow(initial)
}

private class CountingAppUpdateSource(
  override val isSupported: Boolean = true,
  override val checkInterval: Duration = 1.hours,
) : AppUpdateSource {
  val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  var available: AppUpdate? = null
  var checks = 0

  override fun changes(): Flow<Unit> = changes

  override suspend fun getAvailableUpdate(): AppUpdate? {
    checks++
    return available
  }

  override suspend fun installUpdate(): Flow<AppUpdateProgress> = emptyFlow()
}

class AppUpdateCheckerTest {

  private val fatherTime = FakeFatherTime()
  private val lifecycle = FakeAppLifecycleObserver()

  private fun TestScope.checker(source: AppUpdateSource) = AppUpdateChecker(
    appUpdateSource = source,
    appLifecycleObserver = lifecycle,
    fatherTime = fatherTime,
    scope = backgroundScope,
  )

  private fun TestScope.foreground() {
    lifecycle.state.value = AppLifecycleState.Foreground
    runCurrent()
  }

  private fun TestScope.background() {
    lifecycle.state.value = AppLifecycleState.Background
    runCurrent()
  }

  @Test
  fun `waits for the first foreground before checking`() = runTest {
    val source = CountingAppUpdateSource().apply { available = UPDATE }
    val checker = checker(source)
    runCurrent()
    assertThat(source.checks).isEqualTo(0)
    assertThat(checker.update.value).isNull()

    foreground()

    assertThat(source.checks).isEqualTo(1)
    assertThat(checker.update.value).isEqualTo(UPDATE)
  }

  @Test
  fun `foregrounds within the interval reuse the last check`() = runTest {
    val source = CountingAppUpdateSource(checkInterval = 15.minutes)
    checker(source)
    foreground()

    background()
    fatherTime.nowMillis += 14.minutes.inWholeMilliseconds
    foreground()
    assertThat(source.checks).isEqualTo(1)

    background()
    fatherTime.nowMillis += 1.minutes.inWholeMilliseconds
    foreground()
    assertThat(source.checks).isEqualTo(2)
  }

  @Test
  fun `a zero interval checks on every foreground`() = runTest {
    val source = CountingAppUpdateSource(checkInterval = Duration.ZERO)
    checker(source)

    repeat(3) {
      foreground()
      background()
    }

    assertThat(source.checks).isEqualTo(3)
  }

  @Test
  fun `a clock set backwards makes a check due`() = runTest {
    fatherTime.nowMillis = 1.hours.inWholeMilliseconds
    val source = CountingAppUpdateSource()
    checker(source)
    foreground()

    background()
    fatherTime.nowMillis = 0L
    foreground()

    assertThat(source.checks).isEqualTo(2)
  }

  @Test
  fun `refresh checks regardless of the interval`() = runTest {
    val source = CountingAppUpdateSource()
    val checker = checker(source)
    foreground()

    source.available = UPDATE
    checker.refresh()
    runCurrent()

    assertThat(source.checks).isEqualTo(2)
    assertThat(checker.update.value).isEqualTo(UPDATE)
  }

  @Test
  fun `source changes check regardless of the interval`() = runTest {
    val source = CountingAppUpdateSource()
    val checker = checker(source)
    foreground()

    source.available = UPDATE
    source.changes.tryEmit(Unit)
    runCurrent()

    assertThat(source.checks).isEqualTo(2)
    assertThat(checker.update.value).isEqualTo(UPDATE)
  }

  @Test
  fun `forced checks restart the interval`() = runTest {
    val source = CountingAppUpdateSource(checkInterval = 15.minutes)
    val checker = checker(source)
    foreground()

    fatherTime.nowMillis += 10.minutes.inWholeMilliseconds
    checker.refresh()
    runCurrent()

    background()
    fatherTime.nowMillis += 10.minutes.inWholeMilliseconds
    foreground()

    assertThat(source.checks).isEqualTo(2)
  }

  @Test
  fun `unsupported sources are never checked`() = runTest {
    val source = CountingAppUpdateSource(isSupported = false)
    val checker = checker(source)
    foreground()
    checker.refresh()
    runCurrent()

    assertThat(source.checks).isEqualTo(0)
    assertThat(checker.update.value).isNull()
  }

  private companion object {
    val UPDATE = AppUpdate(versionName = "1.3.0", versionCode = 1_003_099L)
  }
}
