// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Corked
import app.campfire.core.session.UserSession
import app.campfire.core.session.user
import app.campfire.network.reachability.Reachability
import app.campfire.network.reachability.ServerReachability
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import dev.jordond.connectivity.Connectivity
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * Uploads listening recorded while the server was out of reach as soon as it can be reached
 * again, instead of waiting for the next play or pause (the only other times sync runs).
 *
 * Reachability alone isn't enough to trigger on: it only changes when something makes a request,
 * and with the app in the background nothing does. So regaining the network, or moving back onto
 * a network that can reach a local server, triggers an attempt too. An attempt costs nothing while
 * the server still can't be reached (the request fails without touching the network) or when there
 * are no sessions to report. An open locally-owned session is re-sent whole on every sync, as the
 * periodic and pause syncs already do — the server upserts it by id, so a repeat is one idempotent
 * request per reconnect.
 */
@ContributesMultibinding(UserScope::class, boundType = Scoped::class)
@Inject
class ReconnectSessionSync(
  private val userSession: UserSession,
  private val serverReachability: ServerReachability,
  private val connectivity: Connectivity,
  private val remoteSessionsUpdater: RemoteSessionsUpdater,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Scoped {

  companion object : Corked("ReconnectSessionSync")

  override suspend fun onCreate() {
    val serverUrl = userSession.user?.serverUrl ?: return
    coroutineScopeHolder.get().launch {
      reconnectTriggers(
        status = serverReachability.status,
        inRange = serverReachability.observeInRange(serverUrl),
        online = connectivity.statusUpdates.map { it.isConnected },
      ).collect {
        ibark { "Server reachable again; syncing sessions recorded while away" }
        try {
          remoteSessionsUpdater.update(skipInterval = true)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          ebark(e) { "Reconnect sync failed" }
        }
      }
    }
  }
}

/**
 * Emits whenever the server may have just become reachable again: [status] turning
 * [Reachability.Reachable], [inRange] turning true, or [online] turning true. The initial values
 * are not transitions and never emit. Bursts (a network change typically flips several at once)
 * are collapsed by [settle].
 */
@OptIn(FlowPreview::class)
internal fun reconnectTriggers(
  status: Flow<Reachability>,
  inRange: Flow<Boolean>,
  online: Flow<Boolean>,
  settle: Duration = SETTLE,
): Flow<Unit> = merge(
  status.map { it == Reachability.Reachable }.risingEdges(),
  inRange.risingEdges(),
  online.risingEdges(),
).debounce(settle)

/** Emits each false → true change of [this], ignoring its initial value. */
private fun Flow<Boolean>.risingEdges(): Flow<Unit> = distinctUntilChanged()
  .runningFold(null as Pair<Boolean?, Boolean>?) { previous, current -> previous?.second to current }
  .filter { it != null && it.first == false && it.second }
  .map { }

private val SETTLE = 2.seconds
