// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AutoAwesome
import app.campfire.common.compose.icons.rounded.DeleteForever
import app.campfire.common.compose.icons.rounded.EditSquare
import app.campfire.common.compose.icons.rounded.History
import app.campfire.common.compose.icons.rounded.PhotoAlbum
import app.campfire.common.compose.icons.rounded.Refresh
import app.campfire.common.compose.icons.rounded.Search
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.User
import app.campfire.management.api.screen.ItemEditorScreen
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.menu_item_change_cover
import campfire.features.libraries.ui.generated.resources.menu_item_delete_from_server
import campfire.features.libraries.ui.generated.resources.menu_item_edit_details
import campfire.features.libraries.ui.generated.resources.menu_item_match
import campfire.features.libraries.ui.generated.resources.menu_item_playback_history
import campfire.features.libraries.ui.generated.resources.menu_item_quick_match
import campfire.features.libraries.ui.generated.resources.menu_item_rescan
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Something that can be done with an item from its menu, whether the server or the app does it. */
enum class ItemAction {
  /** Change the item's details, such as its title and description. */
  Edit,

  /** Find the item in a source, like Audible, to fill in its details from. */
  Match,

  /** Choose a new cover for the item, or none. */
  ChangeCover,

  /** Have the server fill in the item's details from the first result its source finds. */
  QuickMatch,

  /** Scan the item's files again. */
  Rescan,

  /** Look back through what was done while listening to the item, and jump to a moment of it. */
  PlaybackHistory,

  /** Remove the item from its library, and maybe its files from the server. */
  Delete,
}

/**
 * The [ItemAction]s this account can take for [item], in the order a menu shows them. Those the
 * server carries out are offered only when it would take them from this account, so it doesn't
 * refuse an action the app offered.
 *
 * @param playbackHistoryEnabled whether playback history is kept, so there is any to look back on
 */
fun User.itemActions(
  item: LibraryItem,
  playbackHistoryEnabled: Boolean,
): List<ItemAction> = buildList {
  // Only a book's details can be edited so far
  if (canUpdateItems && item.media is Media.Book) {
    add(ItemAction.Edit)
    add(ItemAction.Match)
    add(ItemAction.ChangeCover)
    add(ItemAction.QuickMatch)
  }
  // The server can't scan an item that is a single file on its own
  if (canRescanItems && !item.isFile) add(ItemAction.Rescan)
  // A podcast's history is kept per episode, which the item's menu can't pick
  if (playbackHistoryEnabled && item.media is Media.Book) add(ItemAction.PlaybackHistory)
  if (canDeleteItems) add(ItemAction.Delete)
}

/** The part of the editor [this] opens on, for those that open it */
val ItemAction.editorSection: ItemEditorScreen.Section?
  get() = when (this) {
    ItemAction.Edit -> ItemEditorScreen.Section.Details
    ItemAction.Match -> ItemEditorScreen.Section.Match
    ItemAction.ChangeCover -> ItemEditorScreen.Section.Cover
    ItemAction.QuickMatch, ItemAction.Rescan, ItemAction.PlaybackHistory, ItemAction.Delete -> null
  }

internal val ItemAction.label: StringResource
  get() = when (this) {
    ItemAction.Edit -> Res.string.menu_item_edit_details
    ItemAction.Match -> Res.string.menu_item_match
    ItemAction.ChangeCover -> Res.string.menu_item_change_cover
    ItemAction.QuickMatch -> Res.string.menu_item_quick_match
    ItemAction.Rescan -> Res.string.menu_item_rescan
    ItemAction.PlaybackHistory -> Res.string.menu_item_playback_history
    ItemAction.Delete -> Res.string.menu_item_delete_from_server
  }

internal val ItemAction.icon: ImageVector
  get() = when (this) {
    ItemAction.Edit -> CampfireIcons.Rounded.EditSquare
    ItemAction.Match -> CampfireIcons.Rounded.Search
    ItemAction.ChangeCover -> CampfireIcons.Rounded.PhotoAlbum
    ItemAction.QuickMatch -> CampfireIcons.Rounded.AutoAwesome
    ItemAction.Rescan -> CampfireIcons.Rounded.Refresh
    ItemAction.PlaybackHistory -> CampfireIcons.Rounded.History
    ItemAction.Delete -> CampfireIcons.Rounded.DeleteForever
  }

internal val ItemAction.testTag: String
  get() = when (this) {
    ItemAction.Edit -> "item_action_edit"
    ItemAction.Match -> "item_action_match"
    ItemAction.ChangeCover -> "item_action_change_cover"
    ItemAction.QuickMatch -> "item_action_quick_match"
    ItemAction.Rescan -> "item_action_rescan"
    ItemAction.PlaybackHistory -> "item_action_playback_history"
    ItemAction.Delete -> "item_action_delete"
  }

/** A menu of [actions], with deleting set apart at the end in the error color. */
@Composable
internal fun ItemActionsMenu(
  expanded: Boolean,
  actions: List<ItemAction>,
  onAction: (ItemAction) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  /** Items to show before [actions], set apart from them by a divider */
  leading: (@Composable ColumnScope.() -> Unit)? = null,
) {
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    shape = MaterialTheme.shapes.medium,
    modifier = modifier,
  ) {
    if (leading != null) {
      leading()
      if (actions.isNotEmpty()) HorizontalDivider()
    }
    actions.forEachIndexed { index, action ->
      ItemActionMenuItem(
        action = action,
        onClick = { onAction(action) },
        divided = action == ItemAction.Delete && index > 0,
      )
    }
  }
}

@Composable
private fun ItemActionMenuItem(
  action: ItemAction,
  onClick: () -> Unit,
  divided: Boolean,
) {
  if (divided) HorizontalDivider()
  val color = if (action == ItemAction.Delete) MaterialTheme.colorScheme.error else Color.Unspecified
  DropdownMenuItem(
    text = { Text(text = stringResource(action.label), color = color) },
    leadingIcon = {
      Icon(
        imageVector = action.icon,
        contentDescription = null,
        tint = if (action == ItemAction.Delete) MaterialTheme.colorScheme.error else LocalContentColor.current,
      )
    },
    onClick = onClick,
    modifier = Modifier.testTag(action.testTag),
  )
}
