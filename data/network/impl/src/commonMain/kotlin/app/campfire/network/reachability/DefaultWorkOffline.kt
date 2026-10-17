// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.ConnectionSettings
import app.campfire.settings.api.OfflineSession
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Resolves the stored [OfflineSession] and the mobile data rule against the current network and
 * time, and ends sessions whose condition is met: a timed one when its time runs out (checked at
 * least every minute, so a device waking from sleep catches up), a Wi-Fi one when the device next
 * joins Wi-Fi or Ethernet.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<WorkOffline>())
@Inject
class DefaultWorkOffline(
  private val connectionSettings: ConnectionSettings,
  private val networkMonitor: NetworkMonitor,
  private val fatherTime: FatherTime,
  @ForScope(AppScope::class) scope: CoroutineScope,
) : WorkOffline, Cork {

  override val tag: String = "WorkOffline"
  override val enabled: Boolean = true

  override val canDetectWifi: Boolean get() = networkMonitor.isSupported

  /**
   * Which Wi-Fi connection doesn't end an [OfflineSession.UntilWifi] that started on Wi-Fi.
   * Connection ids only compare within one process, so a session restored from an earlier launch
   * adopts the first Wi-Fi connection it sees instead: it errs on staying offline.
   */
  @Volatile
  private var wifiBaseline: WifiBaseline = when (val session = connectionSettings.offlineSession) {
    is OfflineSession.UntilWifi -> if (session.startedOnWifi) WifiBaseline.Adopt else WifiBaseline.AnyWifi
    else -> WifiBaseline.AnyWifi
  }

  private val _sessionEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  override val sessionEnded: Flow<Unit> = _sessionEnded.asSharedFlow()

  override val state: StateFlow<OfflineState> = combine(
    connectionSettings.observeOfflineSession(),
    connectionSettings.observeWorkOfflineOnMobileData(),
    connectionSettings.observeMobileDataOfflineSuspended(),
    networkMonitor.snapshot,
  ) { session, onMobileData, suspended, snapshot ->
    resolve(session, onMobileData, suspended, snapshot)
  }.stateIn(scope, SharingStarted.Eagerly, currentState())

  init {
    scope.launch {
      networkMonitor.snapshot.collect(::onNetwork)
    }
    scope.launch {
      connectionSettings.observeOfflineSession().collectLatest { session ->
        if (session !is OfflineSession.Until) return@collectLatest
        while (true) {
          val remaining = session.endsAtMillis - fatherTime.nowInEpochMillis()
          if (remaining <= 0) break
          // Capped so the deadline is rechecked after the device sleeps through it
          delay(remaining.coerceAtMost(TIMER_RECHECK.inWholeMilliseconds))
        }
        end(session)
      }
    }
  }

  override fun isOffline(): Boolean = currentState().isOffline

  override fun goOffline(choice: OfflineChoice) {
    val now = fatherTime.nowInEpochMillis()
    val session = when (choice) {
      OfflineChoice.UntilTurnedOff -> OfflineSession.UntilTurnedOff
      OfflineChoice.OneHour -> OfflineSession.Until(now + 1.hours.inWholeMilliseconds)
      OfflineChoice.FourHours -> OfflineSession.Until(now + 4.hours.inWholeMilliseconds)
      OfflineChoice.UntilTomorrow -> OfflineSession.Until(nextMorning(now, TimeZone.currentSystemDefault()))
      OfflineChoice.UntilWifi -> {
        val snapshot = networkMonitor.snapshot.value
        val onWifi = snapshot.isOnWifi
        wifiBaseline = if (onWifi) WifiBaseline.Connection(snapshot.id) else WifiBaseline.AnyWifi
        OfflineSession.UntilWifi(startedOnWifi = onWifi)
      }
    }
    connectionSettings.offlineSession = session
    ibark { "Working offline: $session" }
  }

  override fun goOnline() {
    val reason = currentState().reason
    connectionSettings.offlineSession = OfflineSession.None
    if (mobileDataRuleApplies(networkMonitor.snapshot.value)) {
      connectionSettings.mobileDataOfflineSuspended = true
    }
    ibark { "Going online (was $reason)" }
  }

  private fun onNetwork(snapshot: NetworkSnapshot) {
    if (!snapshot.isOnWifi) return
    if (connectionSettings.mobileDataOfflineSuspended) {
      connectionSettings.mobileDataOfflineSuspended = false
    }

    val session = connectionSettings.offlineSession as? OfflineSession.UntilWifi ?: return
    when (val baseline = wifiBaseline) {
      WifiBaseline.AnyWifi -> end(session)
      WifiBaseline.Adopt -> wifiBaseline = WifiBaseline.Connection(snapshot.id)
      is WifiBaseline.Connection -> if (snapshot.id != baseline.id) end(session)
    }
  }

  private fun end(session: OfflineSession) {
    if (connectionSettings.offlineSession != session) return
    connectionSettings.offlineSession = OfflineSession.None
    ibark { "Offline session ended: $session" }
    // The mobile data rule may still keep Campfire offline; then nothing came back online
    if (!isOffline()) _sessionEnded.tryEmit(Unit)
  }

  private fun currentState(): OfflineState = resolve(
    session = connectionSettings.offlineSession,
    onMobileData = connectionSettings.workOfflineOnMobileData,
    suspended = connectionSettings.mobileDataOfflineSuspended,
    snapshot = networkMonitor.snapshot.value,
  )

  private fun resolve(
    session: OfflineSession,
    onMobileData: Boolean,
    suspended: Boolean,
    snapshot: NetworkSnapshot,
  ): OfflineState {
    val sessionReason = when (session) {
      OfflineSession.None -> null
      OfflineSession.UntilTurnedOff -> OfflineReason.UntilTurnedOff
      is OfflineSession.Until -> OfflineReason.Until(session.endsAtMillis)
        .takeIf { fatherTime.nowInEpochMillis() < session.endsAtMillis }
      is OfflineSession.UntilWifi -> OfflineReason.UntilWifi(reconnect = session.startedOnWifi)
    }
    val ruleReason = OfflineReason.OnMobileData
      .takeIf { onMobileData && !suspended && snapshot.isMobileDataOnly }
    return OfflineState(reason = sessionReason ?: ruleReason, onWifi = snapshot.isOnWifi)
  }

  private fun mobileDataRuleApplies(snapshot: NetworkSnapshot): Boolean =
    connectionSettings.workOfflineOnMobileData &&
      !connectionSettings.mobileDataOfflineSuspended &&
      snapshot.isMobileDataOnly

  private val NetworkSnapshot.isOnWifi: Boolean
    get() = networkMonitor.isSupported && connected && localTransport != null

  /** On mobile data with no Wi-Fi or Ethernet; a VPN over mobile data still counts. */
  private val NetworkSnapshot.isMobileDataOnly: Boolean
    get() = networkMonitor.isSupported && connected && NetworkTransport.Cellular in transports && localTransport == null

  private sealed interface WifiBaseline {
    /** Any Wi-Fi connection ends the session. */
    data object AnyWifi : WifiBaseline

    /** The next Wi-Fi connection seen becomes the baseline. */
    data object Adopt : WifiBaseline

    data class Connection(val id: Long) : WifiBaseline
  }

  private companion object {
    val TIMER_RECHECK = 1.minutes
  }
}

/** 6 AM in [zone]: today's while it's still earlier than that, else tomorrow's. */
internal fun nextMorning(nowMillis: Long, zone: TimeZone): Long {
  val now = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone)
  val date = if (now.hour < MORNING_HOUR) now.date else now.date.plus(1, DateTimeUnit.DAY)
  return LocalDateTime(date, LocalTime(MORNING_HOUR, 0)).toInstant(zone).toEpochMilliseconds()
}

private const val MORNING_HOUR = 6
