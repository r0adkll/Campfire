// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
import com.r0adkll.kimchi.annotations.ContributesBinding
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * Describes the desktop's default network from its local interfaces, polled on a short interval.
 * It sends no traffic at all — this replaces the connectivity library's HTTP provider, which
 * decided "online" by requesting google.com / github.com / bing.com.
 *
 * Plain Java can't read the default gateway, so the fingerprint is the subnet alone; two networks
 * sharing a subnet look alike, which only means the familiar-network rules apply (today's
 * behavior), never that a reachable server is skipped.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = NetworkMonitor::class, replaces = [UnknownNetworkMonitor::class])
@Inject
class DesktopNetworkMonitor(
  @ForScope(AppScope::class) scope: CoroutineScope,
) : NetworkMonitor, Cork {

  override val tag: String = "NetworkMonitor"
  override val enabled: Boolean = true

  override val isSupported: Boolean = true

  private val _snapshot = MutableStateFlow(readSnapshot())
  override val snapshot: StateFlow<NetworkSnapshot> = _snapshot.asStateFlow()

  init {
    scope.launch(Dispatchers.IO) {
      while (isActive) {
        delay(POLL_INTERVAL)
        val next = readSnapshot()
        if (next != _snapshot.value) {
          dbark { "Default network: ${next.transports} fingerprint=${next.fingerprint}" }
        }
        _snapshot.value = next
      }
    }
  }

  private fun readSnapshot(): NetworkSnapshot = runCatching {
    val local = defaultRouteAddress() ?: return NetworkSnapshot.Disconnected
    val iface = NetworkInterface.getByInetAddress(local) ?: return NetworkSnapshot.Disconnected
    val vpnActive = NetworkInterface.networkInterfaces().toList().any { it.isUp && it.isVpn() }

    val transport = classifyInterface(iface.name, iface.displayName)
    val transports = buildSet {
      add(transport)
      if (vpnActive) add(NetworkTransport.Vpn)
    }
    val prefix = iface.interfaceAddresses.firstOrNull { it.address == local }?.networkPrefixLength?.toInt()
    val fingerprint = if (transport == NetworkTransport.Vpn || prefix == null) {
      null
    } else {
      ipv4Subnet(local.address, prefix)?.let { NetworkFingerprint(subnet = it, gateway = null) }
    }

    NetworkSnapshot(
      connected = true,
      // Desktop connections aren't treated as metered, matching the previous provider
      metered = false,
      transports = transports,
      fingerprint = fingerprint,
      domain = null,
    )
  }.getOrElse {
    ebark(throwable = it) { "Unable to read the default network" }
    NetworkSnapshot.Unknown
  }

  /**
   * The local IPv4 address the OS would route external traffic from, or null with no route.
   * Connecting a UDP socket only selects a route; no packet is sent. The target is TEST-NET-1
   * (RFC 5737), which is never a real host.
   */
  private fun defaultRouteAddress(): Inet4Address? = runCatching {
    DatagramSocket().use { socket ->
      socket.connect(InetAddress.getByAddress(ROUTE_PROBE_TARGET), ROUTE_PROBE_PORT)
      (socket.localAddress as? Inet4Address)?.takeUnless { it.isAnyLocalAddress || it.isLoopbackAddress }
    }
  }.getOrNull()

  private fun NetworkInterface.isVpn(): Boolean =
    classifyInterface(name, displayName) == NetworkTransport.Vpn &&
      inetAddresses.toList().any { it is Inet4Address }

  private companion object {
    val POLL_INTERVAL = 10.seconds
    val ROUTE_PROBE_TARGET = byteArrayOf(192.toByte(), 0, 2, 1)
    const val ROUTE_PROBE_PORT = 9
  }
}

private val VPN_PREFIXES = listOf("utun", "tun", "tap", "wg", "ppp", "ipsec", "tailscale", "zt")
private val WIFI_PREFIXES = listOf("wl", "wlan", "wifi")
private val WIFI_DISPLAY_HINTS = listOf("wi-fi", "wifi", "wireless", "wlan", "802.11")

/**
 * Best-effort interface kind from its OS name (`en0`, `wlp3s0`, `utun4`) and, on Windows, its
 * display name (`Intel(R) Wi-Fi 6 AX201`). Anything unrecognised counts as Ethernet — the only
 * consequence is how a learned network is labelled.
 */
internal fun classifyInterface(name: String, displayName: String?): NetworkTransport {
  val lowerName = name.lowercase()
  val lowerDisplay = displayName.orEmpty().lowercase()
  return when {
    VPN_PREFIXES.any { lowerName.startsWith(it) } ||
      "vpn" in lowerDisplay || "wireguard" in lowerDisplay || "tailscale" in lowerDisplay -> NetworkTransport.Vpn
    WIFI_PREFIXES.any { lowerName.startsWith(it) } ||
      WIFI_DISPLAY_HINTS.any { it in lowerDisplay } -> NetworkTransport.Wifi
    else -> NetworkTransport.Ethernet
  }
}
