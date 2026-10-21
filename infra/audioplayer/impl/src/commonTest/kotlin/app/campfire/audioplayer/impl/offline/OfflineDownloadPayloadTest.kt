// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class OfflineDownloadPayloadTest {

  @Test
  fun payload_readsBackAsWritten() {
    val episode = OfflineDownloadPayload("li_1", "ep_1", title = "Episode One", subtitle = "The Podcast")
    val book = OfflineDownloadPayload("li_2", title = "The Martian")

    assertThat(OfflineDownloadPayload.decode(episode.encode())).isEqualTo(episode)
    assertThat(OfflineDownloadPayload.decode(book.encode())).isEqualTo(book)
  }

  @Test
  fun payload_readsTheOlderFormats() {
    assertThat(OfflineDownloadPayload.decode("li_1|ep_1".encodeToByteArray()))
      .isEqualTo(OfflineDownloadPayload("li_1", "ep_1"))
    assertThat(OfflineDownloadPayload.decode("li_1|".encodeToByteArray()))
      .isEqualTo(OfflineDownloadPayload("li_1"))
    assertThat(OfflineDownloadPayload.decode("li_1".encodeToByteArray()))
      .isEqualTo(OfflineDownloadPayload("li_1"))
  }

  @Test
  fun key_readsBackAsWritten() {
    val episode = OfflineDownloadKey("li_1", "ep_1")
    val book = OfflineDownloadKey("li_2")

    assertThat(OfflineDownloadKey.decode(episode.encode())).isEqualTo(episode)
    assertThat(OfflineDownloadKey.decode(book.encode())).isEqualTo(book)
  }
}
