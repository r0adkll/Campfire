package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.widgets.FancyLinearProgressIndicator
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.readableFormat
import app.campfire.core.model.MediaProgress
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.remaining_duration_finished
import campfire.features.libraries.ui.generated.resources.remaining_duration_format
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MediaProgressBar(
  progress: MediaProgress,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth(),
  ) {
    FancyLinearProgressIndicator(
      progress = { progress.actualProgress },
      modifier = Modifier.fillMaxWidth(),
      strokeCap = StrokeCap.Round,
      color = if (progress.isFinished) {
        MaterialTheme.colorScheme.inversePrimary
      } else {
        MaterialTheme.colorScheme.primary
      },
    )

    Spacer(Modifier.height(4.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (progress.isFinished) {
        Icon(
          Icons.Rounded.Check,
          contentDescription = null,
          tint = Color.Green,
          modifier = Modifier
            .size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
      }

      val remainingText = when {
        progress.isFinished -> stringResource(
          Res.string.remaining_duration_finished,
          progress.finishedAt!!.asDate().readableFormat,
        )
        else -> {
          val remainingDurationMillis = (progress.duration - (progress.duration * progress.actualProgress)) * 1000f
          val remainingDuration = remainingDurationMillis.roundToLong().milliseconds.readoutFormat()
          stringResource(Res.string.remaining_duration_format, remainingDuration)
        }
      }
      Text(
        text = remainingText,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
      )

      Spacer(Modifier.weight(1f))

      Text(
        text = "${progress.actualProgress.times(100f).roundToInt()}%",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
