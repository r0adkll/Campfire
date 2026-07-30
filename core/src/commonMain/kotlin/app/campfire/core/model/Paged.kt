// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

class Paged<Data>(
  val page: Int,
  val limit: Int,
  val total: Int,
  val data: List<Data>,
)

inline fun <T, R> Paged<T>.map(transform: (T) -> R): Paged<R> {
  return Paged(
    page = page,
    limit = limit,
    total = total,
    data = data.map(transform),
  )
}
