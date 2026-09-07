// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import app.campfire.core.image.CoverUrls
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import coil3.Canvas
import coil3.Image
import coil3.memory.MemoryCache
import kotlin.test.Test

class ImageRequestsTest {

  private val cover = "https://abs.example.com/api/items/li_1/cover?ts=5"

  @Test
  fun finds_the_rendition_a_smaller_slot_already_cached() {
    val cache = cacheOf(800)
    assertThat(cache.smallerCachedRendition(cover, 1200))
      .isEqualTo(MemoryCache.Key("$cover&width=800"))
  }

  /**
   * The whole mechanism is a silent no-op if this key drifts from the URL `CoverSizingInterceptor`
   * actually stores under, so pin the literal both sides have to agree on.
   */
  @Test
  fun keys_by_the_url_the_sizing_interceptor_writes() {
    assertThat(CoverUrls.sized(cover, 768)).isEqualTo("$cover&width=800")
  }

  @Test
  fun prefers_the_largest_cached_rendition_below_the_request() {
    val cache = cacheOf(200, 600)
    assertThat(cache.smallerCachedRendition(cover, 1200))
      .isEqualTo(MemoryCache.Key("$cover&width=600"))
  }

  @Test
  fun ignores_renditions_at_or_above_the_requested_width() {
    val cache = cacheOf(800, 1200)
    assertThat(cache.smallerCachedRendition(cover, 800)).isNull()
  }

  @Test
  fun returns_null_when_no_rendition_is_cached() {
    assertThat(cacheOf().smallerCachedRendition(cover, 1200)).isNull()
  }

  @Test
  fun leaves_urls_the_server_does_not_resize_alone() {
    val external = "https://cdn.example.com/art.png"
    val cache = MemoryCache.Builder().maxSizeBytes(MaxSize).build().apply {
      this[MemoryCache.Key(external)] = MemoryCache.Value(NoopImage)
    }
    assertThat(cache.smallerCachedRendition(external, 1200)).isNull()
  }

  @Test
  fun ignores_data_that_is_not_a_url() {
    assertThat(cacheOf(800).smallerCachedRendition(42, 1200)).isNull()
  }

  private fun cacheOf(vararg widths: Int): MemoryCache {
    return MemoryCache.Builder().maxSizeBytes(MaxSize).build().apply {
      for (width in widths) {
        this[MemoryCache.Key(CoverUrls.sized(cover, width))] = MemoryCache.Value(NoopImage)
      }
    }
  }

  private object NoopImage : Image {
    override val size: Long = 0
    override val width: Int = 1
    override val height: Int = 1
    override val shareable: Boolean = true
    override fun draw(canvas: Canvas) = Unit
  }

  private companion object {
    const val MaxSize = 1024L * 1024L
  }
}
