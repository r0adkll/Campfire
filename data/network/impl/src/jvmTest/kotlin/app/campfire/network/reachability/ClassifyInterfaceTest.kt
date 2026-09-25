// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ClassifyInterfaceTest {

  @Test
  fun `vpn interfaces are recognised by name`() {
    listOf("utun4", "tun0", "wg0", "tailscale0", "ppp0", "ipsec0").forEach {
      assertThat(classifyInterface(it, it), it).isEqualTo(NetworkTransport.Vpn)
    }
  }

  @Test
  fun `windows vpn adapters are recognised by display name`() {
    assertThat(classifyInterface("eth3", "WireGuard Tunnel")).isEqualTo(NetworkTransport.Vpn)
    assertThat(classifyInterface("eth4", "Tailscale Tunnel")).isEqualTo(NetworkTransport.Vpn)
  }

  @Test
  fun `wifi interfaces are recognised by name or display name`() {
    assertThat(classifyInterface("wlp3s0", "wlp3s0")).isEqualTo(NetworkTransport.Wifi)
    assertThat(classifyInterface("wlan0", null)).isEqualTo(NetworkTransport.Wifi)
    assertThat(classifyInterface("wireless_32768", "Intel(R) Wi-Fi 6 AX201 160MHz")).isEqualTo(NetworkTransport.Wifi)
  }

  @Test
  fun `anything else counts as ethernet`() {
    assertThat(classifyInterface("en0", "en0")).isEqualTo(NetworkTransport.Ethernet)
    assertThat(classifyInterface("enp0s31f6", "enp0s31f6")).isEqualTo(NetworkTransport.Ethernet)
  }
}
