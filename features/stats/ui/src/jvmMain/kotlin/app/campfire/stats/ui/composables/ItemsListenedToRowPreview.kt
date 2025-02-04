package app.campfire.stats.ui.composables

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.core.model.ItemListenedTo
import app.campfire.core.model.Media
import app.campfire.stats.ui.StatsUiModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.persistentListOf

@Preview
@Composable
fun ItemsListenedToRowPreview() {
  PreviewScaffold {
    ItemsListenedToRow(
      itemsListenedTo = StatsUiModel.ItemsListenedTo(
        items = persistentListOf(
          createItemListenedTo(
            id = "1",
            timeListening = 5.seconds,
            coverImageUrl = "",
          ),
          createItemListenedTo(
            id = "2",
            timeListening = 120.minutes + 42.seconds,
            coverImageUrl = "",
          ),
          createItemListenedTo(
            id = "3",
            timeListening = 50.hours + 23.minutes,
            coverImageUrl = "",
          ),
          createItemListenedTo(
            id = "4",
            timeListening = 20.days,
            coverImageUrl = "",
          ),
        ),
      ),
      onItemClick = { },
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp),
    )
  }
}

private fun createItemListenedTo(
  id: String,
  timeListening: Duration,
  coverImageUrl: String,
): ItemListenedTo = ItemListenedTo(
  id = id,
  timeListening = timeListening,
  coverImageUrl = coverImageUrl,
  mediaMetadata = Media.Metadata(
    title = null,
    titleIgnorePrefix = null,
    subtitle = null,
    authorName = null,
    authorNameLastFirst = null,
    narratorName = null,
    seriesName = null,
    seriesSequence = null,
    genres = emptyList(),
    publishedYear = null,
    publishedDate = null,
    publisher = null,
    description = null,
    ISBN = null,
    ASIN = null,
    language = null,
    isExplicit = false,
    isAbridged = false,
  ),
)
