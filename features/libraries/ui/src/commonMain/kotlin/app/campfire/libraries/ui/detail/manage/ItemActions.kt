// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AutoAwesome
import app.campfire.common.compose.icons.rounded.DeleteForever
import app.campfire.common.compose.icons.rounded.EditSquare
import app.campfire.common.compose.icons.rounded.Refresh
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.User
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.menu_item_delete_from_server
import campfire.features.libraries.ui.generated.resources.menu_item_edit_details
import campfire.features.libraries.ui.generated.resources.menu_item_quick_match
import campfire.features.libraries.ui.generated.resources.menu_item_rescan
import org.jetbrains.compose.resources.stringResource

/** Something an account can do to an item on the server, rather than to its own listening. */
enum class ItemAction {
  /** Change the item's details, such as its title and description. */
  Edit,

  /** Have the server fill in the item's details from the first result its source finds. */
  QuickMatch,

  /** Scan the item's files again. */
  Rescan,

  /** Remove the item from its library, and maybe its files from the server. */
  Delete,
}

/**
 * The [ItemAction]s the server would take from this account for [item], in the order a menu shows
 * them. Offering only these keeps the server from refusing an action the app offered.
 */
fun User.itemActions(item: LibraryItem): List<ItemAction> = buildList {
  // Only a book's details can be edited so far
  if (canUpdateItems && item.media is Media.Book) {
    add(ItemAction.Edit)
    add(ItemAction.QuickMatch)
  }
  // The server can't scan an item that is a single file on its own
  if (canRescanItems && !item.isFile) add(ItemAction.Rescan)
  if (canDeleteItems) add(ItemAction.Delete)
}

/** A menu of [actions], with deleting set apart at the end in the error color. */
@Composable
internal fun ItemActionsMenu(
  expanded: Boolean,
  actions: List<ItemAction>,
  onAction: (ItemAction) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    shape = MaterialTheme.shapes.medium,
    modifier = modifier,
  ) {
    actions.forEachIndexed { index, action ->
      when (action) {
        ItemAction.Edit -> DropdownMenuItem(
          text = { Text(stringResource(Res.string.menu_item_edit_details)) },
          leadingIcon = { Icon(CampfireIcons.Rounded.EditSquare, contentDescription = null) },
          onClick = { onAction(action) },
          modifier = Modifier.testTag("item_action_edit"),
        )

        ItemAction.QuickMatch -> DropdownMenuItem(
          text = { Text(stringResource(Res.string.menu_item_quick_match)) },
          leadingIcon = { Icon(CampfireIcons.Rounded.AutoAwesome, contentDescription = null) },
          onClick = { onAction(action) },
          modifier = Modifier.testTag("item_action_quick_match"),
        )

        ItemAction.Rescan -> DropdownMenuItem(
          text = { Text(stringResource(Res.string.menu_item_rescan)) },
          leadingIcon = { Icon(CampfireIcons.Rounded.Refresh, contentDescription = null) },
          onClick = { onAction(action) },
          modifier = Modifier.testTag("item_action_rescan"),
        )

        ItemAction.Delete -> {
          if (index > 0) HorizontalDivider()
          DropdownMenuItem(
            text = {
              Text(
                text = stringResource(Res.string.menu_item_delete_from_server),
                color = MaterialTheme.colorScheme.error,
              )
            },
            leadingIcon = {
              Icon(
                CampfireIcons.Rounded.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
              )
            },
            onClick = { onAction(action) },
            modifier = Modifier.testTag("item_action_delete"),
          )
        }
      }
    }
  }
}
