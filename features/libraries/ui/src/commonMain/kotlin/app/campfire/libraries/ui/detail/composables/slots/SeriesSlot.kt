// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.SeriesSequence
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.SeriesMetadata

data class SeriesWithBooks(
  val series: SeriesSequence,
  val books: List<LibraryItem>,
)

class SeriesSlot(
  private val libraryItem: LibraryItem,
  private val series: List<SeriesWithBooks>,
) : ContentSlot {

  override val id: String = "series"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Column(modifier) {
      MetadataHeader(
        title = "Series",
        textStyle = MaterialTheme.typography.titleLarge,
        textColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
          .heightIn(min = 56.dp)
          .padding(
            horizontal = 16.dp,
          ),
      )
      series.forEachIndexed { index, (seriesSequence, seriesBooks) ->
        Spacer(Modifier.height(if (index == 0) 8.dp else 16.dp))
        SeriesMetadata(
          seriesName = seriesSequence.name,
          seriesBooks = seriesBooks,
          modifier = Modifier
            .clickable(
              onClick = {
                eventSink(LibraryItemUiEvent.SeriesClick(libraryItem, seriesSequence))
              },
            )
            .padding(horizontal = 16.dp),
        )
      }
    }
  }
}
