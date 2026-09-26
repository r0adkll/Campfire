// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import app.campfire.core.permission.LocalNetworkPermissionController
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.DevSettings
import app.campfire.settings.api.LocalServerSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import dev.jordond.connectivity.Connectivity
import io.ktor.http.Url
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.tatarka.inject.annotations.Inject

/**
 * Tracks whether the signed-in user's server can be reached, per network connection.
 *
 * Any response proves the server reachable. A connection-level failure marks it unreachable for
 * the current connection: requests then fail without touching the network, except for one real
 * attempt after each step of [RETRY_SCHEDULE] (30s, 1m, 2m, 5m, then every 10m). Joining a
 * network — any network, including rejoining the same one — starts fresh. This covers both a
 * server that's down while you're home (it's found again within minutes) and being somewhere it
 * can't be reached (a handful of cheap attempts an hour), without having to tell them apart.
 *
 * Some networks can't reach a local server at all; [routeVerdict] decides those, and nothing is
 * attempted on them ([Reachability.OutOfRange]).
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = ServerReachability::class)
@ContributesBinding(AppScope::class, boundType = ReachabilityGate::class)
@Inject
class DefaultServerReachability(
  private val connectivity: Connectivity,
  private val networkMonitor: NetworkMonitor,
  private val localServerSettings: LocalServerSettings,
  private val devSettings: DevSettings,
  private val localNetworkPermission: LocalNetworkPermissionController,
  private val fatherTime: FatherTime,
) : ServerReachability, ReachabilityGate, Cork {

  override val tag: String = "ServerReachability"
  override val enabled: Boolean = true

  private val belief = MutableStateFlow(Belief())

  private val _status = MutableStateFlow(Reachability.Unknown)
  override val status: StateFlow<Reachability> = _status.asStateFlow()

  override fun reportReachable(serverUrl: String) {
    reachable(serverOrigin(serverUrl))
  }

  override fun observeInRange(serverUrl: String): Flow<Boolean> {
    val locality = ServerLocality.of(serverUrl)
    return combine(
      networkMonitor.snapshot,
      localServerSettings.observeAvoidMobileData(),
      localNetworkPermission.observePermissionMissing(),
    ) { snapshot, avoidMobileData, blocked ->
      routeVerdict(
        locality = locality,
        network = snapshot,
        isSupported = networkMonitor.isSupported,
        avoidMobileData = avoidMobileData,
        localNetworkBlocked = blocked,
      ) == RouteVerdict.Allow
    }.distinctUntilChanged()
  }

  override fun isLocalServer(serverUrl: String): Boolean =
    ServerLocality.of(serverUrl) != ServerLocality.Public

  override fun reachable(origin: String) {
    publish(Belief(origin = origin, status = Reachability.Reachable))
  }

  override fun unreachable(origin: String) {
    val network = currentNetwork()
    val previous = belief.value
    val failures = if (
      previous.origin == origin &&
      previous.status == Reachability.Unreachable &&
      previous.network == network
    ) {
      previous.failures + 1
    } else {
      1
    }
    publish(
      Belief(
        origin = origin,
        status = Reachability.Unreachable,
        checkedAtMs = fatherTime.nowInEpochMillis(),
        network = network,
        failures = failures,
      ),
    )
    wbark { "Server unreachable ($failures in a row); next attempt in ${retryDelay(failures)}" }
  }

  override fun shouldFailFast(origin: String): Boolean {
    return when (verdict(origin)) {
      RouteVerdict.Skip -> {
        markOutOfRange(origin)
        true
      }
      RouteVerdict.Allow -> devSettings.adaptToUnreachableServer && failFastWhileUnreachable(origin)
    }
  }

  private fun verdict(origin: String): RouteVerdict = routeVerdict(
    locality = ServerLocality.of(origin),
    network = networkMonitor.snapshot.value,
    isSupported = networkMonitor.isSupported,
    avoidMobileData = localServerSettings.avoidMobileData,
    localNetworkBlocked = localNetworkPermission.isPermissionMissing(),
  )

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
   * was unreachable on this connection. False when it wasn't, the device changed connections since,
   * or the next retry is due — in which case exactly one caller claims it and the rest keep
   * failing fast.
   */
  private fun failFastWhileUnreachable(origin: String): Boolean {
    while (true) {
      val current = belief.value
      if (current.status != Reachability.Unreachable || current.origin != origin) return false

      if (currentNetwork() != current.network) {
        ibark { "Network changed since the server was unreachable; retrying" }
        if (belief.compareAndSet(current, Belief())) {
          _status.value = Reachability.Unknown
          return false
        }
        continue
      }

      val now = fatherTime.nowInEpochMillis()
      if (now - current.checkedAtMs < retryDelay(current.failures).inWholeMilliseconds) return true

      // Retry due: claim it by restarting the window so concurrent callers keep failing fast
      if (belief.compareAndSet(current, current.copy(checkedAtMs = now))) return false
    }
  }

  private fun publish(new: Belief) {
    belief.value = new
    _status.value = new.status
  }

  /**
   * Identifies the current network connection: the monitor's snapshot where the platform can
   * describe it (its id changes on every join), else connectivity status.
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
    val failures: Int = 0,
  )

  companion object {
    /** How long to wait before the next real attempt after each consecutive failure. */
    val RETRY_SCHEDULE: List<Duration> = listOf(30.seconds, 1.minutes, 2.minutes, 5.minutes, 10.minutes)

    internal fun retryDelay(failures: Int): Duration =
      RETRY_SCHEDULE[(failures - 1).coerceIn(0, RETRY_SCHEDULE.lastIndex)]
  }
}

/** Reduces a server URL to scheme://host:port so every endpoint on one server shares a belief. */
internal fun serverOrigin(url: Url): String = "${url.protocol.name}://${url.host}:${url.port}"

internal fun serverOrigin(url: String): String = serverOrigin(Url(url))
