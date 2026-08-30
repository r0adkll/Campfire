// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover

import app.campfire.bookinfo.hardcover.graphql.parseCoverUrl
import app.campfire.bookinfo.hardcover.graphql.parseRatingsDistribution
import app.campfire.bookinfo.hardcover.graphql.parseRetryAfter
import app.campfire.bookinfo.hardcover.graphql.parseUsername
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

class HardcoverParsersTest {

  @Test
  fun `retry after header is used directly`() {
    val headers = headersOf("Retry-After", "30")

    assertThat(parseRetryAfter(headers)).isEqualTo(30.seconds)
  }

  @Test
  fun `ietf ratelimit header reset is parsed`() {
    val headers = headersOf("RateLimit", "limit=60, remaining=0, reset=25")

    assertThat(parseRetryAfter(headers)).isEqualTo(25.seconds)
  }

  @Test
  fun `legacy epoch reset header is converted to a delay`() {
    val headers = headersOf("X-RateLimit-Reset", "1700000060")

    assertThat(parseRetryAfter(headers, nowEpochSeconds = 1_700_000_000)).isEqualTo(60.seconds)
  }

  @Test
  fun `legacy relative reset header is used as seconds`() {
    val headers = headersOf("X-RateLimit-Reset", "45")

    assertThat(parseRetryAfter(headers, nowEpochSeconds = 1_700_000_000)).isEqualTo(45.seconds)
  }

  @Test
  fun `no known headers yields null`() {
    assertThat(parseRetryAfter(headersOf())).isNull()
  }

  @Test
  fun `ratings distribution object form is parsed`() {
    val element = Json.parseToJsonElement("""{"1": 4, "2": 10, "5": 900}""")

    assertThat(parseRatingsDistribution(element)).isEqualTo(mapOf(1 to 4, 2 to 10, 5 to 900))
  }

  @Test
  fun `ratings distribution array form is parsed`() {
    val element = Json.parseToJsonElement("""[{"rating": 1, "count": 4}, {"rating": 5, "count": 900}]""")

    assertThat(parseRatingsDistribution(element)).isEqualTo(mapOf(1 to 4, 5 to 900))
  }

  @Test
  fun `unknown ratings distribution shape yields null`() {
    assertThat(parseRatingsDistribution(Json.parseToJsonElement(""""oops""""))).isNull()
    assertThat(parseRatingsDistribution(null)).isNull()
  }

  @Test
  fun `cover url is read from cached image blob`() {
    val element = Json.parseToJsonElement("""{"url": "https://assets.hardcover.app/cover.jpg", "color": "#fff"}""")

    assertThat(parseCoverUrl(element)).isEqualTo("https://assets.hardcover.app/cover.jpg")
  }

  @Test
  fun `username is parsed from object and array forms`() {
    assertThat(parseUsername(Json.parseToJsonElement("""{"username": "reader"}"""))).isEqualTo("reader")
    assertThat(parseUsername(Json.parseToJsonElement("""[{"username": "reader"}]"""))).isEqualTo("reader")
    assertThat(parseUsername(null)).isNull()
  }
}
