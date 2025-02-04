package app.campfire.stats.ui.composables

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.core.model.LargestItem
import app.campfire.stats.ui.StatsUiModel
import kotlinx.collections.immutable.persistentListOf

@Preview
@Composable
fun LargestItemBarChartPreview() {
  PreviewScaffold {
    LargestItemBarChart(
      model = StatsUiModel.LargestItems(
        largestSizeInBytes = 50L * 1024L * 1024L,
        totalSizeInBytes = 3L * 1024L * 1024L * 1024L * 1024L,
        largestItems = persistentListOf(
          LargestItem(
            id = "1",
            title = "Not till all are lost",
            coverImageUrl = "",
            sizeInBytes = 50L * 1024L * 1024L,
          ),
          LargestItem(
            id = "2",
            title = "Dune",
            coverImageUrl = "",
            sizeInBytes = 42L * 1024L * 1024L,
          ),
          LargestItem(
            id = "3",
            title = "The Way of Kings",
            coverImageUrl = "",
            sizeInBytes = 20L * 1024L * 1024L,
          ),
          LargestItem(
            id = "4",
            title = "Onyx Storm",
            coverImageUrl = "",
            sizeInBytes = 5L * 1024L * 1024L,
          ),
        ),
      ),
      onItemClick = {},
      modifier = Modifier.padding(horizontal = 16.dp),
    )
  }
}
