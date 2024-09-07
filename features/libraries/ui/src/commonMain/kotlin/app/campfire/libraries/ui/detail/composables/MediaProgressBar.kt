package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.extensions.timeAgo
import app.campfire.core.model.MediaProgress
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun MediaProgressBar(
  progress: MediaProgress,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
  ) {
    LinearProgressIndicator(
      progress = { progress.progress },
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(4.dp))

    Row {
      val remainingDurationMillis = (progress.duration - (progress.duration * progress.progress)) * 1000f
      val remainingDuration = remainingDurationMillis.roundToLong().milliseconds.readoutFormat()
      Text(
        text = remainingDuration,
        style = MaterialTheme.typography.labelSmall,
      )

      Spacer(Modifier.weight(1f))

      Text(
        text = "${progress.progress.times(100f).roundToInt()}%",
        style = MaterialTheme.typography.labelSmall,
      )
    }
  }
}
