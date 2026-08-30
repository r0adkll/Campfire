// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import kotlin.time.Duration

/**
 * Result of a provider call. Distinguishes the auth and quota failures that
 * require different UX (re-link prompt, backoff to cache) from plain errors.
 */
sealed interface BookInfoResult<out T> {
  data class Success<T>(val data: T) : BookInfoResult<T>

  /** The provider has no record of the requested book or series. */
  data object NotFound : BookInfoResult<Nothing>

  /** The provider requires an account link and none exists for the current user. */
  data object NotLinked : BookInfoResult<Nothing>

  /** The stored credential was rejected; the user must re-link the provider. */
  data object TokenInvalid : BookInfoResult<Nothing>

  /** The provider's rate limit was hit; retry no sooner than [retryAfter]. */
  data class RateLimited(val retryAfter: Duration?) : BookInfoResult<Nothing>

  data class Failure(val cause: Throwable) : BookInfoResult<Nothing>
}

inline fun <T, R> BookInfoResult<T>.map(mapper: (T) -> R): BookInfoResult<R> = when (this) {
  is BookInfoResult.Success -> BookInfoResult.Success(mapper(data))
  is BookInfoResult.NotFound -> this
  is BookInfoResult.NotLinked -> this
  is BookInfoResult.TokenInvalid -> this
  is BookInfoResult.RateLimited -> this
  is BookInfoResult.Failure -> this
}

val <T> BookInfoResult<T>.dataOrNull: T? get() = (this as? BookInfoResult.Success<T>)?.data
