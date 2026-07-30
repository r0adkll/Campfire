// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.browse

import kotlin.test.Test
import kotlin.test.assertEquals

class BrowseMediaIdTest {

  @Test
  fun `bare library item id round trips`() {
    val browseId = BrowseMediaId("li_abc123")
    assertEquals("li_abc123", browseId.encoded())
    assertEquals(browseId, BrowseMediaId.decode(browseId.encoded()))
  }

  @Test
  fun `episode id round trips`() {
    val browseId = BrowseMediaId("li_abc123", "ep_xyz789")
    assertEquals("li_abc123::ep_xyz789", browseId.encoded())
    assertEquals(browseId, BrowseMediaId.decode(browseId.encoded()))
  }

  @Test
  fun `uuid style ids round trip`() {
    val browseId = BrowseMediaId(
      libraryItemId = "0693cf5c-98f5-4bc4-b2c4-63a91d38b0f0",
      episodeId = "77b6f0e5-8929-4111-b6b8-eef6f4f9c0f3",
    )
    assertEquals(browseId, BrowseMediaId.decode(browseId.encoded()))
  }

  @Test
  fun `decoding an id with a trailing separator yields no episode`() {
    val browseId = BrowseMediaId.decode("li_abc123::")
    assertEquals(BrowseMediaId("li_abc123"), browseId)
  }

  @Test
  fun `decoding a folder id passes through unchanged`() {
    val browseId = BrowseMediaId.decode("downloads-campfire")
    assertEquals(BrowseMediaId("downloads-campfire"), browseId)
  }
}
