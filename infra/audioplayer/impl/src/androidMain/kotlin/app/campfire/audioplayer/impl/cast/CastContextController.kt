package app.campfire.audioplayer.impl.cast

import android.app.Application
import androidx.annotation.MainThread
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastState
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import app.campfire.core.logging.bark
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState as GoogleCastState
import com.google.android.gms.cast.framework.CastStateListener
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(
  scope = AppScope::class,
  boundType = CastController::class,
)
@Inject
class CastContextController(
  private val application: Application,
) : CastController, CastStateListener, MediaRouter.Callback(), Cork {

  override val tag: String = "CastContextController"

  var castContext: CastContextState = CastContextState.Unavailable
    private set

  override val state = MutableStateFlow(CastState.Unavailable)

  private var devices = mutableListOf<CastDevice>()
  override val availableDevices = MutableStateFlow<List<CastDevice>>(devices)

  @MainThread
  fun initialize() {
    try {
      val context = CastContext.getSharedInstance(application)
      castContext = CastContextState.Ready(context)

      context.addCastStateListener(this)

      // Emit the current state, if any
      state.value = context.castState.asDomain()

      ibark { "CastController:initialize(state = ${context.castState.asDomain()})" }

      // Start scanning for devices
      scanForDevices()
    } catch (e: IllegalStateException) {
      e.printStackTrace()
      castContext = CastContextState.Error(e)
    }
  }

  @MainThread
  fun destroy() {
    try {
      val context = CastContext.getSharedInstance(application)
      context.removeCastStateListener(this)
    } catch (e: IllegalStateException) {
      bark(throwable = e) { "Failed to destroy CastContext" }
    } finally {
      castContext = CastContextState.Unavailable
      state.value = CastState.Unavailable

      stopScanningForDevices()
    }
  }

  @MainThread
  private fun scanForDevices() {
    try {
      val selector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_AUDIO_PLAYBACK)
        .build()

      val mediaRouter = MediaRouter.getInstance(application)

      mediaRouter.addCallback(selector, this, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)

      ibark { "CastController:scanForDevices()" }
    } catch (e: Exception) {
      ebark(throwable = e) { "Failed to start device scan" }
    }
  }

  @MainThread
  private fun stopScanningForDevices() {
    try {
      val mediaRouter = MediaRouter.getInstance(application)
      mediaRouter.removeCallback(this)
      ibark { "Stop scanning for devices" }
    } catch (e: Exception) {
      ebark(throwable = e) { "Failed to stop scanning for devices" }
    }
  }

  /*
   * CastStateListener methods
   */

  override fun onCastStateChanged(castState: Int) {
    state.value = castState.asDomain()
    ibark { "CastController:onCastStateChanged(state = ${castState.asDomain()})" }
  }

  /*
   * MediaRouter.Callback methods
   */

  override fun onRouteAdded(
    router: MediaRouter,
    route: MediaRouter.RouteInfo,
  ) {
    val newDevice = CastDevice(
      id = route.id,
      name = route.name,
      description = route.description,
      iconUri = route.iconUri?.toString(),
    )

    ibark { "CastController:onRouteAdded(device = $newDevice)" }

    devices += newDevice
    availableDevices.value = devices
  }

  override fun onRouteRemoved(
    router: MediaRouter,
    route: MediaRouter.RouteInfo,
  ) {
    ibark { "CastController:onRouteRemoved(device = $route)" }
    devices.removeIf { it.id == route.id }
    availableDevices.value = devices
  }

  override fun onRouteChanged(
    router: MediaRouter,
    route: MediaRouter.RouteInfo,
  ) {
    val updatedDevice = CastDevice(
      id = route.id,
      name = route.name,
      description = route.description,
      iconUri = route.iconUri?.toString()
    )

    ibark { "CastController:onRouteChanged(device = $updatedDevice)" }

    val existingIndex = devices.indexOfFirst { it.id == route.id }
    if (existingIndex != -1) {
      devices[existingIndex] = updatedDevice
      availableDevices.value = devices
    }
  }
}

sealed interface CastContextState {
  val contextOrNull: CastContext?
    get() = (this as? CastContextState.Ready)?.castContext

  data object Unavailable : CastContextState
  data class Ready(val castContext: CastContext) : CastContextState
  data class Error(val error: Throwable) : CastContextState
}

private fun Int.asDomain(): CastState = when (this) {
  GoogleCastState.CONNECTING -> CastState.Connecting
  GoogleCastState.CONNECTED -> CastState.Connected
  GoogleCastState.NOT_CONNECTED -> CastState.NotConnected
  GoogleCastState.NO_DEVICES_AVAILABLE -> CastState.NoDevicesAvailable
  else -> CastState.Unavailable
}
