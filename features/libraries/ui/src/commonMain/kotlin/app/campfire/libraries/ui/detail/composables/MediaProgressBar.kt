package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.widgets.FancyLinearProgressIndicator
import app.campfire.core.model.MediaProgress
import campfire.features.libraries.ui.generated.resources.Res
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
    )

    Spacer(Modifier.height(4.dp))

    Row {
      val remainingDurationMillis = (progress.duration - (progress.duration * progress.actualProgress)) * 1000f
      val remainingDuration = remainingDurationMillis.roundToLong().milliseconds.readoutFormat()
      Text(
        text = stringResource(Res.string.remaining_duration_format, remainingDuration),
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
