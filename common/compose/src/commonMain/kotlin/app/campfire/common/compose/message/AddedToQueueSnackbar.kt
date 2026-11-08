// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.message

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.action_undo
import campfire.common.compose.generated.resources.episode_added_to_queue
import campfire.common.compose.generated.resources.episode_removed_from_queue
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Tells that an episode was added to the queue, with an Undo.
 *
 * @return true if Undo was tapped.
 */
suspend fun SnackbarHostState.showAddedToQueue(): Boolean = showQueueChange(Res.string.episode_added_to_queue)

/**
 * Tells that an episode was taken out of the queue, with an Undo.
 *
 * @return true if Undo was tapped.
 */
suspend fun SnackbarHostState.showRemovedFromQueue(): Boolean =
  showQueueChange(Res.string.episode_removed_from_queue)

/**
 * Replaces whatever the snackbar is showing, so changing several episodes in a row doesn't line up
 * a message for each.
 */
private suspend fun SnackbarHostState.showQueueChange(message: StringResource): Boolean {
  currentSnackbarData?.dismiss()
  return showSnackbar(
    message = getString(message),
    actionLabel = getString(Res.string.action_undo),
    duration = SnackbarDuration.Short,
  ) == SnackbarResult.ActionPerformed
}
