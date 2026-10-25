// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.AppStateSettings
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateCheckException
import app.campfire.updates.source.AppUpdateSheet
import app.campfire.updates.source.AppUpdateSource
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Checks the [AppUpdateSource] for an update each time the app comes to the foreground, at most
 * once per [AppUpdateSource.checkInterval], and whenever the source reports a change or a caller
 * asks for a [refresh].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AppUpdateChecker(
  private val appUpdateSource: AppUpdateSource,
  private val appLifecycleObserver: AppLifecycleObserver,
  private val fatherTime: FatherTime,
  private val appStateSettings: AppStateSettings,
  @ForScope(AppScope::class) private val scope: CoroutineScope,
) : AppUpdates {

  private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  private var lastCheckMillis: Long? = null
  private var lastUpdate: AppUpdate? = null

  override val isSupported: Boolean get() = appUpdateSource.isSupported

  override val channel: String? get() = appUpdateSource.channel

  private val _check = MutableStateFlow(AppUpdateCheck())
  override val check: StateFlow<AppUpdateCheck> = _check.asStateFlow()

  override val update: StateFlow<AppUpdate?> = if (appUpdateSource.isSupported) {
    merge(
      appLifecycleObserver.state
        .filter { it == AppLifecycleState.Foreground }
        .map { Trigger.Foreground },
      appUpdateSource.changes().map { Trigger.Change },
      refreshes.map { Trigger.Refresh },
    )
      // After the merge, so the throttle is read and written by one coroutine.
      .filter { trigger -> shouldCheck(forced = trigger != Trigger.Foreground) }
      .mapLatest { trigger -> check(trigger) }
      .stateIn(scope, SharingStarted.Eagerly, null)
  } else {
    MutableStateFlow<AppUpdate?>(null).asStateFlow()
  }

  override val undismissedUpdate: StateFlow<AppUpdate?> = combine(
    update,
    appStateSettings.observeAppUpdateDismissedVersionCode(),
  ) { update, dismissedVersionCode ->
    update?.takeIf { !it.status.isDismissable || it.versionCode != dismissedVersionCode }
  }.stateIn(scope, SharingStarted.Eagerly, null)

  override val installSheet: AppUpdateSheet = appUpdateSource.updateSheet
    ?: CampfireAppUpdateSheet(appUpdateSource, update)

  private val _openedBuild = MutableStateFlow<OpenedBuild?>(null)
  override val openedBuild: StateFlow<OpenedBuild?> = _openedBuild.asStateFlow()

  override fun refresh() {
    refreshes.tryEmit(Unit)
  }

  override fun dismiss(update: AppUpdate) {
    appStateSettings.appUpdateDismissedVersionCode = update.versionCode
  }

  override fun openBuild(buildId: String) {
    if (!appUpdateSource.isSupported) return
    scope.launch {
      val opened = try {
        if (appUpdateSource.openBuild(buildId)) OpenedBuild.Offered else null
      } catch (e: AppUpdateCheckException) {
        bark(LogPriority.WARN, throwable = e) { "Couldn't open build $buildId: ${e.message}" }
        OpenedBuild.Failed
      }
      opened?.let { _openedBuild.value = it }
    }
  }

  override fun closeOpenedBuild() {
    appUpdateSource.closeOpenedBuild()
    _openedBuild.value = null
    refresh()
  }

  private suspend fun check(trigger: Trigger): AppUpdate? {
    // A source change (a download progressing, say) only re-reads the source's state, so it
    // isn't shown as a check.
    val tracked = trigger != Trigger.Change
    if (tracked) _check.update { it.copy(isChecking = true) }
    return try {
      appUpdateSource.getAvailableUpdate().also { update ->
        lastUpdate = update
        if (tracked) _check.value = AppUpdateCheck(checkedAt = now())
      }
    } catch (e: AppUpdateCheckException) {
      bark(LogPriority.WARN, throwable = e) { "App update check failed: ${e.message}" }
      if (tracked) _check.value = AppUpdateCheck(checkedAt = now(), failed = true)
      // Keep what the last check found rather than reading the failure as up to date.
      lastUpdate
    }
  }

  private fun now(): Instant = Instant.fromEpochMilliseconds(fatherTime.nowInEpochMillis())

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

  private enum class Trigger { Foreground, Change, Refresh }
}

/** Whether the drawer card can be dismissed: not once the update is under way. */
internal val AppUpdate.Status.isDismissable: Boolean
  get() = when (this) {
    AppUpdate.Status.Available,
    AppUpdate.Status.ChannelEnded,
    AppUpdate.Status.ReinstallRequired,
    AppUpdate.Status.Failed,
    -> true

    is AppUpdate.Status.Downloading,
    AppUpdate.Status.ReadyToInstall,
    AppUpdate.Status.Installing,
    -> false
  }
