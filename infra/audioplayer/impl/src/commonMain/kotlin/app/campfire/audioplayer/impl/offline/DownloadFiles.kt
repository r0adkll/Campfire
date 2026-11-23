// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

// The few file operations offline downloads need, over kotlinx-io's file system

internal fun Path.isFile(): Boolean = SystemFileSystem.metadataOrNull(this)?.isRegularFile == true

internal fun Path.isDirectory(): Boolean = SystemFileSystem.metadataOrNull(this)?.isDirectory == true

/** The file's size in bytes, or 0 when it doesn't exist. */
internal fun Path.length(): Long = SystemFileSystem.metadataOrNull(this)?.takeIf { it.isRegularFile }?.size ?: 0L

/** The directories directly inside this one; empty when it isn't a directory. */
internal fun Path.childDirectories(): List<Path> =
  if (isDirectory()) SystemFileSystem.list(this).filter { it.isDirectory() } else emptyList()

/** Moves this file onto [target] in one step, replacing whatever was there. */
internal fun Path.moveOnto(target: Path) {
  SystemFileSystem.atomicMove(this, target)
}

internal fun Path.deleteRecursively() {
  if (isDirectory()) SystemFileSystem.list(this).forEach { it.deleteRecursively() }
  SystemFileSystem.delete(this, mustExist = false)
}

/** Removes this directory if nothing is left in it. */
internal fun Path.deleteIfEmpty() {
  if (!isDirectory() || SystemFileSystem.list(this).isNotEmpty()) return
  try {
    SystemFileSystem.delete(this, mustExist = false)
  } catch (_: IOException) {
    // Something landed in it meanwhile
  }
}

// kotlinx-io's file metadata has no times

/** When the file was last modified, in epoch milliseconds, or -1 if that can't be read. */
internal expect fun Path.modifiedAtMs(): Long
