// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.core.model.Media
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.book_details_isbn
import campfire.features.libraries.ui.generated.resources.book_details_language
import campfire.features.libraries.ui.generated.resources.book_details_published
import campfire.features.libraries.ui.generated.resources.book_details_publisher
import campfire.features.libraries.ui.generated.resources.book_details_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The facts about an edition a reader looks for, that an audiobook's length and narrators stand in for. */
class BookDetailsSlot(
  @get:VisibleForTesting val rows: List<Pair<StringResource, String>>,
) : ContentSlot {

  override val id: String = "book_details"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Column(
      modifier = modifier.padding(horizontal = 16.dp),
    ) {
      MetadataHeader(
        title = stringResource(Res.string.book_details_title),
      )
      SelectionContainer {
        Column {
          rows.forEach { (label, value) ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            ) {
              Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(Modifier.width(16.dp))
              Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }
  }

  companion object {

    /** [metadata]'s details, or null when it has none to show. */
    fun from(metadata: Media.Metadata): BookDetailsSlot? = listOfNotNull(
      metadata.language?.takeIf { it.isNotBlank() }?.let { Res.string.book_details_language to it },
      metadata.ISBN?.takeIf { it.isNotBlank() }?.let { Res.string.book_details_isbn to it },
      metadata.publisher?.takeIf { it.isNotBlank() }?.let { Res.string.book_details_publisher to it },
      (metadata.publishedDate ?: metadata.publishedYear)
        ?.takeIf { it.isNotBlank() }
        ?.let { Res.string.book_details_published to it },
    ).takeIf { it.isNotEmpty() }?.let(::BookDetailsSlot)
  }
}
