// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the open workspace, such as the item editor, wants the preview beside it put away for
 * now, as while it shows a comparison of its own and could use the room.
 */
@Stable
class WorkspacePreview {
  var isHidden by mutableStateOf(false)
    internal set
}

/** The preview beside the open workspace, which a fresh one stands in for where there's none */
val LocalWorkspacePreview = staticCompositionLocalOf { WorkspacePreview() }

/** Puts the preview beside the workspace away while [hidden], and back once this leaves */
@Composable
fun HideWorkspacePreview(hidden: Boolean) {
  val preview = LocalWorkspacePreview.current
  DisposableEffect(preview, hidden) {
    preview.isHidden = hidden
    onDispose { preview.isHidden = false }
  }
}
