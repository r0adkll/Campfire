package app.campfire.stats.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Person
import app.campfire.core.model.AuthorWithCount
import app.campfire.stats.ui.StatsUiModel
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.card_top_authors_title
import campfire.features.stats.ui.generated.resources.totals_format
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TopAuthorsBarChart(
  model: StatsUiModel.TopAuthors,
  onItemClick: (AuthorWithCount) -> Unit,
  modifier: Modifier = Modifier,
) {
  BarChartCard(
    header = {
      CardHeader(
        icon = {
          Icon(
            CampfireIcons.Rounded.Person,
            contentDescription = null,
          )
        },
        title = { Text(stringResource(Res.string.card_top_authors_title)) },
        trailing = {
          Text(stringResource(Res.string.totals_format, model.totalCount.toString()))
        },
      )
    },
    modifier = modifier,
  ) {
    model.authors.forEach { item ->
      val progress = item.count.toFloat() / model.largestCount.toFloat()
      ChartBar(
        imageUrl = item.imageUrl,
        title = item.name,
        trailingLabel = item.count.toString(),
        progress = progress,
        modifier = Modifier.clickable {
          onItemClick(item)
        },
      )
    }
  }
}
