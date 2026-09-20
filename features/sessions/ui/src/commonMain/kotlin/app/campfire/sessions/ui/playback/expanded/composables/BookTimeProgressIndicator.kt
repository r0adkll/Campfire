// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.KeyboardDoubleArrowRight
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Session
import app.campfire.sessions.ui.playback.PlayerUiState
import kotlin.time.Duration

@Composable
internal fun ColumnScope.BookTimeProgressIndicator(
  session: Session?,
  playerState: PlayerUiState,
) {
  val bookDuration = session?.duration ?: Duration.ZERO
  if (bookDuration != Duration.ZERO) {
    LinearProgressIndicator(
      progress = { (playerState.bookTime / bookDuration).toFloat() },
      trackColor = MaterialTheme.colorScheme.surfaceContainer,
      color = MaterialTheme.colorScheme.secondary,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 32.dp),
    )

    Spacer(Modifier.height(4.dp))

    TimeRemainingText(
      bookDuration = bookDuration,
      bookTime = playerState.bookTime,
      playbackSpeed = playerState.speed,
      modifier = Modifier.align(Alignment.CenterHorizontally),
    )
  } else {
    LinearProgressIndicator(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 32.dp),
    )
  }
}

@Composable
private fun TimeRemainingText(
  bookDuration: Duration,
  bookTime: Duration,
  playbackSpeed: Float,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
  ) {
    val isAccelerated = playbackSpeed != 1f
    AnimatedVisibility(
      visible = isAccelerated,
    ) {
      Icon(
        CampfireIcons.Rounded.KeyboardDoubleArrowRight,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.secondary,
      )
    }

    val currentRemainingDuration = (bookDuration - bookTime).div(playbackSpeed.toDouble())
    Text(
      text = "${currentRemainingDuration.readoutFormat()} left",
      style = MaterialTheme.typography.labelSmall.fluentIf(isAccelerated) {
        copy(
          fontWeight = FontWeight.Bold,
          fontStyle = FontStyle.Italic,
          color = MaterialTheme.colorScheme.secondary,
          fontSize = 12.sp,
        )
      },
    )
  }
}
