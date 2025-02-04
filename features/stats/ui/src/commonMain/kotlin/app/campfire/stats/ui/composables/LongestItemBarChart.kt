package app.campfire.stats.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.extensions.readoutAtMostHours
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.SquareFoot
import app.campfire.core.model.LongestItem
import app.campfire.stats.ui.StatsUiModel
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.card_longest_items_title
import campfire.features.stats.ui.generated.resources.totals_format
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LongestItemBarChart(
  model: StatsUiModel.LongestItems,
  onItemClick: (LongestItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  BarChartCard(
    header = {
      CardHeader(
        icon = {
          Icon(
            CampfireIcons.Rounded.SquareFoot,
            contentDescription = null,
          )
        },
        title = { Text(stringResource(Res.string.card_longest_items_title)) },
        trailing = {
          Text(stringResource(Res.string.totals_format, model.totalDuration.thresholdReadoutFormat()))
        },
      )
    },
    modifier = modifier,
  ) {
    model.longestItems.forEach { item ->
      val progress = item.duration / model.longestDuration
      ChartBar(
        imageUrl = item.coverImageUrl,
        title = item.title,
        trailingLabel = item.duration.readoutAtMostHours(),
        progress = progress.toFloat(),
        modifier = Modifier.clickable {
          onItemClick(item)
        },
      )
    }
  }
}
