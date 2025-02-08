package app.campfire.audioplayer.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import campfire.infra.audioplayer.public_ui.generated.resources.Res
import campfire.infra.audioplayer.public_ui.generated.resources.action_clear_timer
import campfire.infra.audioplayer.public_ui.generated.resources.label_current_timer
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RunningTimerCard(
  runningTimer: RunningTimer,
  onTimerCleared: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 16.dp, horizontal = 16.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    tonalElevation = 4.dp,
    shadowElevation = 1.dp,
  ) {
    Column {
      // Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp),
      ) {
        Icon(
          Icons.Outlined.Timer,
          contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(Res.string.label_current_timer),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }

      RunningTimerText(
        runningTimer = runningTimer,
        style = { timer ->
          when (timer) {
            is PlaybackTimer.EndOfChapter -> MaterialTheme.typography.titleLarge
            is PlaybackTimer.Epoch -> MaterialTheme.typography.displayMedium
          }
        },
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .height(56.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = onTimerCleared,
        ) {
          Text(stringResource(Res.string.action_clear_timer))
        }
      }
    }
  }
}
