package app.campfire.audioplayer.impl.cast

import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastState
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DesktopCastController() : CastController {
  override val state = MutableStateFlow(CastState.Unavailable)
  override val availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
  override fun connect(device: CastDevice) {
  }
}
