// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.DragIndicator
import app.campfire.common.compose.icons.rounded.Refresh
import app.campfire.home.api.model.HomeLayoutShelf
import app.campfire.home.api.model.ShelfIds
import app.campfire.ui.settings.SettingsUiEvent.HomeLayoutSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.home.hiddenShelves
import app.campfire.ui.settings.home.shownShelves
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.home_layout_description
import campfire.features.settings.ui.generated.resources.home_layout_drag_handle_description
import campfire.features.settings.ui.generated.resources.home_layout_header_hidden
import campfire.features.settings.ui.generated.resources.home_layout_header_shown
import campfire.features.settings.ui.generated.resources.home_layout_library
import campfire.features.settings.ui.generated.resources.home_layout_move_down
import campfire.features.settings.ui.generated.resources.home_layout_move_up
import campfire.features.settings.ui.generated.resources.home_layout_reset
import campfire.features.settings.ui.generated.resources.home_layout_reset_description
import campfire.features.settings.ui.generated.resources.home_layout_shelf_upcoming
import campfire.features.settings.ui.generated.resources.home_layout_shown_empty
import campfire.features.settings.ui.generated.resources.home_layout_unavailable
import campfire.features.settings.ui.generated.resources.setting_home_title
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableColumn

@Composable
internal fun HomeLayoutPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val settings = state.homeLayoutSettings
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_home_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    settings.libraryName?.let { name ->
      Text(
        text = stringResource(Res.string.home_layout_library, name),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      )
    }

    Text(
      text = stringResource(Res.string.home_layout_description),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 16.dp),
    )

    Header(
      title = { Text(stringResource(Res.string.home_layout_header_shown)) },
    )

    val shown = settings.shelves.shownShelves
    if (shown.isEmpty()) {
      Text(
        text = stringResource(Res.string.home_layout_shown_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      )
    } else {
      ShownShelves(
        shelves = shown,
        onMove = { from, to -> state.eventSink(HomeLayoutSettingEvent.MoveShown(from, to)) },
        onMoveBy = { shelf, offset -> state.eventSink(HomeLayoutSettingEvent.MoveShownBy(shelf.id, offset)) },
        onVisibilityChange = { shelf, visible ->
          state.eventSink(HomeLayoutSettingEvent.SetShelfVisible(shelf.id, visible))
        },
      )
    }

    val hidden = settings.shelves.hiddenShelves
    if (hidden.isNotEmpty()) {
      Header(
        title = { Text(stringResource(Res.string.home_layout_header_hidden)) },
      )

      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
      ) {
        hidden.forEachIndexed { index, shelf ->
          ShelfRow(
            shelf = shelf,
            shape = rowShape(index = index, total = hidden.size),
            onVisibilityChange = { visible ->
              state.eventSink(HomeLayoutSettingEvent.SetShelfVisible(shelf.id, visible))
            },
          )
        }
      }
    }

    if (settings.isCustomized) {
      Spacer(Modifier.height(8.dp))
      ActionSetting(
        headlineContent = { Text(stringResource(Res.string.home_layout_reset)) },
        supportingContent = { Text(stringResource(Res.string.home_layout_reset_description)) },
        leadingContent = { Icon(CampfireIcons.Rounded.Refresh, contentDescription = null) },
        onClick = { state.eventSink(HomeLayoutSettingEvent.Reset) },
      )
    }

    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun ShownShelves(
  shelves: List<HomeLayoutShelf>,
  onMove: (from: Int, to: Int) -> Unit,
  onMoveBy: (HomeLayoutShelf, offset: Int) -> Unit,
  onVisibilityChange: (HomeLayoutShelf, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val moveUpLabel = stringResource(Res.string.home_layout_move_up)
  val moveDownLabel = stringResource(Res.string.home_layout_move_down)
  ReorderableColumn(
    list = shelves,
    onSettle = onMove,
    onMove = {
      haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    },
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier.padding(horizontal = 16.dp),
  ) { index, shelf, _ ->
    val interactionSource = remember { MutableInteractionSource() }
    ReorderableItem {
      ShelfRow(
        shelf = shelf,
        shape = rowShape(index = index, total = shelves.size),
        onVisibilityChange = { onVisibilityChange(shelf, it) },
        dragHandleModifier = Modifier.draggableHandle(
          interactionSource = interactionSource,
          onDragStarted = {
            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
          },
          onDragStopped = {
            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
          },
        ),
        interactionSource = interactionSource,
        modifier = Modifier.semantics {
          customActions = buildList {
            if (index > 0) {
              add(CustomAccessibilityAction(moveUpLabel) { onMoveBy(shelf, -1); true })
            }
            if (index < shelves.lastIndex) {
              add(CustomAccessibilityAction(moveDownLabel) { onMoveBy(shelf, 1); true })
            }
          }
        },
      )
    }
  }
}

/**
 * @param dragHandleModifier set for a reorderable (shown) shelf, which then gets a drag handle
 */
@Composable
private fun ShelfRow(
  shelf: HomeLayoutShelf,
  shape: Shape,
  onVisibilityChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  dragHandleModifier: Modifier? = null,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  ElevatedCard(
    enabled = shelf.visible,
    onClick = { },
    shape = shape,
    colors = CardDefaults.elevatedCardColors(
      disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    modifier = modifier,
    interactionSource = interactionSource,
  ) {
    ListItem(
      headlineContent = { Text(shelf.displayLabel()) },
      supportingContent = if (shelf.isAvailable) {
        null
      } else {
        { Text(stringResource(Res.string.home_layout_unavailable)) }
      },
      leadingContent = dragHandleModifier?.let {
        {
          val description = stringResource(Res.string.home_layout_drag_handle_description)
          Icon(
            imageVector = CampfireIcons.Rounded.DragIndicator,
            contentDescription = description,
            modifier = dragHandleModifier.semantics { contentDescription = description },
          )
        }
      },
      trailingContent = {
        Switch(
          checked = shelf.visible,
          onCheckedChange = onVisibilityChange,
        )
      },
      colors = ListItemDefaults.colors(
        headlineColor = LocalContentColor.current,
        containerColor = Color.Transparent,
      ),
    )
  }
}

@Composable
private fun HomeLayoutShelf.displayLabel(): String = when {
  id == ShelfIds.UpcomingReleases -> stringResource(Res.string.home_layout_shelf_upcoming)
  label.isNotBlank() -> label
  else -> id
}
