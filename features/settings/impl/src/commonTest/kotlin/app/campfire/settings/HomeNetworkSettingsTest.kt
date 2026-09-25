// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.settings.api.LearnedHomeNetwork
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test

class HomeNetworkSettingsTest {

  private val network = LearnedHomeNetwork(
    serverOrigin = "http://192.168.1.10:13378",
    subnet = "192.168.1.0/24",
    gateway = "192.168.1.1",
    transport = LearnedHomeNetwork.Transport.Wifi,
    domain = "lan",
    label = "Home \"main\" | 2",
    firstLearnedAtMs = 1_000L,
    lastSeenAtMs = 2_000L,
  )

  @Test
  fun `learned networks round trip`() {
    val other = network.copy(
      subnet = "10.0.0.0/24",
      gateway = null,
      domain = null,
      label = null,
      transport = LearnedHomeNetwork.Transport.Ethernet,
    )

    val decoded = decodeLearnedHomeNetworks(encodeLearnedHomeNetworks(listOf(network, other)))

    assertThat(decoded).containsExactly(network, other)
  }

  @Test
  fun `a malformed store decodes to nothing`() {
    assertThat(decodeLearnedHomeNetworks("not json")).isEmpty()
    assertThat(decodeLearnedHomeNetworks("{}")).isEmpty()
  }

  @Test
  fun `entries missing required fields are dropped`() {
    assertThat(decodeLearnedHomeNetworks("""[{"server":"x","transport":"Wifi"}]""")).isEmpty()
  }
}
