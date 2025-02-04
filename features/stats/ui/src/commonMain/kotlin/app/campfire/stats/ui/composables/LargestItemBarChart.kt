package app.campfire.stats.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.HardDisk
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.LargestItem
import app.campfire.stats.ui.StatsUiModel
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.card_largest_items_title
import campfire.features.stats.ui.generated.resources.totals_format
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LargestItemBarChart(
  model: StatsUiModel.LargestItems,
  onItemClick: (LargestItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  BarChartCard(
    header = {
      CardHeader(
        icon = {
          Icon(
            CampfireIcons.Rounded.HardDisk,
            contentDescription = null,
          )
        },
        title = { Text(stringResource(Res.string.card_largest_items_title)) },
        trailing = {
          Text(stringResource(Res.string.totals_format, model.totalSizeInBytes.asReadableBytes()))
        },
      )
    },
    modifier = modifier,
  ) {
    model.largestItems.forEach { item ->
      val progress = item.sizeInBytes.toFloat() / model.largestSizeInBytes.toFloat()
      ChartBar(
        imageUrl = item.coverImageUrl,
        title = item.title,
        trailingLabel = item.sizeInBytes.asReadableBytes(),
        progress = progress,
        modifier = Modifier.clickable {
          onItemClick(item)
        },
      )
    }
  }
}
