// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.time.FatherTime
import app.campfire.settings.api.OfflineSession
import app.campfire.settings.test.TestConnectionSettings
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWorkOfflineTest {

  private val settings = TestConnectionSettings()
  private val monitor = FakeNetworkMonitor(initial = wifi(id = 1))

  private fun TestScope.workOffline(): DefaultWorkOffline {
    val time = object : FatherTime {
      override fun now(): LocalDateTime = error("unused")
      override fun today() = error("unused")
      override fun nowInEpochMillis(): Long = START_MILLIS + testScheduler.currentTime
    }
    return DefaultWorkOffline(settings, monitor, time, backgroundScope).also { runCurrent() }
  }

  private fun TestScope.endings(workOffline: WorkOffline): List<Unit> = mutableListOf<Unit>().also { list ->
    backgroundScope.launch { workOffline.sessionEnded.collect { list += it } }
    runCurrent()
  }

  @Test
  fun `until turned off lasts until going online`() = runTest {
    val workOffline = workOffline()

    workOffline.goOffline(OfflineChoice.UntilTurnedOff)
    runCurrent()
    assertThat(workOffline.isOffline()).isTrue()
    assertThat(workOffline.state.value.reason).isEqualTo(OfflineReason.UntilTurnedOff)

    workOffline.goOnline()
    runCurrent()
    assertThat(workOffline.isOffline()).isFalse()
    assertThat(workOffline.state.value.reason).isNull()
  }

  @Test
  fun `a timed session ends on its own`() = runTest {
    val workOffline = workOffline()
    val endings = endings(workOffline)

    workOffline.goOffline(OfflineChoice.OneHour)
    runCurrent()
    assertThat(workOffline.state.value.reason)
      .isEqualTo(OfflineReason.Until(START_MILLIS + 1.hours.inWholeMilliseconds))

    advanceTimeBy(1.hours - 1.seconds)
    assertThat(workOffline.isOffline()).isTrue()
    assertThat(endings).isEmpty()

    advanceTimeBy(2.seconds)
    assertThat(workOffline.isOffline()).isFalse()
    assertThat(settings.offlineSession).isEqualTo(OfflineSession.None)
    assertThat(endings).containsExactly(Unit)
  }

  @Test
  fun `an expired session from an earlier launch ends straight away`() = runTest {
    settings.offlineSession = OfflineSession.Until(START_MILLIS - 1)

    val workOffline = workOffline()

    assertThat(workOffline.isOffline()).isFalse()
    assertThat(settings.offlineSession).isEqualTo(OfflineSession.None)
  }

  @Test
  fun `until Wi-Fi started on mobile data ends on joining Wi-Fi`() = runTest {
    monitor.snapshot.value = cellular()
    val workOffline = workOffline()
    val endings = endings(workOffline)

    workOffline.goOffline(OfflineChoice.UntilWifi)
    runCurrent()
    assertThat(workOffline.state.value.reason).isEqualTo(OfflineReason.UntilWifi(reconnect = false))

    monitor.snapshot.value = wifi(id = 2)
    runCurrent()
    assertThat(workOffline.isOffline()).isFalse()
    assertThat(endings).containsExactly(Unit)
  }

  @Test
  fun `until Wi-Fi started on Wi-Fi takes leaving and joining again`() = runTest {
    val workOffline = workOffline()

    workOffline.goOffline(OfflineChoice.UntilWifi)
    runCurrent()
    assertThat(workOffline.state.value.reason).isEqualTo(OfflineReason.UntilWifi(reconnect = true))

    // The same connection reporting again doesn't end it
    monitor.snapshot.value = wifi(id = 1, gateway = "192.168.1.254")
    runCurrent()
    assertThat(workOffline.isOffline()).isTrue()

    monitor.snapshot.value = cellular()
    runCurrent()
    assertThat(workOffline.isOffline()).isTrue()

    monitor.snapshot.value = wifi(id = 2)
    runCurrent()
    assertThat(workOffline.isOffline()).isFalse()
  }

  @Test
  fun `a restored Wi-Fi reconnect session adopts the first Wi-Fi it sees`() = runTest {
    settings.offlineSession = OfflineSession.UntilWifi(startedOnWifi = true)
    monitor.snapshot.value = cellular()
    val workOffline = workOffline()
    assertThat(workOffline.isOffline()).isTrue()

    // Possibly the network it started on, before the app restarted
    monitor.snapshot.value = wifi(id = 7)
    runCurrent()
    assertThat(workOffline.isOffline()).isTrue()

    monitor.snapshot.value = wifi(id = 8)
    runCurrent()
    assertThat(workOffline.isOffline()).isFalse()
  }

  @Test
  fun `the mobile data rule keeps Campfire offline off Wi-Fi`() = runTest {
    settings.workOfflineOnMobileData = true
    val workOffline = workOffline()
    assertThat(workOffline.isOffline()).isFalse()

    monitor.snapshot.value = cellular()
    runCurrent()
    assertThat(workOffline.state.value.reason).isEqualTo(OfflineReason.OnMobileData)

    // A VPN over mobile data is still mobile data
    monitor.snapshot.value = cellular(vpn = true)
    runCurrent()
    assertThat(workOffline.isOffline()).isTrue()

    monitor.snapshot.value = wifi(id = 2)
    runCurrent()
    assertThat(workOffline.isOffline()).isFalse()
  }

  @Test
  fun `going online sets the mobile data rule aside until the next Wi-Fi`() = runTest {
    settings.workOfflineOnMobileData = true
    monitor.snapshot.value = cellular()
    val workOffline = workOffline()
    assertThat(workOffline.isOffline()).isTrue()

    workOffline.goOnline()
    runCurrent()
    assertThat(workOffline.isOffline()).isFalse()

    monitor.snapshot.value = wifi(id = 2)
    runCurrent()
    assertThat(settings.mobileDataOfflineSuspended).isFalse()

    monitor.snapshot.value = cellular()
    runCurrent()
    assertThat(workOffline.isOffline()).isTrue()
  }

  @Test
  fun `a session ending on mobile data stays offline under the rule without announcing it`() = runTest {
    settings.workOfflineOnMobileData = true
    monitor.snapshot.value = cellular()
    val workOffline = workOffline()
    val endings = endings(workOffline)

    workOffline.goOffline(OfflineChoice.OneHour)
    advanceTimeBy(1.hours + 1.seconds)

    assertThat(workOffline.state.value.reason).isEqualTo(OfflineReason.OnMobileData)
    assertThat(endings).isEmpty()
  }

  @Test
  fun `the mobile data rule needs a network monitor`() = runTest {
    settings.workOfflineOnMobileData = true
    val unsupported = FakeNetworkMonitor(supported = false, initial = cellular())
    val time = object : FatherTime {
      override fun now(): LocalDateTime = error("unused")
      override fun today() = error("unused")
      override fun nowInEpochMillis(): Long = START_MILLIS
    }
    val workOffline = DefaultWorkOffline(settings, unsupported, time, backgroundScope)

    assertThat(workOffline.canDetectWifi).isFalse()
    assertThat(workOffline.isOffline()).isFalse()
  }

  @Test
  fun `until tomorrow means the next 6 AM`() {
    val zone = TimeZone.of("America/New_York")
    fun at(text: String) = LocalDateTime.parse(text).toInstant(zone).toEpochMilliseconds()
    fun morning(text: String) = Instant.fromEpochMilliseconds(nextMorning(at(text), zone)).toLocalDateTime(zone)

    assertThat(morning("2026-10-16T21:30")).isEqualTo(LocalDateTime.parse("2026-10-17T06:00"))
    assertThat(morning("2026-10-17T01:15")).isEqualTo(LocalDateTime.parse("2026-10-17T06:00"))
    assertThat(morning("2026-10-17T06:00")).isEqualTo(LocalDateTime.parse("2026-10-18T06:00"))
    // Across the end of daylight saving time
    assertThat(morning("2026-10-31T23:00")).isEqualTo(LocalDateTime.parse("2026-11-01T06:00"))
  }

  private companion object {
    val START_MILLIS = LocalDateTime.parse("2026-10-16T12:00").toInstant(TimeZone.UTC).toEpochMilliseconds()
  }
}
