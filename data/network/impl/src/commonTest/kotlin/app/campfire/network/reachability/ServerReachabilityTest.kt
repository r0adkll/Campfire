// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.settings.test.FakeDevSettings
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import dev.jordond.connectivity.Connectivity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.request.get
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

class ServerReachabilityTest {

  private val time = FakeFatherTime()
  private val connectivity = FakeConnectivity(Connectivity.Status.Connected(metered = false))
  private val devSettings = FakeDevSettings()
  private val reachability = reachability(
    connectivity = connectivity,
    time = time,
    devSettings = devSettings,
  )

  private val server = "https://abs.example.com:443"

  @Test
  fun `unknown and reachable servers never fail fast`() {
    assertThat(reachability.shouldFailFast(server)).isFalse()

    reachability.reachable(server)

    assertThat(reachability.status.value).isEqualTo(Reachability.Reachable)
    assertThat(reachability.shouldFailFast(server)).isFalse()
  }

  @Test
  fun `an unreachable server fails fast until a probe is due`() {
    reachability.unreachable(server)
    assertThat(reachability.status.value).isEqualTo(Reachability.Unreachable)
    assertThat(reachability.shouldFailFast(server)).isTrue()

    time.nowMillis += (DefaultServerReachability.PROBE_INTERVAL - 1.seconds).inWholeMilliseconds
    assertThat(reachability.shouldFailFast(server)).isTrue()
  }

  @Test
  fun `a due probe lets exactly one request through`() {
    reachability.unreachable(server)
    time.nowMillis += DefaultServerReachability.PROBE_INTERVAL.inWholeMilliseconds

    assertThat(reachability.shouldFailFast(server)).isFalse()
    assertThat(reachability.shouldFailFast(server)).isTrue()
  }

  @Test
  fun `a network change clears the unreachable belief`() {
    reachability.unreachable(server)

    connectivity.update(Connectivity.Status.Connected(metered = true))

    assertThat(reachability.shouldFailFast(server)).isFalse()
    assertThat(reachability.status.value).isEqualTo(Reachability.Unknown)
  }

  @Test
  fun `another server is unaffected`() {
    reachability.unreachable(server)

    assertThat(reachability.shouldFailFast("https://other.example.com:443")).isFalse()
  }

  @Test
  fun `a reported socket handshake lifts fail fast`() {
    reachability.unreachable(server)

    reachability.reportReachable("https://abs.example.com")

    assertThat(reachability.shouldFailFast(server)).isFalse()
  }

  @Test
  fun `plugin marks connect failures and then fails fast without the network`() = runTest {
    var calls = 0
    val client = client {
      calls++
      throw ConnectTimeoutException("connect timed out")
    }

    assertFailure { client.get("https://abs.example.com/api/me") }
      .isInstanceOf<ConnectTimeoutException>()
    assertThat(reachability.status.value).isEqualTo(Reachability.Unreachable)

    assertFailure { client.get("https://abs.example.com/api/libraries") }
      .isInstanceOf<ServerUnreachableException>()
    assertThat(calls).isEqualTo(1)
  }

  @Test
  fun `plugin treats any response as reachable`() = runTest {
    reachability.unreachable(server)
    time.nowMillis += DefaultServerReachability.PROBE_INTERVAL.inWholeMilliseconds
    val client = client()

    client.get("https://abs.example.com/api/me")

    assertThat(reachability.status.value).isEqualTo(Reachability.Reachable)
  }

  @Test
  fun `plugin ignores failures that are not connect failures`() = runTest {
    val client = client { throw IllegalStateException("boom") }

    assertFailure { client.get("https://abs.example.com/api/me") }
    assertThat(reachability.status.value).isEqualTo(Reachability.Unknown)
  }

  @Test
  fun `the developer switch turns fail fast off`() {
    devSettings.adaptToUnreachableServer = false

    reachability.unreachable(server)

    assertThat(reachability.status.value).isEqualTo(Reachability.Unreachable)
    assertThat(reachability.shouldFailFast(server)).isFalse()
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
