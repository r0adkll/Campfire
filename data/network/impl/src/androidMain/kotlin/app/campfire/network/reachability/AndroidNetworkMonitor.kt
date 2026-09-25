// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import android.app.Application
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import com.r0adkll.kimchi.annotations.ContributesBinding
import java.net.Inet4Address
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

/**
 * Tracks the default network through a single [ConnectivityManager] callback. It is also the
 * source for the app's [dev.jordond.connectivity.Connectivity] on Android (see
 * `createConnectivity`), so there is one system callback for both.
 *
 * Reads only [NetworkCapabilities] transports and [LinkProperties] addresses/routes, which need
 * nothing beyond ACCESS_NETWORK_STATE — never the Wi-Fi name, which requires location.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = NetworkMonitor::class, replaces = [UnknownNetworkMonitor::class])
@Inject
class AndroidNetworkMonitor(
  application: Application,
) : NetworkMonitor, Cork {

  override val tag: String = "NetworkMonitor"
  override val enabled: Boolean = true

  override val isSupported: Boolean = true

  private val manager = application.getSystemService(ConnectivityManager::class.java)

  private var capabilities: NetworkCapabilities? = null
  private var linkProperties: LinkProperties? = null

  private val _snapshot = MutableStateFlow(initialSnapshot())
  override val snapshot: StateFlow<NetworkSnapshot> = _snapshot.asStateFlow()

  private val callback = object : ConnectivityManager.NetworkCallback() {
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
      update(caps = networkCapabilities)
    }

    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
      update(link = linkProperties)
    }

    override fun onLost(network: Network) {
      synchronized(this@AndroidNetworkMonitor) {
        capabilities = null
        linkProperties = null
        _snapshot.value = NetworkSnapshot.Disconnected
      }
    }
  }

  init {
    runCatching { manager?.registerDefaultNetworkCallback(callback) }
      .onFailure { ebark(throwable = it) { "Unable to monitor the default network" } }
  }

  private fun initialSnapshot(): NetworkSnapshot {
    val network = manager?.activeNetwork ?: return NetworkSnapshot.Disconnected
    capabilities = manager.getNetworkCapabilities(network)
    linkProperties = manager.getLinkProperties(network)
    return snapshotOf(capabilities, linkProperties)
  }

  private fun update(
    caps: NetworkCapabilities? = null,
    link: LinkProperties? = null,
  ) = synchronized(this) {
    if (caps != null) capabilities = caps
    if (link != null) linkProperties = link
    val next = snapshotOf(capabilities, linkProperties)
    if (next != _snapshot.value) {
      dbark { "Default network: ${next.transports} fingerprint=${next.fingerprint} domain=${next.domain}" }
    }
    _snapshot.value = next
  }

  private fun snapshotOf(caps: NetworkCapabilities?, link: LinkProperties?): NetworkSnapshot {
    if (caps == null) return NetworkSnapshot.Disconnected
    val transports = buildSet {
      if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add(NetworkTransport.Wifi)
      if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add(NetworkTransport.Cellular)
      if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add(NetworkTransport.Ethernet)
      if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add(NetworkTransport.Vpn)
    }
    val isWifi = NetworkTransport.Wifi in transports
    val isCellular = NetworkTransport.Cellular in transports
    // Mirrors the connectivity library's rule so metered-interval sync behaves as before
    val metered = !isWifi || isCellular || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

    val isLocal = isWifi || NetworkTransport.Ethernet in transports
    return NetworkSnapshot(
      connected = true,
      metered = metered,
      transports = transports,
      fingerprint = if (isLocal) link?.fingerprint() else null,
      domain = if (isLocal) link?.domains?.takeIf { it.isNotBlank() } else null,
    )
  }
}

/** The device's IPv4 subnet and default gateway on this link, or null without an IPv4 address. */
internal fun LinkProperties.fingerprint(): NetworkFingerprint? {
  val address = linkAddresses.firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
    ?: return null
  val subnet = ipv4Subnet(address.address.address, address.prefixLength) ?: return null
  val gateway = routes
    .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
    ?.gateway
    ?.hostAddress
  return NetworkFingerprint(subnet = subnet, gateway = gateway)
}
