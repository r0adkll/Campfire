package app.campfire.audioplayer.impl.cast

import android.app.Application
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouter.RouteInfo
import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastState
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import app.campfire.core.logging.LogPriority
import com.google.android.gms.cast.framework.CastState as GoogleCastState
import com.google.android.gms.cast.framework.CastStateListener
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(
  scope = AppScope::class,
  boundType = CastController::class,
)
@Inject
class MediaRouterCastController(
  private val application: Application,
) : CastController,
  CastStateListener,
  MediaRouter.Callback(),
  Cork {

  override val tag: String = "CastContextController"

  override val state = MutableStateFlow(CastState.Unavailable)
  override val availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())

  @MainThread
  fun initialize() {
    try {
      val castContext = SafeCastContext.getContext(application) ?: return
      castContext.addCastStateListener(this)

      // Emit the current state, if any
      state.value = castContext.castState.asDomain()

      ibark { "CastController:initialize(state = ${castContext.castState.asDomain()})" }
    } catch (e: Throwable) {
      wbark(throwable = e) { "Unable to initialize CastContext" }
    }
  }

  @MainThread
  fun destroy() {
    try {
      SafeCastContext.getContext(application)?.removeCastStateListener(this)
    } catch (e: Throwable) {
      wbark(throwable = e) { "Failed to destroy CastContext" }
    } finally {
      state.value = CastState.Unavailable
      availableDevices.value = emptyList()
    }
  }

  @MainThread
  fun scanForDevices() {
    try {
      val selector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_AUDIO_PLAYBACK)
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
        .build()

      val mediaRouter = MediaRouter.getInstance(application)

      mediaRouter.addCallback(selector, this, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)

      // Enumerate existing routes immediately so devices that are already
      // available appear without waiting for a callback event.
      updateDevices(mediaRouter)

      ibark { "CastController:scanForDevices()" }
    } catch (e: Throwable) {
      wbark(throwable = e) { "Failed to start device scan" }
    }
  }

  @MainThread
  fun stopScanningForDevices() {
    try {
      val mediaRouter = MediaRouter.getInstance(application)
      mediaRouter.removeCallback(this)
      ibark { "Stop scanning for devices" }
    } catch (e: Throwable) {
      wbark(throwable = e) { "Failed to stop scanning for devices" }
    }
  }

  override fun connect(device: CastDevice) {
    try {
      val mediaRouteCastDevice = device as MediaRouterCastDevice
      val mediaRouter = MediaRouter.getInstance(application)
      ibark { "Connecting route: ${mediaRouteCastDevice.route}" }
      mediaRouter.selectRoute(mediaRouteCastDevice.route)
    } catch (e: Throwable) {
      wbark(throwable = e) { "Failed to connect to device" }
    }
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
    updateDevicesAndState(router)
  }

  override fun onRouteUnselected(router: MediaRouter, route: RouteInfo, reason: Int) {
    ibark { "CastController:onRouteUnselected(device = ${route.id}, reason = $reason)" }
    updateDevicesAndState(router)
  }

  override fun onRouteAdded(
    router: MediaRouter,
    route: RouteInfo,
  ) {
    ibark { "CastController:onRouteAdded(route = ${route.id})" }
    updateDevicesAndState(router)
  }

  override fun onRouteRemoved(
    router: MediaRouter,
    route: RouteInfo,
  ) {
    ibark { "CastController:onRouteRemoved(route = ${route.id})" }
    updateDevicesAndState(router)
  }

  override fun onRouteChanged(
    router: MediaRouter,
    route: RouteInfo,
  ) {
    ibark { "CastController:onRouteChanged(route = ${route.id})" }
    updateDevicesAndState(router)
  }

  /**
   * Update both the device list and derive connection state from
   * the MediaRouter's selected route so non-Cast devices (Bluetooth,
   * wired headphones, etc.) are accurately reflected.
   */
  private fun updateDevicesAndState(router: MediaRouter) {
    updateDevices(router)

    val selected = router.selectedRoute
    if (!selected.isDefaultOrBluetooth()) {
      // A non-default, non-system route is selected – treat as connected
      state.value = CastState.Connected
    }
    // Otherwise let the CastStateListener drive the state value
  }

  private fun updateDevices(router: MediaRouter) {
    val selectedRouteId = router.selectedRoute.id

    availableDevices.value = router.routes
      .filter { it.isEnabled }
      .filter { it.description != "Google Cast Multizone Member" }
      .filter { route ->
        val extras: Bundle? = route.extras
        if (extras != null) {
          try {
            if (extras.getString("com.google.android.gms.cast.EXTRA_SESSION_ID") != null) {
              return@filter false
            }
          } catch (t: Throwable) {
            bark(LogPriority.ERROR, throwable = t) { "Unable to find class for EXTRA_SESSION_ID" }
          }
        }
        true
      }
      .sortedBy {
        when {
          it.isSystemRoute -> 0
          else -> Int.MAX_VALUE
        }
      }
      .map { route ->
        MediaRouterCastDevice(route, isSelected = route.id == selectedRouteId)
      }
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
