// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import app.campfire.core.permission.LocalNetworkPermissionController
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.DevSettings
import app.campfire.settings.api.HomeNetworkSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import dev.jordond.connectivity.Connectivity
import io.ktor.http.Url
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = ServerReachability::class)
@ContributesBinding(AppScope::class, boundType = ReachabilityGate::class)
@Inject
class DefaultServerReachability(
  private val connectivity: Connectivity,
  private val networkMonitor: NetworkMonitor,
  private val homeNetworks: HomeNetworkLearner,
  private val homeNetworkSettings: HomeNetworkSettings,
  private val devSettings: DevSettings,
  private val localNetworkPermission: LocalNetworkPermissionController,
  private val fatherTime: FatherTime,
) : ServerReachability, ReachabilityGate, Cork {

  override val tag: String = "ServerReachability"
  override val enabled: Boolean = true

  private val belief = MutableStateFlow(Belief())

  private val _status = MutableStateFlow(Reachability.Unknown)
  override val status: StateFlow<Reachability> = _status.asStateFlow()

  /** The network an away-from-home probe was already spent on, per server origin. */
  private val probedNetworks = MutableStateFlow<Map<String, Any?>>(emptyMap())

  override fun reportReachable(serverUrl: String) {
    reachable(serverOrigin(serverUrl))
  }

  override fun observeInRange(serverUrl: String): Flow<Boolean> {
    val origin = serverOrigin(serverUrl)
    val locality = ServerLocality.of(serverUrl)
    return combine(
      networkMonitor.snapshot,
      homeNetworkSettings.observePauseAwayFromHome(),
      homeNetworkSettings.observeLearnedHomeNetworks(),
      localNetworkPermission.observePermissionMissing(),
    ) { snapshot, pause, _, blocked ->
      routeVerdict(
        locality = locality,
        network = snapshot,
        isSupported = networkMonitor.isSupported && pause,
        learned = homeNetworks.learned(origin),
        localNetworkBlocked = blocked,
      ) == RouteVerdict.Allow
    }.distinctUntilChanged()
  }

  override fun reachable(origin: String) {
    publish(Belief(origin = origin, status = Reachability.Reachable))
    if (ServerLocality.of(origin) == ServerLocality.Private) {
      homeNetworks.learn(origin, networkMonitor.snapshot.value)
    }
  }

  override fun unreachable(origin: String) {
    val previous = belief.value
    val outOfRange = verdict(origin) == RouteVerdict.ProbeOnce
    val status = if (outOfRange) Reachability.OutOfRange else Reachability.Unreachable
    publish(
      Belief(
        origin = origin,
        status = status,
        checkedAtMs = fatherTime.nowInEpochMillis(),
        network = currentNetwork(),
      ),
    )
    if (previous.status != status || previous.origin != origin) {
      if (outOfRange) {
        wbark { "Server not reachable from this network; pausing requests until the network changes" }
      } else {
        wbark { "Server unreachable; failing fast and probing every $PROBE_INTERVAL" }
      }
    }
  }

  override fun shouldFailFast(origin: String): Boolean {
    return when (verdict(origin)) {
      RouteVerdict.Skip -> {
        markOutOfRange(origin)
        true
      }
      RouteVerdict.ProbeOnce -> !claimProbe(origin)
      RouteVerdict.Allow -> devSettings.adaptToUnreachableServer && failFastWhileUnreachable(origin)
    }
  }

  private fun verdict(origin: String): RouteVerdict = routeVerdict(
    locality = ServerLocality.of(origin),
    network = networkMonitor.snapshot.value,
    // With the away-from-home setting off, only the permission gate still applies
    isSupported = networkMonitor.isSupported && homeNetworkSettings.pauseAwayFromHome,
    learned = homeNetworks.learned(origin),
    localNetworkBlocked = localNetworkPermission.isPermissionMissing(),
  )

  /**
   * On an unfamiliar network, lets exactly one request per network through to find out whether
   * it reaches the server (a second home, a replaced router). Once spent, requests stay off the
   * network until it changes — or the probe succeeds, which learns the network and turns the
   * verdict to Allow.
   */
  private fun claimProbe(origin: String): Boolean {
    val network = currentNetwork()
    while (true) {
      val probed = probedNetworks.value
      if (origin in probed && probed[origin] == network) {
        if (belief.value.status != Reachability.Reachable) markOutOfRange(origin)
        return false
      }
      if (probedNetworks.compareAndSet(probed, probed + (origin to network))) {
        ibark { "Unfamiliar network for a local server; probing once" }
        return true
      }
    }
  }

  private fun markOutOfRange(origin: String) {
    val current = belief.value
    if (current.origin == origin && current.status == Reachability.OutOfRange) return
    publish(
      Belief(
        origin = origin,
        status = Reachability.OutOfRange,
        checkedAtMs = fatherTime.nowInEpochMillis(),
        network = currentNetwork(),
      ),
    )
    ibark { "Server not reachable from this network; skipping requests" }
  }

  /**
   * Whether a request to [origin] should fail without touching the network because the server
   * is known to be unreachable. False whenever it isn't, the device changed networks since it
   * was, or a probe is due — in which case exactly one caller claims the probe and the rest keep
   * failing fast.
   */
  private fun failFastWhileUnreachable(origin: String): Boolean {
    while (true) {
      val current = belief.value
      val known = current.status == Reachability.Unreachable || current.status == Reachability.OutOfRange
      if (!known || current.origin != origin) return false

      if (currentNetwork() != current.network) {
        ibark { "Network changed since the server was unreachable; retrying" }
        if (belief.compareAndSet(current, Belief())) {
          _status.value = Reachability.Unknown
          return false
        }
        continue
      }

      val now = fatherTime.nowInEpochMillis()
      if (now - current.checkedAtMs < PROBE_INTERVAL.inWholeMilliseconds) return true

      // Probe due: claim it by restarting the window so concurrent callers keep failing fast
      if (belief.compareAndSet(current, current.copy(checkedAtMs = now))) return false
    }
  }

  private fun publish(new: Belief) {
    belief.value = new
    _status.value = new.status
  }

  /**
   * Identifies the current network: the monitor's snapshot where the platform can describe it
   * (so moving between two Wi-Fi networks counts as a change), else connectivity status.
   */
  private fun currentNetwork(): Any? = if (networkMonitor.isSupported) {
    networkMonitor.snapshot.value
  } else {
    connectivity.statusUpdates.replayCache.lastOrNull()
  }

  private data class Belief(
    val origin: String? = null,
    val status: Reachability = Reachability.Unknown,
    val checkedAtMs: Long = 0L,
    val network: Any? = null,
  )

  companion object {
    val PROBE_INTERVAL: Duration = 30.seconds
  }
}

/** Reduces a server URL to scheme://host:port so every endpoint on one server shares a belief. */
internal fun serverOrigin(url: Url): String = "${url.protocol.name}://${url.host}:${url.port}"

internal fun serverOrigin(url: String): String = serverOrigin(Url(url))
