// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.message

import androidx.compose.material3.SnackbarHostState
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.episode_delete_failed
import campfire.common.compose.generated.resources.episode_delete_not_allowed
import campfire.common.compose.generated.resources.episode_deleted_file
import campfire.common.compose.generated.resources.episode_removed
import org.jetbrains.compose.resources.getString

/** Tells that the episode titled [episodeTitle] was deleted from the server, and its file if [hardDelete]. */
suspend fun SnackbarHostState.showEpisodeDeleted(episodeTitle: String, hardDelete: Boolean) {
  currentSnackbarData?.dismiss()
  showSnackbar(
    getString(if (hardDelete) Res.string.episode_deleted_file else Res.string.episode_removed, episodeTitle),
  )
}

/** Tells that an episode couldn't be deleted from the server, because the account isn't allowed to if [notAllowed]. */
suspend fun SnackbarHostState.showEpisodeNotDeleted(notAllowed: Boolean) {
  currentSnackbarData?.dismiss()
  showSnackbar(
    getString(if (notAllowed) Res.string.episode_delete_not_allowed else Res.string.episode_delete_failed),
  )
}
