// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ExpandAllSemiBold
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.DefocusedChapterAlpha
import app.campfire.libraries.ui.detail.composables.DefocusedChapterTextAlpha
import app.campfire.libraries.ui.detail.composables.ListItemHeight

class CollapsedChapterSlot(
  private val numOfCollapsedChapters: Int,
) : ContentSlot {

  override val id: String = "collapsed_chapters"
  override val contentType = ContentSlot.ContentType.Chapter

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Column(
      modifier = modifier
        .background(ChapterContainerColor),
    ) {
      Surface(
        modifier = Modifier
          .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(DefocusedChapterAlpha),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(4.dp),
        onClick = {
          eventSink(LibraryItemUiEvent.ExpandChaptersClick)
        },
      ) {
        Row(
          modifier = Modifier
            .defaultMinSize(minHeight = ListItemHeight)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "$numOfCollapsedChapters listened chapters",
            style = MaterialTheme.typography.labelLargeEmphasized,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
              .copy(DefocusedChapterTextAlpha),
            modifier = Modifier.weight(1f),
          )

          Spacer(Modifier.width(16.dp))

          Icon(
            CampfireIcons.Rounded.ExpandAllSemiBold,
            contentDescription = "Expand collapsed chapters",
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      }

      Spacer(Modifier.height(2.dp))
    }
  }
}
