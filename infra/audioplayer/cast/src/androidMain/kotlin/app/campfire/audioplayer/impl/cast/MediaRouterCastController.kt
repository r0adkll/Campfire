// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.cast.DefaultCastOptionsProvider
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouter.RouteInfo
import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastState
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
import app.campfire.core.permission.LocalNetworkPermissionController
import com.google.android.gms.cast.CastDevice as GmsCastDevice
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState as GoogleCastState
import com.google.android.gms.cast.framework.CastStateListener
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * Discovers Google Cast (and system output) routes and exposes them to the shared cast UI.
 *
 * Lifecycle is driven by [ProcessLifecycleOwner] rather than the host Activity: an app-scoped
 * singleton torn down from `Activity.onStop`/`onDestroy` loses its MediaRouter callback on every
 * configuration change, because the old activity's teardown runs *after* the new activity's
 * startup and unregisters what was just registered.
 *
 * Discovery intensity follows the SDK's own UI conventions: passive
 * [MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY] while the app is foregrounded, and
 * [MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN] only while the device picker is open. Active
 * scans self-suppress after 30 seconds, so the picker re-registers the callback on a timer to
 * keep the window fresh.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
  scope = AppScope::class,
  boundType = CastController::class,
  replaces = [NoOpCastController::class],
)
@Inject
class MediaRouterCastController(
  private val application: Application,
  private val localNetworkPermission: LocalNetworkPermissionController,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : CastController,
  CastStateListener,
  MediaRouter.Callback(),
  Cork {

  override val tag: String = "CastContextController"

  override val state = MutableStateFlow(CastState.Unavailable)
  override val availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
  override val needsLocalNetworkPermission = MutableStateFlow(false)

  private var initialized = false
  private var castContext: CastContext? = null
  private var foregrounded = false
  private var devicePickerOpen = false
  private var activeScanRefreshJob: Job? = null

  private val processLifecycleObserver = object : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
      foregrounded = true
      SafeCastContext.initialize(application)
      updateDiscovery()
    }

    override fun onStop(owner: LifecycleOwner) {
      foregrounded = false
      updateDiscovery()
    }
  }

  /**
   * Called once at app startup by [CastControllerInitializer]. Registers the process-lifecycle
   * observer that drives discovery from app foreground state.
   */
  @MainThread
  fun initialize() {
    if (initialized) return
    initialized = true

    needsLocalNetworkPermission.value = localNetworkPermission.isPermissionMissing()
    ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
    applicationScope.launch(dispatcherProvider.main) {
      val context = SafeCastContext.castContext.filterNotNull().first()
      onCastContextReady(context)
    }
  }

  @MainThread
  private fun onCastContextReady(context: CastContext) {
    castContext = context
    context.addCastStateListener(this)
    state.value = context.castState.asDomain()
    updateDiscovery()
    ibark { "CastController:ready(state = ${context.castState.asDomain()})" }
  }

  override fun connect(device: CastDevice) {
    try {
      val mediaRouter = MediaRouter.getInstance(application)
      // Re-find the live route by id: MediaRouter silently ignores selection of a RouteInfo
      // instance that is no longer in its current route list.
      val route = mediaRouter.routes.find { it.id == device.id }
      if (route == null) {
        wbark { "Route ${device.id} is no longer available; ignoring connect request" }
        return
      }
      ibark { "Connecting route: $route" }
      mediaRouter.selectRoute(route)
    } catch (e: Throwable) {
      wbark(throwable = e) { "Failed to connect to device" }
    }
  }

  @MainThread
  override fun startActiveScan() {
    devicePickerOpen = true
    needsLocalNetworkPermission.value = localNetworkPermission.isPermissionMissing()
    activeScanRefreshJob?.cancel()
    activeScanRefreshJob = applicationScope.launch(dispatcherProvider.main) {
      while (isActive) {
        updateDiscovery()
        delay(ACTIVE_SCAN_REFRESH_MS)
      }
    }
    ibark { "CastController:startActiveScan()" }
  }

  @MainThread
  override fun stopActiveScan() {
    devicePickerOpen = false
    activeScanRefreshJob?.cancel()
    activeScanRefreshJob = null
    updateDiscovery()
    ibark { "CastController:stopActiveScan()" }
  }

  override fun requestLocalNetworkPermission() {
    applicationScope.launch(dispatcherProvider.main) {
      localNetworkPermission.request()
      val missing = localNetworkPermission.isPermissionMissing()
      needsLocalNetworkPermission.value = missing
      if (!missing) {
        restartDiscovery()
      }
    }
  }

  /**
   * Fully stops and restarts route discovery. Needed after the local-network permission is
   * granted mid-session: enforcement happens per socket operation, so the cast module's mDNS
   * sockets — created while access was denied, with failed multicast group joins — stay dead
   * after the grant. Dropping every discovery request and cycling the receiver application id
   * makes the Cast route provider tear down its scanner and recreate those sockets under the
   * granted permission.
   */
  @OptIn(UnstableApi::class)
  private suspend fun restartDiscovery() {
    try {
      activeScanRefreshJob?.cancel()
      activeScanRefreshJob = null
      MediaRouter.getInstance(application).removeCallback(this)

      // No session can exist here (discovery was dead without the permission), so cycling the
      // receiver id only affects the provider's discovery configuration.
      castContext?.let { context ->
        context.setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
        delay(DISCOVERY_RESTART_DELAY_MS)
        context.setReceiverApplicationId(DefaultCastOptionsProvider.APP_ID_DEFAULT_RECEIVER_WITH_DRM)
      }
      delay(DISCOVERY_RESTART_DELAY_MS)

      // In case CastContext initialization itself failed while the permission was missing
      SafeCastContext.initialize(application)

      if (devicePickerOpen) {
        startActiveScan()
      } else {
        updateDiscovery()
      }
      ibark { "CastController:restartDiscovery()" }
    } catch (e: Throwable) {
      wbark(throwable = e) { "Failed to restart discovery after permission grant" }
    }
  }

  /**
   * Single point of (de)registration for the MediaRouter callback. Re-invoking
   * [MediaRouter.addCallback] with the same callback updates its selector and flags in place.
   */
  @MainThread
  private fun updateDiscovery() {
    try {
      val mediaRouter = MediaRouter.getInstance(application)
      val context = castContext
      if (context == null || !foregrounded) {
        mediaRouter.removeCallback(this)
        return
      }

      // The Cast route provider only reports devices for selectors containing its own
      // category; the merged selector is the SDK's source of truth for that.
      val selector = context.mergedSelector ?: defaultSelector()
      val flags = if (devicePickerOpen) {
        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
      } else {
        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
      }
      mediaRouter.addCallback(selector, this, flags)

      // Enumerate existing routes immediately so devices that are already
      // available appear without waiting for a callback event.
      updateDevices(mediaRouter)
    } catch (e: Throwable) {
      wbark(throwable = e) { "Failed to update device discovery" }
    }
  }

  @OptIn(UnstableApi::class)
  private fun defaultSelector(): MediaRouteSelector {
    return MediaRouteSelector.Builder()
      .addControlCategory(
        CastMediaControlIntent.categoryForCast(DefaultCastOptionsProvider.APP_ID_DEFAULT_RECEIVER_WITH_DRM),
      )
      .build()
  }

  /*
   * CastStateListener methods
   */

  override fun onCastStateChanged(castState: Int) {
    state.value = castState.asDomain()
    ibark { "CastController:onCastStateChanged(state = ${castState.asDomain()})" }

    // Refresh device list so isSelected stays in sync with Cast state changes
    try {
      val mediaRouter = MediaRouter.getInstance(application)
      updateDevices(mediaRouter)
    } catch (e: Throwable) {
      wbark(throwable = e) { "Failed to update devices after cast state change" }
    }
  }

  /*
   * MediaRouter.Callback methods
   */

  override fun onRouteSelected(router: MediaRouter, selectedRoute: RouteInfo, reason: Int, requestedRoute: RouteInfo) {
    ibark {
      "CastController:onRouteSelected(device = ${selectedRoute.id}, reason = $reason, " +
        "requestedRoute = ${requestedRoute.id})"
    }
    updateDevices(router)
  }

  override fun onRouteUnselected(router: MediaRouter, route: RouteInfo, reason: Int) {
    ibark { "CastController:onRouteUnselected(device = ${route.id}, reason = $reason)" }
    updateDevices(router)
  }

  override fun onRouteAdded(
    router: MediaRouter,
    route: RouteInfo,
  ) {
    ibark { "CastController:onRouteAdded(route = ${route.id})" }
    updateDevices(router)
  }

  override fun onRouteRemoved(
    router: MediaRouter,
    route: RouteInfo,
  ) {
    ibark { "CastController:onRouteRemoved(route = ${route.id})" }
    updateDevices(router)
  }

  override fun onRouteChanged(
    router: MediaRouter,
    route: RouteInfo,
  ) {
    ibark { "CastController:onRouteChanged(route = ${route.id})" }
    updateDevices(router)
  }

  private fun updateDevices(router: MediaRouter) {
    val selectedRoute = router.selectedRoute
    val selectedCastDeviceId = selectedRoute.castDeviceId

    availableDevices.value = router.routes
      .filter { it.isEnabled }
      .filter { it.description != MULTIZONE_MEMBER_DESCRIPTION }
      // The Cast provider publishes an extra session-scoped route for a connected device
      // alongside the device's own route; drop the duplicate.
      .filter { route -> route.extras?.getString(EXTRA_SESSION_ID) == null }
      .distinctBy { route -> route.castDeviceId ?: route.id }
      .sortedBy {
        when {
          it.isSystemRoute -> 0
          else -> Int.MAX_VALUE
        }
      }
      .map { route ->
        // Selection may land on the session-scoped duplicate we filtered out, so match the
        // underlying device by its Cast device id as well as by route id.
        val isSelected = route.id == selectedRoute.id ||
          (selectedCastDeviceId != null && route.castDeviceId == selectedCastDeviceId)
        MediaRouterCastDevice(route, isSelected = isSelected)
      }
  }

  private val RouteInfo.castDeviceId: String?
    get() = try {
      GmsCastDevice.getFromBundle(extras ?: return null)?.deviceId
    } catch (t: Throwable) {
      null
    }

  companion object {
    private const val EXTRA_SESSION_ID = "com.google.android.gms.cast.EXTRA_SESSION_ID"
    private const val MULTIZONE_MEMBER_DESCRIPTION = "Google Cast Multizone Member"

    // Active scans are suppressed 30s after registration
    // (MediaRouterActiveScanThrottlingHelper.MAX_ACTIVE_SCAN_DURATION_MS); re-register
    // just under that to keep scanning while the picker stays open.
    private const val ACTIVE_SCAN_REFRESH_MS = 25_000L

    // Propagation window for an empty discovery request / receiver-id change to reach the
    // Cast route provider before discovery is re-registered.
    private const val DISCOVERY_RESTART_DELAY_MS = 500L
  }
}

class MediaRouterCastDevice(
  internal val route: RouteInfo,
  isSelected: Boolean,
) : CastDevice(
  id = route.id,
  name = route.name,
  description = null,
  iconUri = route.iconUri?.toString(),
  type = route.deviceType.asType(),
  isSelected = isSelected,
)

private fun Int.asDomain(): CastState = when (this) {
  GoogleCastState.CONNECTING -> CastState.Connecting
  GoogleCastState.CONNECTED -> CastState.Connected
  GoogleCastState.NOT_CONNECTED -> CastState.NotConnected
  GoogleCastState.NO_DEVICES_AVAILABLE -> CastState.NoDevicesAvailable
  else -> CastState.Unavailable
}

private fun Int.asType(): CastDevice.Type = when (this) {
  RouteInfo.DEVICE_TYPE_REMOTE_SPEAKER,
  RouteInfo.DEVICE_TYPE_BUILTIN_SPEAKER,
  RouteInfo.DEVICE_TYPE_AUDIO_VIDEO_RECEIVER,
  -> CastDevice.Type.SPEAKER

  RouteInfo.DEVICE_TYPE_BLUETOOTH_A2DP -> CastDevice.Type.BLUETOOTH

  RouteInfo.DEVICE_TYPE_TABLET_DOCKED,
  RouteInfo.DEVICE_TYPE_TABLET,
  -> CastDevice.Type.TABLET

  RouteInfo.DEVICE_TYPE_TV -> CastDevice.Type.TV
  RouteInfo.DEVICE_TYPE_COMPUTER -> CastDevice.Type.COMPUTER
  RouteInfo.DEVICE_TYPE_GAME_CONSOLE -> CastDevice.Type.GAME_CONSOLE
  RouteInfo.DEVICE_TYPE_CAR -> CastDevice.Type.CAR

  RouteInfo.DEVICE_TYPE_SMARTWATCH -> CastDevice.Type.SMARTWATCH
  RouteInfo.DEVICE_TYPE_SMARTPHONE -> CastDevice.Type.SMARTPHONE

  RouteInfo.DEVICE_TYPE_WIRED_HEADSET,
  RouteInfo.DEVICE_TYPE_USB_HEADSET,
  RouteInfo.DEVICE_TYPE_BLE_HEADSET,
  RouteInfo.DEVICE_TYPE_WIRED_HEADPHONES,
  -> CastDevice.Type.HEADPHONES

  RouteInfo.DEVICE_TYPE_HDMI_ARC,
  RouteInfo.DEVICE_TYPE_HDMI_EARC,
  RouteInfo.DEVICE_TYPE_HDMI,
  -> CastDevice.Type.HDMI

  RouteInfo.DEVICE_TYPE_DOCK,
  RouteInfo.DEVICE_TYPE_USB_ACCESSORY,
  RouteInfo.DEVICE_TYPE_USB_DEVICE,
  -> CastDevice.Type.USB

  RouteInfo.DEVICE_TYPE_HEARING_AID -> CastDevice.Type.HEARING_AID
  else -> CastDevice.Type.UNKNOWN
}
