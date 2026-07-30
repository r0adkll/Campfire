// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

inline fun <A, B, R> Result<A>.with(other: Result<B>, transform: (A, B) -> R): Result<R> {
  return if (this.isSuccess && other.isSuccess) {
    Result.success(transform(getOrThrow(), other.getOrThrow()))
  } else {
    Result.failure(FusedResultFailure(this, other))
  }
}

class FusedResultFailure(
  val first: Result<*>,
  val second: Result<*>,
) : Exception("FusedResultFailure[${first.exceptionOrNull()?.message}, ${second.exceptionOrNull()?.message}]")
