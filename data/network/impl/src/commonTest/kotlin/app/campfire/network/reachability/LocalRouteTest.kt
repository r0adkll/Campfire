// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class LocalRouteTest {

  private val home = NetworkFingerprint("192.168.1.0/24", "192.168.1.1")

  // region ServerLocality

  @Test
  fun `private IPv4 ranges are local`() {
    listOf("10.0.0.5", "172.16.4.2", "172.31.255.1", "192.168.1.10", "169.254.3.3").forEach {
      assertThat(ServerLocality.ofHost(it), it).isEqualTo(ServerLocality.Private)
    }
  }

  @Test
  fun `addresses just outside the private ranges are public`() {
    listOf("172.15.0.1", "172.32.0.1", "192.169.1.1", "8.8.8.8", "127.0.0.1").forEach {
      assertThat(ServerLocality.ofHost(it), it).isEqualTo(ServerLocality.Public)
    }
  }

  @Test
  fun `local hostnames are local`() {
    listOf("abs.local", "abs.lan", "abs.home.arpa", "nas.internal", "ABS.LAN.").forEach {
      assertThat(ServerLocality.ofHost(it), it).isEqualTo(ServerLocality.Private)
    }
  }

  @Test
  fun `tailscale addresses and names need the VPN`() {
    assertThat(ServerLocality.ofHost("100.101.5.6")).isEqualTo(ServerLocality.VpnOnly)
    assertThat(ServerLocality.ofHost("abs.tail1234.ts.net")).isEqualTo(ServerLocality.VpnOnly)
    assertThat(ServerLocality.ofHost("100.128.0.1")).isEqualTo(ServerLocality.Public)
  }

  @Test
  fun `private IPv6 ranges are local`() {
    assertThat(ServerLocality.ofHost("[fd12:3456::1]")).isEqualTo(ServerLocality.Private)
    assertThat(ServerLocality.ofHost("fe80::1")).isEqualTo(ServerLocality.Private)
    assertThat(ServerLocality.ofHost("2001:db8::1")).isEqualTo(ServerLocality.Public)
  }

  @Test
  fun `server urls are judged by host`() {
    assertThat(ServerLocality.of("http://192.168.1.10:13378")).isEqualTo(ServerLocality.Private)
    assertThat(ServerLocality.of("https://abs.example.com")).isEqualTo(ServerLocality.Public)
  }

  // endregion

  // region routeVerdict

  @Test
  fun `public servers are always allowed`() {
    assertThat(verdict(ServerLocality.Public, cellular())).isEqualTo(RouteVerdict.Allow)
  }

  @Test
  fun `unsupported platforms are always allowed`() {
    assertThat(
      routeVerdict(ServerLocality.Private, cellular(), isSupported = false, learned = setOf(home)),
    ).isEqualTo(RouteVerdict.Allow)
  }

  @Test
  fun `a local server is skipped on cellular`() {
    assertThat(verdict(ServerLocality.Private, cellular())).isEqualTo(RouteVerdict.Skip)
  }

  @Test
  fun `a VPN reaches a local server from anywhere`() {
    assertThat(verdict(ServerLocality.Private, cellular(vpn = true))).isEqualTo(RouteVerdict.Allow)
    assertThat(verdict(ServerLocality.VpnOnly, wifi(subnet = "10.9.0.0/24", vpn = true))).isEqualTo(RouteVerdict.Allow)
  }

  @Test
  fun `a VPN-only server is skipped without the VPN`() {
    assertThat(verdict(ServerLocality.VpnOnly, wifi())).isEqualTo(RouteVerdict.Skip)
  }

  @Test
  fun `a learned network is allowed`() {
    assertThat(verdict(ServerLocality.Private, wifi())).isEqualTo(RouteVerdict.Allow)
  }

  @Test
  fun `an unfamiliar network gets one probe`() {
    assertThat(verdict(ServerLocality.Private, wifi(subnet = "10.0.0.0/24", gateway = "10.0.0.1")))
      .isEqualTo(RouteVerdict.ProbeOnce)
  }

  @Test
  fun `nothing learned yet means learning, not blocking`() {
    assertThat(
      routeVerdict(ServerLocality.Private, wifi(subnet = "10.0.0.0/24"), isSupported = true, learned = emptySet()),
    ).isEqualTo(RouteVerdict.Allow)
  }

  @Test
  fun `a network without a fingerprint is allowed`() {
    val snapshot = wifi().copy(fingerprint = null)
    assertThat(verdict(ServerLocality.Private, snapshot)).isEqualTo(RouteVerdict.Allow)
  }

  @Test
  fun `a local server is skipped while the local network permission is missing`() {
    assertThat(
      routeVerdict(
        ServerLocality.Private,
        wifi(),
        isSupported = true,
        learned = setOf(home),
        localNetworkBlocked = true,
      ),
    ).isEqualTo(RouteVerdict.Skip)
  }

  @Test
  fun `the permission gate applies even where networks can't be described`() {
    assertThat(
      routeVerdict(
        ServerLocality.Private,
        wifi(),
        isSupported = false,
        learned = emptySet(),
        localNetworkBlocked = true,
      ),
    ).isEqualTo(RouteVerdict.Skip)
  }

  @Test
  fun `a VPN or a public server is unaffected by the permission`() {
    assertThat(
      routeVerdict(
        ServerLocality.Private,
        cellular(vpn = true),
        isSupported = true,
        learned = setOf(home),
        localNetworkBlocked = true,
      ),
    ).isEqualTo(RouteVerdict.Allow)
    assertThat(
      routeVerdict(
        ServerLocality.Public,
        wifi(),
        isSupported = true,
        learned = setOf(home),
        localNetworkBlocked = true,
      ),
    ).isEqualTo(RouteVerdict.Allow)
  }

  private fun verdict(locality: ServerLocality, network: NetworkSnapshot) =
    routeVerdict(locality, network, isSupported = true, learned = setOf(home))

  // endregion

  // region ipv4Subnet

  @Test
  fun `subnets are masked to their prefix`() {
    assertThat(ipv4Subnet(byteArrayOf(192.toByte(), 168.toByte(), 1, 57), 24)).isEqualTo("192.168.1.0/24")
    assertThat(ipv4Subnet(byteArrayOf(10, 20, 30, 40), 8)).isEqualTo("10.0.0.0/8")
    assertThat(ipv4Subnet(byteArrayOf(172.toByte(), 16, 5, 200.toByte()), 20)).isEqualTo("172.16.0.0/20")
    assertThat(ipv4Subnet(byteArrayOf(1, 2, 3, 4), 0)).isEqualTo("0.0.0.0/0")
  }

  @Test
  fun `invalid subnets are rejected`() {
    assertThat(ipv4Subnet(byteArrayOf(1, 2, 3), 24)).isNull()
    assertThat(ipv4Subnet(byteArrayOf(1, 2, 3, 4), 33)).isNull()
  }

  // endregion
}
