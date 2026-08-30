// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo

import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.store.CachedBookInfo
import app.campfire.bookinfo.store.isStale
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CachedBookInfoTest {

  private val now = 1_700_000_000_000L
  private val matchKey = "isbn:9780765393043;asin:"

  private val hitInfo = BookCommunityInfo(
    providerBookId = "386446",
    providerUrl = null,
    rating = 4.63,
    ratingsCount = 4109,
    ratingsDistribution = null,
    reviewsCount = 422,
    releaseDate = null,
    coverUrl = null,
  )

  private fun hit(fetchedAt: Long, key: String = matchKey) =
    CachedBookInfo(info = hitInfo, fetchedAt = fetchedAt, matchKey = key)

  private fun miss(fetchedAt: Long, key: String = matchKey) =
    CachedBookInfo(info = null, fetchedAt = fetchedAt, matchKey = key)

  @Test
  fun `fresh hits are not stale`() {
    assertThat(hit(now - 23.hours.inWholeMilliseconds).isStale(now, matchKey)).isFalse()
  }

  @Test
  fun `hits older than the ttl are stale`() {
    assertThat(hit(now - 25.hours.inWholeMilliseconds).isStale(now, matchKey)).isTrue()
  }

  @Test
  fun `misses expire on the short ttl`() {
    assertThat(miss(now - 30.minutes.inWholeMilliseconds).isStale(now, matchKey)).isFalse()
    assertThat(miss(now - 2.hours.inWholeMilliseconds).isStale(now, matchKey)).isTrue()
  }

  @Test
  fun `changed identifiers make even a fresh row stale`() {
    val fresh = hit(now, key = "isbn:;asin:B002RI9Z9E")

    assertThat(fresh.isStale(now, currentMatchKey = matchKey)).isTrue()
  }

  @Test
  fun `rows cached before match keys existed are stale`() {
    val legacy = CachedBookInfo(info = hitInfo, fetchedAt = now, matchKey = null)

    assertThat(legacy.isStale(now, currentMatchKey = matchKey)).isTrue()
  }
}
