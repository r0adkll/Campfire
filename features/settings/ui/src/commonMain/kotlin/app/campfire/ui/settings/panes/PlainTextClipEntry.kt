// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.ui.platform.ClipEntry

/** A plain-text clip of [text]; [label] names it where the platform keeps one (Android). */
internal expect fun plainTextClipEntry(text: String, label: String): ClipEntry
