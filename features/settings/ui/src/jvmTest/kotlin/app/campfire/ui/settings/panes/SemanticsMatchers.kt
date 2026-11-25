// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

/** The node offers a long-press action that accessibility services announce as [label]. */
internal fun hasLongClickLabel(label: String) = SemanticsMatcher("OnLongClick labelled '$label'") {
  it.config.getOrNull(SemanticsActions.OnLongClick)?.label == label
}
