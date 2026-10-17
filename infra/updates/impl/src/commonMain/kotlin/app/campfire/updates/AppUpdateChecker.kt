// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import app.campfire.core.time.FatherTime
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateSource
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/**
 * Checks the [AppUpdateSource] for an update each time the app comes to the foreground, at most
 * once per [AppUpdateSource.checkInterval], and whenever the source reports a change or a caller
 * asks for a [refresh].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@Inject
class AppUpdateChecker(
  private val appUpdateSource: AppUpdateSource,
  private val appLifecycleObserver: AppLifecycleObserver,
  private val fatherTime: FatherTime,
  @ForScope(AppScope::class) scope: CoroutineScope,
) {

  private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  private var lastCheckMillis: Long? = null

  val update: StateFlow<AppUpdate?> = if (appUpdateSource.isSupported) {
    merge(
      appLifecycleObserver.state
        .filter { it == AppLifecycleState.Foreground }
        .map { false },
      appUpdateSource.changes().map { true },
      refreshes.map { true },
    )
      // After the merge, so the throttle is read and written by one coroutine.
      .filter { forced -> shouldCheck(forced) }
      .mapLatest { appUpdateSource.getAvailableUpdate() }
      .stateIn(scope, SharingStarted.Eagerly, null)
  } else {
    MutableStateFlow<AppUpdate?>(null).asStateFlow()
  }

  /** Check again now, regardless of the interval, e.g. after the update sheet closes. */
  fun refresh() {
    refreshes.tryEmit(Unit)
  }

  private fun shouldCheck(forced: Boolean): Boolean {
    val now = fatherTime.nowInEpochMillis()
    val elapsed = lastCheckMillis?.let { (now - it).milliseconds }
    // A clock set backwards counts as due rather than holding checks off until it catches up.
    val due = forced ||
      elapsed == null ||
      elapsed.isNegative() ||
      elapsed >= appUpdateSource.checkInterval
    if (due) lastCheckMillis = now
    return due
  }
}
