// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

internal actual fun plainTextClipEntry(text: String, label: String): ClipEntry =
  ClipData.newPlainText(label, text).toClipEntry()
