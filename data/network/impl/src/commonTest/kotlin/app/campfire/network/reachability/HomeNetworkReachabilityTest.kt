// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.settings.test.FakeHomeNetworkSettings
import app.cash.turbine.test
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
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
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class HomeNetworkReachabilityTest {

  private val time = FakeFatherTime()
  private val monitor = FakeNetworkMonitor(initial = wifi())
  private val settings = FakeHomeNetworkSettings()
  private val store = HomeNetworkStore(settings, monitor, time)
  private val reachability = reachability(
    monitor = monitor,
    homeNetworkSettings = settings,
    time = time,
  )

  private val localUrl = "http://192.168.1.10:13378"
  private val local = serverOrigin(localUrl)

  @Test
  fun `reaching a local server learns the network`() {
    reachability.reachable(local)

    assertThat(settings.learnedHomeNetworks).hasSize(1)
    assertThat(settings.learnedHomeNetworks.single().subnet).isEqualTo("192.168.1.0/24")
  }

  @Test
  fun `a public server learns nothing`() {
    reachability.reachable(serverOrigin("https://abs.example.com"))

    assertThat(settings.learnedHomeNetworks).isEmpty()
  }

  @Test
  fun `nothing is learned over a VPN`() {
    monitor.snapshot.value = wifi(vpn = true)

    reachability.reachable(local)

    assertThat(settings.learnedHomeNetworks).isEmpty()
  }

  @Test
  fun `cellular skips a local server without touching the network`() = runTest {
    reachability.reachable(local)
    monitor.snapshot.value = cellular()
    var calls = 0
    val client = client { calls++ }

    assertFailure { client.get("$localUrl/api/me") }.isInstanceOf<ServerUnreachableException>()
    assertThat(calls).isEqualTo(0)
    assertThat(reachability.status.value).isEqualTo(Reachability.OutOfRange)
  }

  @Test
  fun `an unfamiliar network gets one probe, then nothing`() = runTest {
    reachability.reachable(local)
    monitor.snapshot.value = wifi(subnet = "10.0.0.0/24", gateway = "10.0.0.1")
    var calls = 0
    val client = client {
      calls++
      throw ConnectTimeoutException("connect timed out")
    }

    assertFailure { client.get("$localUrl/api/me") }.isInstanceOf<ConnectTimeoutException>()
    assertThat(reachability.status.value).isEqualTo(Reachability.OutOfRange)

    // Well past the probe interval: an out-of-range network still gets no second attempt
    time.nowMillis += 1.hours.inWholeMilliseconds
    assertFailure { client.get("$localUrl/api/libraries") }.isInstanceOf<ServerUnreachableException>()
    assertThat(calls).isEqualTo(1)
  }

  @Test
  fun `a successful probe learns a second home`() = runTest {
    reachability.reachable(local)
    monitor.snapshot.value = wifi(subnet = "10.0.0.0/24", gateway = "10.0.0.1")

    client().get("$localUrl/api/me")

    assertThat(settings.learnedHomeNetworks).hasSize(2)
    assertThat(reachability.shouldFailFast(local)).isFalse()
  }

  @Test
  fun `returning home lifts the restriction`() = runTest {
    reachability.reachable(local)
    monitor.snapshot.value = cellular()
    assertThat(reachability.shouldFailFast(local)).isTrue()

    monitor.snapshot.value = wifi()

    assertThat(reachability.shouldFailFast(local)).isFalse()
  }

  @Test
  fun `turning the setting off restores normal behavior`() {
    reachability.reachable(local)
    monitor.snapshot.value = cellular()

    settings.pauseAwayFromHome = false

    assertThat(reachability.shouldFailFast(local)).isFalse()
  }

  @Test
  fun `in range follows the network`() = runTest {
    reachability.reachable(local)

    reachability.observeInRange(localUrl).test {
      assertThat(awaitItem()).isTrue()

      monitor.snapshot.value = cellular()
      assertThat(awaitItem()).isFalse()

      monitor.snapshot.value = wifi()
      assertThat(awaitItem()).isTrue()
    }
  }

  @Test
  fun `a public server is always in range`() = runTest {
    monitor.snapshot.value = cellular()

    assertThat(reachability.observeInRange("https://abs.example.com").first()).isTrue()
  }

  // region HomeNetworkStore

  @Test
  fun `each server keeps its most recent networks`() {
    repeat(HomeNetworkStore.MAX_NETWORKS_PER_SERVER + 2) { index ->
      time.nowMillis += 1_000
      store.learn(local, wifi(subnet = "10.0.$index.0/24", gateway = "10.0.$index.1"))
    }

    val subnets = settings.learnedHomeNetworks.map { it.subnet }
    assertThat(subnets).hasSize(HomeNetworkStore.MAX_NETWORKS_PER_SERVER)
    assertThat(subnets.contains("10.0.0.0/24")).isFalse()
  }

  @Test
  fun `last seen only moves after its resolution`() {
    store.learn(local, wifi())
    val first = settings.learnedHomeNetworks.single().lastSeenAtMs

    time.nowMillis += 1_000
    store.learn(local, wifi())
    assertThat(settings.learnedHomeNetworks.single().lastSeenAtMs).isEqualTo(first)

    time.nowMillis += HomeNetworkStore.LAST_SEEN_RESOLUTION.inWholeMilliseconds
    store.learn(local, wifi())
    assertThat(settings.learnedHomeNetworks.single().lastSeenAtMs).isEqualTo(time.nowMillis)
  }

  @Test
  fun `networks can be renamed and forgotten`() = runTest {
    store.learn(local, wifi())
    store.learn(local, wifi(subnet = "10.0.0.0/24", gateway = "10.0.0.1"))
    val key = NetworkFingerprint("192.168.1.0/24", "192.168.1.1").key

    store.rename(localUrl, key, "  Home  ")
    val renamed = store.observe(localUrl).first().networks.single { it.key == key }
    assertThat(renamed.label).isEqualTo("Home")
    assertThat(renamed.isCurrent).isTrue()

    store.forget(localUrl, key)
    assertThat(store.observe(localUrl).first().networks.map { it.subnet }).containsExactly("10.0.0.0/24")

    store.forgetAll(localUrl)
    assertThat(store.observe(localUrl).first().networks).isEmpty()
  }

  @Test
  fun `home networks report whether the server is local`() = runTest {
    assertThat(store.observe(localUrl).first().isLocalServer).isTrue()
    assertThat(store.observe("https://abs.example.com").first().isLocalServer).isFalse()
  }

  // endregion

  private fun client(beforeRespond: () -> Unit = {}): HttpClient = HttpClient(
    MockEngine {
      beforeRespond()
      respondOk()
    },
  ) {
    install(serverReachabilityPlugin(reachability))
  }
}
