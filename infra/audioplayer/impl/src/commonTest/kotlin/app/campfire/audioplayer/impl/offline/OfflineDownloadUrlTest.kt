// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.offlineDownloadUrl
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class OfflineDownloadUrlTest {

  @Test
  fun libraryFileUrl_downloadsThroughTheDownloadRoute() {
    assertThat(offlineDownloadUrl("https://abs.example.com/api/items/li_1/file/123"))
      .isEqualTo("https://abs.example.com/api/items/li_1/file/123/download")
  }

  @Test
  fun libraryFileUrlWithQuery_dropsTheQuery() {
    assertThat(offlineDownloadUrl("https://abs.example.com/api/items/li_1/file/123?token=abc"))
      .isEqualTo("https://abs.example.com/api/items/li_1/file/123/download")
  }

  @Test
  fun otherUrls_downloadAsIs() {
    assertThat(offlineDownloadUrl("https://abs.example.com/hls/stream.m3u8"))
      .isEqualTo("https://abs.example.com/hls/stream.m3u8")
  }
}
