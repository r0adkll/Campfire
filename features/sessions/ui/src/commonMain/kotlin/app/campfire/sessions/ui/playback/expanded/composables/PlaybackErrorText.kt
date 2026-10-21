// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import app.campfire.audioplayer.PlaybackEngineUnavailableException
import app.campfire.audioplayer.PlaybackFailedException
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.playback_engine_unavailable_message
import campfire.features.sessions.ui.generated.resources.playback_error_code
import campfire.features.sessions.ui.generated.resources.playback_error_format
import campfire.features.sessions.ui.generated.resources.playback_error_network
import campfire.features.sessions.ui.generated.resources.playback_error_not_found
import campfire.features.sessions.ui.generated.resources.playback_error_unauthorized
import campfire.features.sessions.ui.generated.resources.playback_error_unknown
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Explains a player [error] in terms the user can act on, followed by the player's error code
 * when it has one, so a report of the failure says what actually went wrong.
 */
@Composable
internal fun PlaybackErrorText(
  error: Throwable,
  modifier: Modifier = Modifier,
) {
  val message = stringResource(error.messageResource)
  val code = (error as? PlaybackFailedException)?.code
  Text(
    text = if (code != null) {
      message + "\n" + stringResource(Res.string.playback_error_code, code)
    } else {
      message
    },
    textAlign = TextAlign.Center,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.error,
    fontStyle = FontStyle.Italic,
    modifier = modifier,
  )
}

private val Throwable.messageResource: StringResource
  get() = when (this) {
    is PlaybackEngineUnavailableException -> Res.string.playback_engine_unavailable_message
    is PlaybackFailedException -> when (kind) {
      PlaybackFailedException.Kind.Network -> Res.string.playback_error_network
      PlaybackFailedException.Kind.Unauthorized -> Res.string.playback_error_unauthorized
      PlaybackFailedException.Kind.NotFound -> Res.string.playback_error_not_found
      PlaybackFailedException.Kind.UnsupportedFormat -> Res.string.playback_error_format
      PlaybackFailedException.Kind.Unknown -> Res.string.playback_error_unknown
    }
    else -> Res.string.playback_error_unknown
  }
