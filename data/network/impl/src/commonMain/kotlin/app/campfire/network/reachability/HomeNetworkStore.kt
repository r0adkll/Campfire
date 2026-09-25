// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.HomeNetworkSettings
import app.campfire.settings.api.LearnedHomeNetwork
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.tatarka.inject.annotations.Inject

/**
 * Remembers the networks each local server has been reached from, in [HomeNetworkSettings]. Keyed by
 * server origin so switching accounts or servers never mixes their homes.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = HomeNetworks::class)
@ContributesBinding(AppScope::class, boundType = HomeNetworkLearner::class)
@Inject
class HomeNetworkStore(
  private val homeNetworkSettings: HomeNetworkSettings,
  private val networkMonitor: NetworkMonitor,
  private val fatherTime: FatherTime,
) : HomeNetworks, HomeNetworkLearner {

  // Read on every request, so parse the stored list once; all writes go through [write]
  @Volatile
  private var cache: List<LearnedHomeNetwork>? = null

  private fun all(): List<LearnedHomeNetwork> =
    cache ?: homeNetworkSettings.learnedHomeNetworks.also { cache = it }

  private fun write(networks: List<LearnedHomeNetwork>) {
    cache = networks
    homeNetworkSettings.learnedHomeNetworks = networks
  }

  override fun learned(origin: String): Set<NetworkFingerprint> =
    all()
      .filter { it.serverOrigin == origin }
      .mapTo(mutableSetOf()) { NetworkFingerprint(it.subnet, it.gateway) }

  /**
   * Records that [origin] was reached from [network]. Only a directly attached local network is
   * learned — over a VPN the local network says nothing about reaching home.
   */
  override fun learn(origin: String, network: NetworkSnapshot) {
    val fingerprint = network.fingerprint ?: return
    if (network.hasVpn) return
    val transport = when (network.localTransport) {
      NetworkTransport.Wifi -> LearnedHomeNetwork.Transport.Wifi
      NetworkTransport.Ethernet -> LearnedHomeNetwork.Transport.Ethernet
      else -> return
    }

    val now = fatherTime.nowInEpochMillis()
    val all = all()
    val existing = all.firstOrNull { it.serverOrigin == origin && it.key == fingerprint.key }
    if (existing != null) {
      // Every successful request lands here; only rewrite settings when last-seen moves meaningfully
      if (now - existing.lastSeenAtMs < LAST_SEEN_RESOLUTION.inWholeMilliseconds) return
      write(
        all.map {
          if (it === existing) it.copy(lastSeenAtMs = now, domain = network.domain ?: it.domain) else it
        },
      )
      return
    }

    val learned = LearnedHomeNetwork(
      serverOrigin = origin,
      subnet = fingerprint.subnet,
      gateway = fingerprint.gateway,
      transport = transport,
      domain = network.domain,
      label = null,
      firstLearnedAtMs = now,
      lastSeenAtMs = now,
    )
    val (mine, others) = all.partition { it.serverOrigin == origin }
    // Keep the most recently seen networks per server; a stale router config ages out
    val kept = (mine + learned).sortedByDescending { it.lastSeenAtMs }.take(MAX_NETWORKS_PER_SERVER)
    write(others + kept)
  }

  override fun observe(serverUrl: String): Flow<HomeNetworksState> {
    val origin = serverOrigin(serverUrl)
    val isLocal = ServerLocality.of(serverUrl) == ServerLocality.Private
    return combine(
      homeNetworkSettings.observeLearnedHomeNetworks(),
      networkMonitor.snapshot,
    ) { all, snapshot ->
      val current = snapshot.fingerprint?.key?.takeUnless { snapshot.hasVpn }
      HomeNetworksState(
        isLocalServer = isLocal,
        networks = all
          .filter { it.serverOrigin == origin }
          .sortedByDescending { it.lastSeenAtMs }
          .map { it.asHomeNetwork(isCurrent = it.key == current) },
      )
    }
  }

  override fun rename(serverUrl: String, key: String, label: String?) {
    val origin = serverOrigin(serverUrl)
    write(
      all().map {
        if (it.serverOrigin == origin && it.key == key) it.copy(label = label?.trim()?.ifEmpty { null }) else it
      },
    )
  }

  override fun forget(serverUrl: String, key: String) {
    val origin = serverOrigin(serverUrl)
    write(all().filterNot { it.serverOrigin == origin && it.key == key })
  }

  override fun forgetAll(serverUrl: String) {
    val origin = serverOrigin(serverUrl)
    write(all().filterNot { it.serverOrigin == origin })
  }

  private fun LearnedHomeNetwork.asHomeNetwork(isCurrent: Boolean) = HomeNetwork(
    key = key,
    subnet = subnet,
    gateway = gateway,
    transport = when (transport) {
      LearnedHomeNetwork.Transport.Wifi -> NetworkTransport.Wifi
      LearnedHomeNetwork.Transport.Ethernet -> NetworkTransport.Ethernet
    },
    domain = domain,
    label = label,
    lastSeenAtMs = lastSeenAtMs,
    isCurrent = isCurrent,
  )

  internal companion object {
    const val MAX_NETWORKS_PER_SERVER = 8
    val LAST_SEEN_RESOLUTION = 1.hours
  }
}
