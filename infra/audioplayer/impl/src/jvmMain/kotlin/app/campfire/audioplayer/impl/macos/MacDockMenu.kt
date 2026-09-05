// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.Taskbar

/**
 * Transport controls on the Dock icon's context menu. AWT owns the Dock menu, so labels are plain
 * strings here rather than Compose resources; they act on whichever player is current.
 */
internal class MacDockMenu(private val holder: AudioPlayerHolder) {

  fun install(): Boolean {
    if (!Taskbar.isTaskbarSupported()) return false
    val taskbar = Taskbar.getTaskbar()
    if (!taskbar.isSupported(Taskbar.Feature.MENU)) return false

    val menu = PopupMenu().apply {
      add(item("Play / Pause") { it.playPause() })
      add(item("Skip Back") { it.seekBackward() })
      add(item("Skip Forward") { it.seekForward() })
      addSeparator()
      add(item("Previous Chapter") { it.skipToPrevious() })
      add(item("Next Chapter") { it.skipToNext() })
    }
    taskbar.menu = menu
    return true
  }

  private fun item(label: String, action: (AudioPlayer) -> Unit) = MenuItem(label).apply {
    addActionListener { holder.currentPlayer.value?.let(action) }
  }
}
