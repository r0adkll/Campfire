// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

open class DispatcherProvider(
  val io: CoroutineDispatcher,
  val databaseWrite: CoroutineDispatcher,
  val databaseRead: CoroutineDispatcher,
  val computation: CoroutineDispatcher,
  val main: CoroutineDispatcher,
)
