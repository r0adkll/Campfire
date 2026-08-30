// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import app.campfire.home.ui.mediaMetadata
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class BookMatchTest {

  @Test
  fun `all available identifiers are carried`() {
    val metadata = mediaMetadata(ISBN = "9780765393043", ASIN = "B002RI9Z9E")

    assertThat(metadata.bestMatch()).isEqualTo(
      BookMatch.Identifiers(isbn = "9780765393043", asin = "B002RI9Z9E"),
    )
  }

  @Test
  fun `a single identifier still matches`() {
    assertThat(mediaMetadata(ISBN = null, ASIN = "B002RI9Z9E").bestMatch())
      .isEqualTo(BookMatch.Identifiers(isbn = null, asin = "B002RI9Z9E"))
    assertThat(mediaMetadata(ISBN = "9780765393043", ASIN = null).bestMatch())
      .isEqualTo(BookMatch.Identifiers(isbn = "9780765393043", asin = null))
  }

  @Test
  fun `blank identifiers are dropped`() {
    val metadata = mediaMetadata(ISBN = "  ", ASIN = "B002RI9Z9E")

    assertThat(metadata.bestMatch()).isEqualTo(
      BookMatch.Identifiers(isbn = null, asin = "B002RI9Z9E"),
    )
  }

  @Test
  fun `title and author are used when identifiers are missing`() {
    val metadata = mediaMetadata(
      ISBN = null,
      ASIN = null,
      title = "The Way of Kings",
      authorName = "Brandon Sanderson",
    )

    assertThat(metadata.bestMatch()).isEqualTo(
      BookMatch.TitleAuthor(title = "The Way of Kings", author = "Brandon Sanderson"),
    )
  }

  @Test
  fun `no identifiers and no title yields no match`() {
    assertThat(mediaMetadata(ISBN = null, ASIN = null, title = null).bestMatch()).isNull()
  }

  @Test
  fun `cache key changes when identifiers change`() {
    val asinOnly = BookMatch.Identifiers(isbn = null, asin = "B002RI9Z9E")
    val withIsbn = BookMatch.Identifiers(isbn = "9780765393043", asin = "B002RI9Z9E")

    assertThat(asinOnly.cacheKey).isNotEqualTo(withIsbn.cacheKey)
    assertThat(asinOnly.cacheKey).isEqualTo(BookMatch.Identifiers(null, "B002RI9Z9E").cacheKey)
  }
}
