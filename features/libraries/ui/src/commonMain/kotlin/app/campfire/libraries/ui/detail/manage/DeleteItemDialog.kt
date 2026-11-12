// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.widgets.dialog.DeleteFromServerDialog
import app.campfire.common.compose.widgets.dialog.asDeleteQuestionSubject
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_action_hard
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_action_hard_again
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_action_remove
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_hard
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_hard_description
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_lists
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_progress
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_remove
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_remove_description
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks how to delete an item: from its library only, which a scan undoes, or its files too, which
 * nothing undoes and so takes a second press. Removing from the library is always the default.
 */
@Composable
internal fun DeleteItemDialog(
  itemTitle: String,
  onConfirm: (hardDelete: Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DeleteFromServerDialog(
    title = stringResource(Res.string.dialog_delete_item_title, itemTitle.asDeleteQuestionSubject()),
    removeLabel = stringResource(Res.string.dialog_delete_item_remove),
    removeDescription = stringResource(Res.string.dialog_delete_item_remove_description),
    hardDeleteLabel = stringResource(Res.string.dialog_delete_item_hard),
    hardDeleteDescription = stringResource(Res.string.dialog_delete_item_hard_description),
    consequences = listOf(
      stringResource(Res.string.dialog_delete_item_progress),
      stringResource(Res.string.dialog_delete_item_lists),
    ),
    removeAction = stringResource(Res.string.dialog_delete_item_action_remove),
    hardDeleteAction = stringResource(Res.string.dialog_delete_item_action_hard),
    hardDeleteAgainAction = stringResource(Res.string.dialog_delete_item_action_hard_again),
    onConfirm = onConfirm,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  )
}
