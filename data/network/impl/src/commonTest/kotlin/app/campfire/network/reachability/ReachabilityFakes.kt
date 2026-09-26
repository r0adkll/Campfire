// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.permission.LocalNetworkPermissionController
import app.campfire.core.time.FatherTime
import app.campfire.settings.test.FakeDevSettings
import app.campfire.settings.test.FakeLocalServerSettings
import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

internal fun reachability(
  connectivity: Connectivity = FakeConnectivity(Connectivity.Status.Connected(metered = false)),
  monitor: NetworkMonitor = FakeNetworkMonitor(supported = false),
  localServerSettings: FakeLocalServerSettings = FakeLocalServerSettings(),
  devSettings: FakeDevSettings = FakeDevSettings(),
  localNetworkPermission: FakeLocalNetworkPermission = FakeLocalNetworkPermission(),
  time: FakeFatherTime = FakeFatherTime(),
): DefaultServerReachability = DefaultServerReachability(
  connectivity = connectivity,
  networkMonitor = monitor,
  localServerSettings = localServerSettings,
  devSettings = devSettings,
  localNetworkPermission = localNetworkPermission,
  fatherTime = time,
)

internal class FakeLocalNetworkPermission(missing: Boolean = false) : LocalNetworkPermissionController {
  val missing = MutableStateFlow(missing)

  override suspend fun requestIfNeeded(serverUrl: String): Boolean = !missing.value
  override fun isPermissionMissing(): Boolean = missing.value
  override fun observePermissionMissing(): Flow<Boolean> = missing
}

internal class FakeFatherTime(var nowMillis: Long = 1_000_000L) : FatherTime {
  override fun now(): LocalDateTime = error("unused")
  override fun today(): LocalDate = error("unused")
  override fun nowInEpochMillis(): Long = nowMillis
}

internal class FakeConnectivity(initial: Connectivity.Status) : Connectivity {
  private val updates = MutableSharedFlow<Connectivity.Status>(replay = 1).apply { tryEmit(initial) }

  override val statusUpdates: SharedFlow<Connectivity.Status> = updates
  override val monitoring: StateFlow<Boolean> = MutableStateFlow(true)

  override suspend fun status(): Connectivity.Status = updates.replayCache.last()

  fun update(status: Connectivity.Status) {
    updates.tryEmit(status)
  }

  override fun start() = Unit
  override fun stop() = Unit
  override fun close() = Unit
}

internal class FakeNetworkMonitor(
  supported: Boolean = true,
  initial: NetworkSnapshot = NetworkSnapshot.Unknown,
) : NetworkMonitor {
  override val isSupported: Boolean = supported
  override val snapshot = MutableStateFlow(initial)
}

internal fun wifi(
  subnet: String = "192.168.1.0/24",
  gateway: String? = "192.168.1.1",
  vpn: Boolean = false,
  id: Long = 1L,
) = NetworkSnapshot(
  connected = true,
  metered = false,
  transports = buildSet {
    add(NetworkTransport.Wifi)
    if (vpn) add(NetworkTransport.Vpn)
  },
  fingerprint = NetworkFingerprint(subnet, gateway),
  domain = null,
  id = id,
)

internal fun cellular(vpn: Boolean = false) = NetworkSnapshot(
  connected = true,
  metered = true,
  transports = buildSet {
    add(NetworkTransport.Cellular)
    if (vpn) add(NetworkTransport.Vpn)
  },
  fingerprint = null,
  domain = null,
)
