// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.SplitAttributionBar

class SplitAttributionSlot(
  private val leftLabel: @Composable () -> String,
  private val leftAttributions: List<String>,
  private val rightLabel: @Composable () -> String,
  private val rightAttributions: List<String>,
  private val onLeftAttributeClick: ((String) -> LibraryItemUiEvent)? = null,
  private val onRightAttributeClick: ((String) -> LibraryItemUiEvent)? = null,
) : ContentSlot {

  override val id: String = "split-attribution-slot"

  @Composable
  override fun Content(
    modifier: Modifier,
    eventSink: (LibraryItemUiEvent) -> Unit,
  ) {
    SplitAttributionBar(
      leftTitle = { Text(leftLabel()) },
      leftAttributions = leftAttributions,
      onLeftAttributeClick = { author ->
        onLeftAttributeClick?.invoke(author)?.let {
          eventSink(it)
        }
      },
      rightTitle = { Text(rightLabel()) },
      rightAttributions = rightAttributions,
      onRightAttributeClick = { narrator ->
        onRightAttributeClick?.invoke(narrator)?.let {
          eventSink(it)
        }
      },
    )
  }
}
