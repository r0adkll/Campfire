// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.composables

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Forward
import app.campfire.common.compose.icons.rounded.Forward10
import app.campfire.common.compose.icons.rounded.Forward30
import app.campfire.common.compose.icons.rounded.Forward5
import app.campfire.common.compose.icons.rounded.Replay
import app.campfire.common.compose.icons.rounded.Replay10
import app.campfire.common.compose.icons.rounded.Replay30
import app.campfire.common.compose.icons.rounded.Replay5
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
  5000L -> CampfireIcons.Rounded.Forward5
  10_000L -> CampfireIcons.Rounded.Forward10
  30_000L -> CampfireIcons.Rounded.Forward30
  else -> CampfireIcons.Rounded.Forward
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
  5000L -> CampfireIcons.Rounded.Replay5
  10_000L -> CampfireIcons.Rounded.Replay10
  30_000L -> CampfireIcons.Rounded.Replay30
  else -> CampfireIcons.Rounded.Replay
}
