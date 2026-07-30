// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

fun <T, R> Flow<T?>.mapIfNotNull(mapper: suspend (T) -> R): Flow<R?> = map {
  if (it != null) mapper(it) else null
}
fun <T> Flow<T?>.ifNull(mapper: suspend () -> T): Flow<T> = map {
  it ?: mapper()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<T?>.flatMapIfNotNull(mapper: suspend (T) -> Flow<R>): Flow<R?> = flatMapLatest {
  if (it != null) mapper(it) else flowOf(null)
}
