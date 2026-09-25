// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import app.campfire.core.time.FatherTime
import com.r0adkll.kimchi.annotations.ContributesBinding
import dev.jordond.connectivity.Connectivity
import io.ktor.http.Url
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = ServerReachability::class)
@ContributesBinding(AppScope::class, boundType = ReachabilityGate::class)
@Inject
class DefaultServerReachability(
  private val connectivity: Connectivity,
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

  override fun reachable(origin: String) {
    publish(Belief(origin = origin, status = Reachability.Reachable))
  }

  override fun unreachable(origin: String) {
    val previous = belief.value
    publish(
      Belief(
        origin = origin,
        status = Reachability.Unreachable,
        checkedAtMs = fatherTime.nowInEpochMillis(),
        network = currentNetwork(),
      ),
    )
    if (previous.status != Reachability.Unreachable || previous.origin != origin) {
      wbark { "Server unreachable; failing fast and probing every $PROBE_INTERVAL" }
    }
  }

  override fun shouldFailFast(origin: String): Boolean {
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
      if (now - current.checkedAtMs < PROBE_INTERVAL.inWholeMilliseconds) return true

      // Probe due: claim it by restarting the window so concurrent callers keep failing fast
      if (belief.compareAndSet(current, current.copy(checkedAtMs = now))) return false
    }
  }

  private fun publish(new: Belief) {
    belief.value = new
    _status.value = new.status
  }

  private fun currentNetwork(): Connectivity.Status? = connectivity.statusUpdates.replayCache.lastOrNull()

  private data class Belief(
    val origin: String? = null,
    val status: Reachability = Reachability.Unknown,
    val checkedAtMs: Long = 0L,
    val network: Connectivity.Status? = null,
  )

  companion object {
    val PROBE_INTERVAL: Duration = 30.seconds
  }
}

/** Reduces a server URL to scheme://host:port so every endpoint on one server shares a belief. */
internal fun serverOrigin(url: Url): String = "${url.protocol.name}://${url.host}:${url.port}"

internal fun serverOrigin(url: String): String = serverOrigin(Url(url))
