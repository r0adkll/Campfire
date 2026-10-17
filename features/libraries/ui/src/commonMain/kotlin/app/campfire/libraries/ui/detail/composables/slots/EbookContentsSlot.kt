// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.ChapterListItem
import app.campfire.reader.api.EbookContentsEntry
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.header_contents
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/** The heading over an ebook's contents, styled as the chapters of an audiobook are. */
class EbookContentsHeaderSlot : ContentSlot {

  override val id: String = "ebook_contents_header"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.extraLarge.copy(
        bottomStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
      ),
      color = ChapterContainerColor,
    ) {
      Column {
        Spacer(Modifier.height(8.dp))
        MetadataHeader(
          title = stringResource(Res.string.header_contents),
          textStyle = MaterialTheme.typography.titleLarge,
          textColor = MaterialTheme.colorScheme.contentColorFor(ChapterContainerColor),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
      }
    }
  }
}

/**
 * One entry of an ebook's contents, which opens the reader there. Like an audiobook's chapters, the
 * entry being read is highlighted with how far into it the reader is, and those before it fade.
 */
class EbookContentsEntrySlot(
  @get:VisibleForTesting val entry: EbookContentsEntry,
  private val index: Int,
  private val isFirst: Boolean,
  private val isLast: Boolean,
  @get:VisibleForTesting val state: State,
  /** How far through this entry the reader is, when it's the [State.Current] one. */
  @get:VisibleForTesting val progressInEntry: Float = 0f,
) : ContentSlot {

  enum class State { Read, Current, Unread }

  override val id: String = "ebook_contents_$index"
  override val contentType = ContentSlot.ContentType.Chapter

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    val topCornerSize = if (isFirst) CornerSize(20.dp) else CornerSize(4.dp)
    val bottomCornerSize = if (isLast) CornerSize(20.dp) else CornerSize(4.dp)
    Column(
      modifier = modifier.background(ChapterContainerColor),
    ) {
      Surface(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(0.dp),
        shape = RoundedCornerShape(
          topStart = topCornerSize,
          topEnd = topCornerSize,
          bottomStart = bottomCornerSize,
          bottomEnd = bottomCornerSize,
        ),
      ) {
        ChapterListItem(
          title = entry.label,
          trailing = entry.fraction?.let { "${(it * 100).roundToInt()}%" }.orEmpty(),
          // The row only highlights strictly inside (0, 1), so the entry being read always does
          progress = when (state) {
            State.Read -> 1f
            State.Current -> progressInEntry.coerceIn(0.01f, 0.99f)
            State.Unread -> 0f
          },
          startPadding = 16.dp + (entry.depth * 16).dp,
          modifier = Modifier
            .clickable { eventSink(LibraryItemUiEvent.ReadAt(entry.href)) }
            .testTag("ebook_contents_entry"),
        )
      }

      Spacer(Modifier.height(if (isLast) 32.dp else 2.dp))
    }
  }
}
