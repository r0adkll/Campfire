// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.graphql

import kotlin.time.Duration

sealed interface HardcoverResult<out T> {
  data class Success<T>(val data: T) : HardcoverResult<T>
  data object NotLinked : HardcoverResult<Nothing>
  data object TokenInvalid : HardcoverResult<Nothing>
  data class RateLimited(val retryAfter: Duration?) : HardcoverResult<Nothing>
  data class GraphQlErrors(val messages: List<String>) : HardcoverResult<Nothing>
  data class NetworkFailure(val cause: Throwable) : HardcoverResult<Nothing>
}

class HardcoverGraphQlException(messages: List<String>) :
  Exception("Hardcover GraphQL error: ${messages.joinToString()}")

class HardcoverHttpException(val status: Int) :
  Exception("Hardcover HTTP $status")
