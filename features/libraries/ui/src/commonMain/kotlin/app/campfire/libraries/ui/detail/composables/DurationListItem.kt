package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.clockFormat
import app.campfire.common.compose.extensions.thenIf
import app.campfire.core.extensions.fluentIf
import kotlin.time.Duration

private val ListItemHeight = 48.dp
private val IndicatorSize = 8.dp
private val IndicatorPadding = 0.dp
private val ProgressCornerRadius = 24.dp
private val ProgressHeight = 8.dp

@Composable
internal fun DurationListItem(
  title: String,
  duration: Duration,
  modifier: Modifier = Modifier,
  progress: Float = 0f,
  progressColor: Color = MaterialTheme.colorScheme.secondary,
  trackColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.50f),
  selectedColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
  val isActiveChapter = progress > 0f && progress < 1f

  Row(
    modifier = modifier
      .thenIf(isActiveChapter) {
        background(selectedColor)
      }
      .defaultMinSize(minHeight = ListItemHeight)
      .fillMaxWidth()
      .fluentIf(isActiveChapter) {
        drawBehind {
          val width = size.width * progress
          val trackWidth = size.width * (1 - progress)
          val y = size.height - ProgressHeight.toPx()
          val cornerRadius = CornerRadius(ProgressCornerRadius.toPx())

          drawRoundRect(
            color = trackColor,
            topLeft = Offset(width - 4.dp.toPx(), y),
            size = Size(trackWidth + ProgressCornerRadius.toPx(), ProgressHeight.toPx()),
            cornerRadius = cornerRadius,
          )

          drawRoundRect(
            color = progressColor,
            topLeft = Offset(-ProgressCornerRadius.toPx(), y),
            size = Size(width + ProgressCornerRadius.toPx(), ProgressHeight.toPx()),
            cornerRadius = cornerRadius,
          )

//          drawRoundRect(
//            color = selectedColor,
//            topLeft = Offset(-IndicatorSize.toPx(), IndicatorPadding.toPx()),
//            size = Size(IndicatorSize.toPx() * 2f, size.height - (IndicatorPadding * 2).toPx()),
//            cornerRadius = CornerRadius(IndicatorSize.toPx()),
//          )
        }
      }
      .padding(
        horizontal = 16.dp,
        vertical = 12.dp,
      )
      .thenIf(isActiveChapter) {
        padding(bottom = ProgressHeight)
      },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = if (isActiveChapter) FontWeight.Bold else null,
      modifier = Modifier.weight(1f),
    )
    Spacer(Modifier.width(16.dp))
    Text(
      text = duration.clockFormat(),
      style = MaterialTheme.typography.labelLarge,
      fontFamily = FontFamily.Monospace,
      fontWeight = if (isActiveChapter) FontWeight.Bold else null,
    )
  }
}
