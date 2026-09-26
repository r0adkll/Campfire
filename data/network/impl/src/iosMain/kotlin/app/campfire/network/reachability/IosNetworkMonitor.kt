// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import platform.Network.nw_endpoint_get_address
import platform.Network.nw_interface_get_name
import platform.Network.nw_interface_get_type
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_other
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_interface_type_wired
import platform.Network.nw_path_enumerate_gateways
import platform.Network.nw_path_enumerate_interfaces
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_t
import platform.darwin.DISPATCH_QUEUE_SERIAL_WITH_AUTORELEASE_POOL
import platform.darwin.dispatch_queue_create
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.posix.AF_INET
import platform.posix.sockaddr_in

/**
 * Tracks the device's current path with an `nw_path_monitor`, and also feeds the app's
 * connectivity status (see `ConnectivityModule`), so there is one monitor for both. Reads
 * interface types, gateways and interface addresses only — never the Wi-Fi name, which needs
 * location access and an entitlement.
 */
@OptIn(ExperimentalForeignApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = NetworkMonitor::class, replaces = [UnknownNetworkMonitor::class])
@Inject
class IosNetworkMonitor : NetworkMonitor, Cork {

  override val tag: String = "NetworkMonitor"
  override val enabled: Boolean = true

  override val isSupported: Boolean = true

  private val _snapshot = MutableStateFlow(NetworkSnapshot.Unknown)
  override val snapshot: StateFlow<NetworkSnapshot> = _snapshot.asStateFlow()

  private val monitor = nw_path_monitor_create()

  init {
    nw_path_monitor_set_update_handler(monitor) { path ->
      val next = runCatching { snapshotOf(path) }
        .getOrElse {
          ebark(throwable = it) { "Unable to read the current path" }
          NetworkSnapshot.Unknown
        }
        .withConnectionId(_snapshot.value)
      if (next != _snapshot.value) {
        dbark { "Current path: ${next.transports} fingerprint=${next.fingerprint}" }
      }
      _snapshot.value = next
    }
    nw_path_monitor_set_queue(
      monitor,
      dispatch_queue_create("app.campfire.network.monitor", DISPATCH_QUEUE_SERIAL_WITH_AUTORELEASE_POOL),
    )
    nw_path_monitor_start(monitor)
  }

  private fun snapshotOf(path: nw_path_t): NetworkSnapshot {
    if (nw_path_get_status(path) != nw_path_status_satisfied) return NetworkSnapshot.Disconnected

    val interfaces = mutableListOf<Pair<String, NetworkTransport?>>()
    nw_path_enumerate_interfaces(path) { iface ->
      val name = nw_interface_get_name(iface)?.toKString().orEmpty()
      val transport = when (nw_interface_get_type(iface)) {
        nw_interface_type_wifi -> NetworkTransport.Wifi
        nw_interface_type_cellular -> NetworkTransport.Cellular
        nw_interface_type_wired -> NetworkTransport.Ethernet
        // VPNs surface as "other" utun/ipsec interfaces
        nw_interface_type_other -> NetworkTransport.Vpn.takeIf { name.startsWith("utun") || name.startsWith("ipsec") }
        else -> null
      }
      interfaces += name to transport
      true
    }
    val transports = interfaces.mapNotNullTo(mutableSetOf()) { it.second }

    // The first Wi-Fi/Ethernet interface is the one the path uses for local traffic
    val localName = interfaces.firstOrNull {
      it.second == NetworkTransport.Wifi || it.second == NetworkTransport.Ethernet
    }?.first
    val fingerprint = localName?.let { name ->
      ipv4SubnetOf(name)?.let { subnet -> NetworkFingerprint(subnet = subnet, gateway = ipv4Gateway(path)) }
    }

    val isWifi = NetworkTransport.Wifi in transports
    return NetworkSnapshot(
      connected = true,
      // Mirrors the connectivity library's Apple rule
      metered = !isWifi && (nw_path_is_expensive(path) || nw_path_is_constrained(path)),
      transports = transports,
      fingerprint = fingerprint,
      domain = null,
    )
  }

  private fun ipv4Gateway(path: nw_path_t): String? {
    var gateway: String? = null
    nw_path_enumerate_gateways(path) { endpoint ->
      val address = nw_endpoint_get_address(endpoint)
      if (address != null && address.pointed.sa_family.toInt() == AF_INET) {
        gateway = address.reinterpret<sockaddr_in>().pointed.sin_addr.s_addr.toIpv4String()
        false
      } else {
        true
      }
    }
    return gateway
  }

  /** The IPv4 subnet of interface [name], e.g. `192.168.1.0/24`, via `getifaddrs`. */
  private fun ipv4SubnetOf(name: String): String? = memScoped {
    val head = alloc<kotlinx.cinterop.CPointerVar<ifaddrs>>()
    if (getifaddrs(head.ptr) != 0) return null
    try {
      var cursor = head.value
      while (cursor != null) {
        val entry = cursor.pointed
        val address = entry.ifa_addr
        val netmask = entry.ifa_netmask
        if (entry.ifa_name?.toKString() == name &&
          address != null && netmask != null &&
          address.pointed.sa_family.toInt() == AF_INET
        ) {
          val ip = address.reinterpret<sockaddr_in>().pointed.sin_addr.s_addr
          val mask = netmask.reinterpret<sockaddr_in>().pointed.sin_addr.s_addr
          return ipv4Subnet(ip.toNetworkBytes(), mask.toNetworkBytes().prefixLength())
        }
        cursor = entry.ifa_next
      }
      null
    } finally {
      freeifaddrs(head.value)
    }
  }
}

/** `in_addr.s_addr` is network byte order stored in a host (little-endian) integer. */
private fun UInt.toNetworkBytes(): ByteArray = ByteArray(4) { index -> ((this shr (index * 8)) and 0xFFu).toByte() }

private fun UInt.toIpv4String(): String = toNetworkBytes().joinToString(".") { (it.toInt() and 0xFF).toString() }

private fun ByteArray.prefixLength(): Int = sumOf { byte -> (byte.toInt() and 0xFF).countOneBits() }
