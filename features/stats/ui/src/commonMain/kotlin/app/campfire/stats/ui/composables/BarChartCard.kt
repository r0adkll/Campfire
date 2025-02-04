package app.campfire.stats.ui.composables

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.campfire.common.compose.widgets.ItemImage

@Composable
internal fun BarChartCard(
  header: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  ElevatedCard(
    modifier = modifier
      .padding(
        horizontal = StatsDefaults.HorizontalPadding,
        vertical = StatsDefaults.VerticalPadding,
      ),
  ) {
    header()
    Column(
      verticalArrangement = verticalArrangement,
      content = content,
      modifier = Modifier
        .padding(
          horizontal = 16.dp,
          vertical = 8.dp,
        ),
    )
    Spacer(Modifier.height(8.dp))
  }
}

@Composable
internal fun ChartBar(
  imageUrl: String,
  title: String,
  trailingLabel: String,
  progress: Float,
  modifier: Modifier = Modifier,
) {
  BarBackground(
    modifier = modifier
      .height(DefaultBarHeight)
      .fillMaxWidth(),
  ) {
    BarForeground(
      modifier = Modifier
        .height(DefaultBarHeight)
        .defaultMinSize(minWidth = DefaultBarHeight)
        .fillMaxWidth(progress),
    ) {
      ItemImage(
        imageUrl = imageUrl,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(DefaultBarHeight)
          .clip(MaterialTheme.shapes.medium)
          .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium,
          )
          .zIndex(2f),
      )

      if (progress > 0.5f) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .weight(1f),
        )
      } else {
        Spacer(Modifier.weight(1f))
      }

      Text(
        text = trailingLabel,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
          .padding(end = 16.dp),
      )
    }

    if (progress <= .5f) {
      Text(
        text = title,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodyMedium,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier
          .align(Alignment.CenterVertically)
          .padding(horizontal = 16.dp)
          .weight(1f),
      )
    }
  }
}

@Composable
private fun BarBackground(
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit = {},
) {
  val color = MaterialTheme.colorScheme.surfaceContainerHighest
  val border = MaterialTheme.colorScheme.outlineVariant
  Row(
    modifier = modifier
      .drawBehind {
        drawRoundRect(
          color = color,
          cornerRadius = CornerRadius(12.dp.toPx()),
        )
        drawRoundRect(
          color = border,
          cornerRadius = CornerRadius(12.dp.toPx()),
          style = Stroke(
            width = 1.dp.toPx(),
          ),
        )
      },
    content = content,
  )
}

@Composable
private fun BarForeground(
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit = {},
) {
  val color = MaterialTheme.colorScheme.secondaryContainer
  val border = MaterialTheme.colorScheme.onSecondaryContainer
  Row(
    modifier = modifier
      .drawBehind {
        drawRoundRect(
          color = color,
          cornerRadius = CornerRadius(12.dp.toPx()),
        )
        drawRoundRect(
          color = border,
          cornerRadius = CornerRadius(12.dp.toPx()),
          style = Stroke(
            width = 1.dp.toPx(),
          ),
        )
      },
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}

internal val DefaultBarHeight = 48.dp
