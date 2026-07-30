// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import org.mobilenativefoundation.store.store5.FetcherResult

fun <T : Any> Result<T>.asFetcherResult(): FetcherResult<T> {
  return when {
    isSuccess -> FetcherResult.Data(getOrThrow(), "api")
    else -> FetcherResult.Error.Exception(exceptionOrNull()!!)
  }
}
