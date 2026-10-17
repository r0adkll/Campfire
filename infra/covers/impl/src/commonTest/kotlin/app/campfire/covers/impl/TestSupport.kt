// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.impl

import app.campfire.core.coroutines.DispatcherProvider
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory

val testDispatchers = DispatcherProvider(
  io = Dispatchers.Default,
  databaseWrite = Dispatchers.Default,
  databaseRead = Dispatchers.Default,
  computation = Dispatchers.Default,
  main = Dispatchers.Default,
)

fun tempCoverDirectory() = Path(SystemTemporaryDirectory, "offline-covers-test-${Random.nextLong()}")

fun Path.deleteRecursively() {
  if (!SystemFileSystem.exists(this)) return
  if (SystemFileSystem.metadataOrNull(this)?.isDirectory == true) {
    SystemFileSystem.list(this).forEach { it.deleteRecursively() }
  }
  SystemFileSystem.delete(this)
}
