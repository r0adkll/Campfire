// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.NativeClipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

/** Holds the last clip entry, readable back as plain text. */
internal class FakeClipboard : Clipboard {
  private var entry: ClipEntry? = null

  @OptIn(ExperimentalComposeUiApi::class)
  val text: String?
    get() = (entry?.nativeClipEntry as? Transferable)?.getTransferData(DataFlavor.stringFlavor) as? String

  override suspend fun getClipEntry(): ClipEntry? = entry

  override suspend fun setClipEntry(clipEntry: ClipEntry?) {
    entry = clipEntry
  }

  override val nativeClipboard: NativeClipboard
    get() = error("Not used")
}
