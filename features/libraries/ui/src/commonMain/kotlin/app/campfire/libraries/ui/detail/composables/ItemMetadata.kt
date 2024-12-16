package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.InterpreterMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RealEstateAgent
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.core.model.LibraryItem
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ItemMetadata(
  item: LibraryItem,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
    ) {
      item.media.metadata.publishedYear?.let { year ->
        ItemDetailItem(
          icon = Icons.Rounded.CalendarMonth,
          text = year,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item.media.metadata.publisher
      ?.takeIf { it.isNotBlank() }
      ?.let { publisher ->
        ItemDetailItem(
          icon = Icons.Rounded.RealEstateAgent,
          text = publisher,
        )
      }
  }
}
