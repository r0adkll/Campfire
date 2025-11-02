package app.campfire.audioplayer.ui.cast

import androidx.compose.runtime.Composable
import app.campfire.audioplayer.cast.CastDevice
import campfire.infra.audioplayer.public_ui.generated.resources.Res
import campfire.infra.audioplayer.public_ui.generated.resources.label_this_phone
import org.jetbrains.compose.resources.stringResource

val CastDevice.displayName: String
  @Composable get() {
    return if (id == CastDevice.DEFAULT_ID) stringResource(Res.string.label_this_phone) else name
  }
