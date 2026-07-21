package app.campfire.ui.settings.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.settings.api.ResumeRewindTier
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_playback_resume_rewind_none
import campfire.features.settings.ui.generated.resources.setting_playback_resume_rewind_tier_title
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.compose.resources.stringResource

/**
 * A slider that edits a single [Duration] setting, snapping to [stepSeconds] increments within [valueRange].
 */
@Composable
internal fun DurationSliderSetting(
  title: String,
  value: Duration,
  valueRange: ClosedRange<Duration>,
  onValueChange: (Duration) -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  stepSeconds: Int = 1,
  valueLabel: (Duration) -> String = { it.toString() },
) {
  val min = valueRange.start.inWholeSeconds.toFloat()
  val max = valueRange.endInclusive.inWholeSeconds.toFloat()
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    SliderHeader(title = title, valueLabel = valueLabel(value), subtitle = subtitle)
    Spacer(Modifier.height(8.dp))
    Slider(
      value = value.inWholeSeconds.toFloat().coerceIn(min, max),
      onValueChange = { onValueChange(it.roundToInt().seconds) },
      valueRange = min..max,
      steps = sliderSteps(min, max, stepSeconds),
    )
  }
}

/**
 * A range slider that edits a min/max [Duration] pair, snapping to [stepSeconds] increments within [valueRange].
 */
@Composable
internal fun DurationRangeSliderSetting(
  title: String,
  min: Duration,
  max: Duration,
  valueRange: ClosedRange<Duration>,
  onValueChange: (min: Duration, max: Duration) -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  stepSeconds: Int = 5,
) {
  val rangeMin = valueRange.start.inWholeSeconds.toFloat()
  val rangeMax = valueRange.endInclusive.inWholeSeconds.toFloat()
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    SliderHeader(
      title = title,
      valueLabel = "$min – $max",
      subtitle = subtitle,
    )
    Spacer(Modifier.height(8.dp))
    RangeSlider(
      value = min.inWholeSeconds.toFloat().coerceIn(rangeMin, rangeMax)..max.inWholeSeconds.toFloat().coerceIn(
        rangeMin,
        rangeMax,
      ),
      onValueChange = { range ->
        val snappedStep = stepSeconds.coerceAtLeast(1)
        val newMin = (range.start / snappedStep).roundToInt() * snappedStep
        val newMax = (range.endInclusive / snappedStep).roundToInt() * snappedStep
        onValueChange(newMin.seconds, newMax.seconds)
      },
      valueRange = rangeMin..rangeMax,
      steps = sliderSteps(rangeMin, rangeMax, stepSeconds),
    )
  }
}

/**
 * A read-only preview row for a computed [ResumeRewindTier] — "Paused ≥ 1m" → "34s".
 */
@Composable
internal fun ResumeRewindPreviewRow(
  tier: ResumeRewindTier,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = 16.dp,
        vertical = 4.dp,
      ),
  ) {
    Text(
      text = stringResource(Res.string.setting_playback_resume_rewind_tier_title, tier.pauseThreshold.toString()),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.alignByBaseline(),
    )
    DottedLeader(
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier
        .weight(1f)
        .alignByBaseline()
        .padding(horizontal = 8.dp),
    )
    Text(
      text = if (tier.rewindAmount <= Duration.ZERO) {
        stringResource(Res.string.setting_playback_resume_rewind_none)
      } else {
        tier.rewindAmount.toString()
      },
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.alignByBaseline(),
    )
  }
}

/**
 * A horizontal dotted "leader" line (as in a table of contents) that reports its [FirstBaseline] at the line
 * itself, so placing it in a [Row] with [androidx.compose.foundation.layout.RowScope.alignByBaseline] lines
 * the dots up with the baseline of the surrounding text. Takes zero vertical space of its own.
 */
@Composable
private fun DottedLeader(
  color: Color,
  modifier: Modifier = Modifier,
) {
  Layout(
    modifier = modifier.drawBehind {
      drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 3.dp.toPx())),
      )
    },
  ) { _, constraints ->
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
    // A 1px-tall strip whose baseline sits at the drawn line, so it aligns to the surrounding text baseline
    // while taking effectively no vertical space.
    layout(width, 1, alignmentLines = mapOf(FirstBaseline to 1)) {}
  }
}

@Composable
private fun SliderHeader(
  title: String,
  valueLabel: String,
  subtitle: String?,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = valueLabel,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    }
    if (subtitle != null) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun sliderSteps(min: Float, max: Float, stepSeconds: Int): Int {
  val step = stepSeconds.coerceAtLeast(1)
  return (((max - min) / step).roundToInt() - 1).coerceAtLeast(0)
}
