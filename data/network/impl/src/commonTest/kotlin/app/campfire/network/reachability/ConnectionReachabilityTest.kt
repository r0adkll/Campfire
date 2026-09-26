// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.settings.test.FakeLocalServerSettings
import app.cash.turbine.test
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.request.get
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ConnectionReachabilityTest {

  private val time = FakeFatherTime()
  private val monitor = FakeNetworkMonitor(initial = wifi(id = 1))
  private val settings = FakeLocalServerSettings()
  private val permission = FakeLocalNetworkPermission()
  private val reachability = reachability(
    monitor = monitor,
    localServerSettings = settings,
    localNetworkPermission = permission,
    time = time,
  )

  private val localUrl = "http://192.168.1.10:13378"
  private val local = serverOrigin(localUrl)

  @Test
  fun `retries back off from 30 seconds to 10 minutes`() {
    assertThat(DefaultServerReachability.retryDelay(1)).isEqualTo(30.seconds)
    assertThat(DefaultServerReachability.retryDelay(2)).isEqualTo(1.minutes)
    assertThat(DefaultServerReachability.retryDelay(3)).isEqualTo(2.minutes)
    assertThat(DefaultServerReachability.retryDelay(4)).isEqualTo(5.minutes)
    assertThat(DefaultServerReachability.retryDelay(5)).isEqualTo(10.minutes)
    assertThat(DefaultServerReachability.retryDelay(40)).isEqualTo(10.minutes)
  }

  @Test
  fun `each failed retry waits longer before the next`() = runTest {
    var calls = 0
    val client = client {
      calls++
      throw ConnectTimeoutException("connect timed out")
    }

    attempt(client) // 1st failure → next retry in 30s
    assertThat(calls).isEqualTo(1)

    advance(29.seconds)
    attempt(client)
    assertThat(calls).isEqualTo(1)

    advance(1.seconds)
    attempt(client) // 2nd failure → next retry in 1m
    assertThat(calls).isEqualTo(2)

    advance(59.seconds)
    attempt(client)
    assertThat(calls).isEqualTo(2)

    advance(1.seconds)
    attempt(client)
    assertThat(calls).isEqualTo(3)
  }

  @Test
  fun `a response ends the backoff`() = runTest {
    reachability.unreachable(local)
    reachability.unreachable(local)
    advance(DefaultServerReachability.retryDelay(2))

    client().get("$localUrl/api/me")

    assertThat(reachability.status.value).isEqualTo(Reachability.Reachable)
    assertThat(reachability.shouldFailFast(local)).isFalse()
  }

  @Test
  fun `rejoining a network starts fresh`() {
    reachability.unreachable(local)
    assertThat(reachability.shouldFailFast(local)).isTrue()

    // Same Wi-Fi, new connection
    monitor.snapshot.value = wifi(id = 2)

    assertThat(reachability.shouldFailFast(local)).isFalse()
    assertThat(reachability.status.value).isEqualTo(Reachability.Unknown)
  }

  @Test
  fun `the failure count restarts on a new connection`() {
    repeat(4) { reachability.unreachable(local) }
    monitor.snapshot.value = wifi(id = 2)
    assertThat(reachability.shouldFailFast(local)).isFalse()

    reachability.unreachable(local)
    advance(DefaultServerReachability.retryDelay(1))

    assertThat(reachability.shouldFailFast(local)).isFalse()
  }

  @Test
  fun `mobile data skips a local server without touching the network`() = runTest {
    monitor.snapshot.value = cellular()
    var calls = 0

    assertFailure { client { calls++ }.get("$localUrl/api/me") }.isInstanceOf<ServerUnreachableException>()
    assertThat(calls).isEqualTo(0)
    assertThat(reachability.status.value).isEqualTo(Reachability.OutOfRange)
  }

  @Test
  fun `turning the setting off allows mobile data`() {
    monitor.snapshot.value = cellular()
    settings.avoidMobileData = false

    assertThat(reachability.shouldFailFast(local)).isFalse()
  }

  @Test
  fun `a missing local network permission skips the home server`() = runTest {
    permission.missing.value = true
    var calls = 0

    assertFailure { client { calls++ }.get("$localUrl/api/me") }.isInstanceOf<ServerUnreachableException>()
    assertThat(calls).isEqualTo(0)
    assertThat(reachability.status.value).isEqualTo(Reachability.OutOfRange)
  }

  @Test
  fun `in range follows mobile data and the permission`() = runTest {
    reachability.observeInRange(localUrl).test {
      assertThat(awaitItem()).isTrue()

      monitor.snapshot.value = cellular()
      assertThat(awaitItem()).isFalse()

      monitor.snapshot.value = wifi(id = 3)
      assertThat(awaitItem()).isTrue()

      permission.missing.value = true
      assertThat(awaitItem()).isFalse()

      permission.missing.value = false
      assertThat(awaitItem()).isTrue()
    }
  }

  @Test
  fun `a public server is always in range`() = runTest {
    monitor.snapshot.value = cellular()

    assertThat(reachability.observeInRange("https://abs.example.com").first()).isTrue()
  }

  @Test
  fun `local servers are recognised by address`() {
    assertThat(reachability.isLocalServer(localUrl)).isTrue()
    assertThat(reachability.isLocalServer("https://abs.tail1234.ts.net")).isTrue()
    assertThat(reachability.isLocalServer("https://abs.example.com")).isFalse()
  }

  private suspend fun attempt(client: HttpClient) {
    runCatching { client.get("$localUrl/api/me") }
  }

  private fun advance(duration: Duration) {
    time.nowMillis += duration.inWholeMilliseconds
  }

  private fun client(beforeRespond: () -> Unit = {}): HttpClient = HttpClient(
    MockEngine {
      beforeRespond()
      respondOk()
    },
  ) {
    install(serverReachabilityPlugin(reachability))
  }
}
