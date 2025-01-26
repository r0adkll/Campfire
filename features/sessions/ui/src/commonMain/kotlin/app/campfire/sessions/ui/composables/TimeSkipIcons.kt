package app.campfire.sessions.ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.common.compose.di.rememberComponent
import app.campfire.core.di.AppScope
import app.campfire.settings.api.PlaybackSettings
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.cd_forward_time
import campfire.features.sessions.ui.generated.resources.cd_rewind_time
import com.r0adkll.kimchi.annotations.ContributesTo
import org.jetbrains.compose.resources.stringResource

@ContributesTo(AppScope::class)
interface PlaybackSettingsComponent {
  val playbackSettings: PlaybackSettings
}

@Composable
internal fun ForwardIcon(
  modifier: Modifier = Modifier,
  component: PlaybackSettingsComponent = rememberComponent(),
) {
  val forwardTimeMs by component.playbackSettings
    .observeForwardTimeMs()
    .collectAsState()

  Icon(
    forwardTimeMs.asForwardImageVector(),
    contentDescription = stringResource(Res.string.cd_forward_time, forwardTimeMs / 1000),
    modifier = modifier,
  )
}

private fun Long.asForwardImageVector(): ImageVector = when (this) {
  5000L -> Icons.Rounded.Forward5
  10_000L -> Icons.Rounded.Forward10
  30_000L -> Icons.Rounded.Forward30
  else -> Icons.AutoMirrored.Rounded.Forward
}

@Composable
internal fun RewindIcon(
  modifier: Modifier = Modifier,
  component: PlaybackSettingsComponent = rememberComponent(),
) {
  val backwardTimeMs by component.playbackSettings
    .observeBackwardTimeMs()
    .collectAsState()

  Icon(
    backwardTimeMs.asReplayImageVector(),
    contentDescription = stringResource(Res.string.cd_rewind_time, backwardTimeMs / 1000),
    modifier = modifier,
  )
}

private fun Long.asReplayImageVector(): ImageVector = when (this) {
  5000L -> Icons.Rounded.Replay5
  10_000L -> Icons.Rounded.Replay10
  30_000L -> Icons.Rounded.Replay30
  else -> Icons.Rounded.Replay
}
