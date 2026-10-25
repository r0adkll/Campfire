// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AutoAwesome
import app.campfire.common.compose.icons.rounded.DeleteForever
import app.campfire.common.compose.icons.rounded.EditSquare
import app.campfire.common.compose.icons.rounded.Refresh
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.common.compose.widgets.sheets.LocalSheetPresentation
import app.campfire.common.compose.widgets.sheets.SheetPresentation
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.ui.detail.manage.DeleteItemDialog
import app.campfire.libraries.ui.detail.manage.ItemAction
import app.campfire.libraries.ui.detail.manage.ItemActionsMenu
import app.campfire.libraries.ui.detail.manage.QuickMatchChoice
import app.campfire.libraries.ui.detail.manage.QuickMatchDialog
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.menu_item_delete_from_server
import campfire.features.libraries.ui.generated.resources.menu_item_edit_details
import campfire.features.libraries.ui.generated.resources.menu_item_quick_match
import campfire.features.libraries.ui.generated.resources.menu_item_rescan
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import org.jetbrains.compose.resources.stringResource

/** The action picked from an item's actions, or none when they were put away. */
private sealed interface ActionChoice {
  data object Dismissed : ActionChoice
  data class Picked(val action: ItemAction) : ActionChoice
}

/** Shows [actions] for [item] as a sheet, and returns the one picked. */
internal suspend fun OverlayHost.showItemActionsSheet(
  item: LibraryItem,
  actions: List<ItemAction>,
): ItemAction? {
  val choice = show(
    AdaptiveSheetOverlay<LibraryItem, ActionChoice>(
      model = item,
      onDismiss = { ActionChoice.Dismissed },
      skipPartiallyExpanded = true,
    ) { model, navigator ->
      Impression {
        ScreenViewEvent("ItemActions", ScreenType.Overlay)
      }

      ItemActionsSheet(
        item = model,
        actions = actions,
        onAction = { navigator.finish(ActionChoice.Picked(it)) },
      )
    },
  )
  return (choice as? ActionChoice.Picked)?.action
}

/** Shows [actions] as a menu at [pointer], a position in the window, and returns the one picked. */
internal suspend fun OverlayHost.showItemActionsMenu(
  actions: List<ItemAction>,
  pointer: Offset,
): ItemAction? {
  val choice = show(ItemActionsMenuOverlay(actions, pointer))
  return (choice as? ActionChoice.Picked)?.action
}

/**
 * Asks how to delete the item titled [itemTitle], and returns whether its files go too, or null
 * when the user backs out.
 */
internal suspend fun OverlayHost.showDeleteItemDialog(itemTitle: String): Boolean? {
  return when (val choice = show(DeleteItemOverlay(itemTitle))) {
    DeleteChoice.Cancel -> null
    is DeleteChoice.Delete -> choice.hardDelete
  }
}

/**
 * Asks whether to quick match the item titled [itemTitle] against [sourceName], and returns what
 * the match may replace, or null when the user backs out.
 */
internal suspend fun OverlayHost.showQuickMatchDialog(itemTitle: String, sourceName: String): QuickMatchChoice? {
  return when (val choice = show(QuickMatchOverlay(itemTitle, sourceName))) {
    QuickMatchAnswer.Cancel -> null
    is QuickMatchAnswer.Match -> choice.choice
  }
}

/** The item, then what can be done to it. */
@Composable
internal fun ItemActionsSheet(
  item: LibraryItem,
  actions: List<ItemAction>,
  onAction: (ItemAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isSideSheet = LocalSheetPresentation.current == SheetPresentation.Side
  Column(
    modifier = modifier
      .then(if (isSideSheet) Modifier.width(SideSheetWidth) else Modifier.fillMaxWidth())
      .padding(bottom = 16.dp),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
      ItemImage(
        imageUrl = item.media.coverImageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(56.dp)
          .clip(MaterialTheme.shapes.medium),
      )
      Column {
        Text(
          text = item.media.metadata.title.orEmpty(),
          style = MaterialTheme.typography.titleMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        val author = item.media.metadata.authorName ?: item.media.metadata.author
        if (author != null) {
          Text(
            text = author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    HorizontalDivider(Modifier.padding(vertical = 8.dp))

    actions.forEach { action ->
      ActionRow(action = action, onClick = { onAction(action) })
    }

    Spacer(Modifier.navigationBarsPadding())
  }
}

@Composable
private fun ActionRow(
  action: ItemAction,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val color = when (action) {
    ItemAction.Edit, ItemAction.QuickMatch, ItemAction.Rescan -> MaterialTheme.colorScheme.onSurface
    ItemAction.Delete -> MaterialTheme.colorScheme.error
  }
  ListItem(
    headlineContent = {
      Text(
        text = stringResource(
          when (action) {
            ItemAction.Edit -> Res.string.menu_item_edit_details
            ItemAction.QuickMatch -> Res.string.menu_item_quick_match
            ItemAction.Rescan -> Res.string.menu_item_rescan
            ItemAction.Delete -> Res.string.menu_item_delete_from_server
          },
        ),
        color = color,
      )
    },
    leadingContent = {
      Icon(
        when (action) {
          ItemAction.Edit -> CampfireIcons.Rounded.EditSquare
          ItemAction.QuickMatch -> CampfireIcons.Rounded.AutoAwesome
          ItemAction.Rescan -> CampfireIcons.Rounded.Refresh
          ItemAction.Delete -> CampfireIcons.Rounded.DeleteForever
        },
        contentDescription = null,
        tint = if (action == ItemAction.Delete) color else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = modifier
      .clickable(onClick = onClick)
      .padding(start = 8.dp)
      .testTag(
        when (action) {
          ItemAction.Edit -> "item_action_edit"
          ItemAction.QuickMatch -> "item_action_quick_match"
          ItemAction.Rescan -> "item_action_rescan"
          ItemAction.Delete -> "item_action_delete"
        },
      ),
  )
}

/** The menu a right click opens, anchored where the click was. */
private class ItemActionsMenuOverlay(
  private val actions: List<ItemAction>,
  private val pointer: Offset,
) : Overlay<ActionChoice> {

  @Composable
  override fun Content(navigator: OverlayNavigator<ActionChoice>) {
    // The pointer is in the window; the overlay may not start at the window's corner
    var origin by remember { mutableStateOf<Offset?>(null) }
    Box(
      Modifier
        .fillMaxSize()
        .onGloballyPositioned { origin = it.positionInWindow() },
    ) {
      origin?.let { origin ->
        Box(Modifier.offset { (pointer - origin).round() }) {
          ItemActionsMenu(
            expanded = true,
            actions = actions,
            onAction = { navigator.finish(ActionChoice.Picked(it)) },
            onDismissRequest = { navigator.finish(ActionChoice.Dismissed) },
          )
        }
      }
    }
  }
}

private sealed interface DeleteChoice {
  data object Cancel : DeleteChoice
  data class Delete(val hardDelete: Boolean) : DeleteChoice
}

private class DeleteItemOverlay(
  private val itemTitle: String,
) : Overlay<DeleteChoice> {

  @Composable
  override fun Content(navigator: OverlayNavigator<DeleteChoice>) {
    DeleteItemDialog(
      itemTitle = itemTitle,
      onConfirm = { navigator.finish(DeleteChoice.Delete(it)) },
      onDismissRequest = { navigator.finish(DeleteChoice.Cancel) },
    )
  }
}

private sealed interface QuickMatchAnswer {
  data object Cancel : QuickMatchAnswer
  data class Match(val choice: QuickMatchChoice) : QuickMatchAnswer
}

private class QuickMatchOverlay(
  private val itemTitle: String,
  private val sourceName: String,
) : Overlay<QuickMatchAnswer> {

  @Composable
  override fun Content(navigator: OverlayNavigator<QuickMatchAnswer>) {
    QuickMatchDialog(
      itemTitle = itemTitle,
      sourceName = sourceName,
      onConfirm = { navigator.finish(QuickMatchAnswer.Match(it)) },
      onDismissRequest = { navigator.finish(QuickMatchAnswer.Cancel) },
    )
  }
}

private val SideSheetWidth = 360.dp
