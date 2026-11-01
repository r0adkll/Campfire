// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.image

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class CoverUrlsTest {

  private val itemCover = "https://abs.example.com/api/items/li_123/cover"
  private val authorImage = "https://abs.example.com/api/authors/au_456/image"

  @Test
  fun recognizes_item_cover_and_author_image_urls() {
    assertThat(CoverUrls.isServerImageUrl(itemCover)).isTrue()
    assertThat(CoverUrls.isServerImageUrl(authorImage)).isTrue()
    assertThat(CoverUrls.isServerImageUrl("$itemCover?ts=1")).isTrue()
  }

  @Test
  fun ignores_unrelated_urls() {
    assertThat(CoverUrls.isServerImageUrl("https://abs.example.com/api/items/li_123")).isFalse()
    assertThat(CoverUrls.isServerImageUrl("https://abs.example.com/api/items/li_123/play")).isFalse()
    assertThat(CoverUrls.isServerImageUrl("https://cdn.example.com/cover.jpg")).isFalse()
    assertThat(CoverUrls.isServerImageUrl("https://abs.example.com/api/items/li_123/cover/extra")).isFalse()
  }

  @Test
  fun bucket_rounds_up_to_next_supported_width() {
    assertThat(CoverUrls.bucketWidth(1)).isEqualTo(200)
    assertThat(CoverUrls.bucketWidth(200)).isEqualTo(200)
    assertThat(CoverUrls.bucketWidth(201)).isEqualTo(300)
    assertThat(CoverUrls.bucketWidth(360)).isEqualTo(400)
    assertThat(CoverUrls.bucketWidth(1080)).isEqualTo(1200)
  }

  @Test
  fun bucket_caps_at_largest_width() {
    assertThat(CoverUrls.bucketWidth(4000)).isEqualTo(1600)
  }

  @Test
  fun bucket_uses_artwork_width_when_size_is_unknown() {
    assertThat(CoverUrls.bucketWidth(null)).isEqualTo(CoverUrls.ARTWORK_WIDTH)
    assertThat(CoverUrls.bucketWidth(0)).isEqualTo(CoverUrls.ARTWORK_WIDTH)
    assertThat(CoverUrls.bucketWidth(-5)).isEqualTo(CoverUrls.ARTWORK_WIDTH)
  }

  @Test
  fun sized_appends_width_to_bare_url() {
    assertThat(CoverUrls.sized(itemCover, 360)).isEqualTo("$itemCover?width=400")
  }

  @Test
  fun sized_appends_width_to_url_with_existing_query() {
    assertThat(CoverUrls.sized("$itemCover?ts=1700000000", 360))
      .isEqualTo("$itemCover?ts=1700000000&width=400")
  }

  @Test
  fun sized_preserves_fragment() {
    assertThat(CoverUrls.sized("$authorImage#frag", 200)).isEqualTo("$authorImage?width=200#frag")
  }

  @Test
  fun sized_leaves_urls_that_already_specify_a_size_alone() {
    assertThat(CoverUrls.sized("$itemCover?width=800", 200)).isEqualTo("$itemCover?width=800")
    assertThat(CoverUrls.sized("$itemCover?height=800", 200)).isEqualTo("$itemCover?height=800")
    assertThat(CoverUrls.sized("$itemCover?raw=1", 200)).isEqualTo("$itemCover?raw=1")
    assertThat(CoverUrls.sized("$itemCover?ts=1&width=800", 200)).isEqualTo("$itemCover?ts=1&width=800")
  }

  @Test
  fun sized_leaves_non_server_urls_alone() {
    val external = "https://cdn.example.com/cover.jpg"
    assertThat(CoverUrls.sized(external, 360)).isEqualTo(external)
  }

  @Test
  fun sized_uses_artwork_width_when_size_is_unknown() {
    assertThat(CoverUrls.sized(itemCover, null)).isEqualTo("$itemCover?width=${CoverUrls.ARTWORK_WIDTH}")
  }

  @Test
  fun itemId_reads_the_item_of_a_cover_url() {
    assertThat(CoverUrls.itemId(itemCover)).isEqualTo("li_123")
    assertThat(CoverUrls.itemId("$itemCover?ts=1&width=400")).isEqualTo("li_123")
    assertThat(CoverUrls.itemId(authorImage)).isNull()
    assertThat(CoverUrls.itemId("https://cdn.example.com/cover.jpg")).isNull()
  }

  @Test
  fun timestamp_reads_the_ts_param() {
    assertThat(CoverUrls.timestamp("$itemCover?ts=1700000000")).isEqualTo(1700000000L)
    assertThat(CoverUrls.timestamp("$itemCover?width=400&ts=42#frag")).isEqualTo(42L)
    assertThat(CoverUrls.timestamp(itemCover)).isNull()
    assertThat(CoverUrls.timestamp("$itemCover?ts=abc")).isNull()
  }

  @Test
  fun original_asks_for_the_unresized_server_image() {
    assertThat(CoverUrls.original(itemCover)).isEqualTo("$itemCover?raw=1")
    assertThat(CoverUrls.original("$itemCover?ts=42&width=400#frag")).isEqualTo("$itemCover?ts=42&raw=1#frag")
    assertThat(CoverUrls.original("https://cdn.example.com/cover.jpg")).isEqualTo("https://cdn.example.com/cover.jpg")
  }
}
