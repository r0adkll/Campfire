// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account

import app.campfire.common.test.user
import app.campfire.core.session.UserSession
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ServerUrlHydratorTest {

  private val hydrator = ServerUrlHydrator(
    userSession = UserSession.LoggedIn(user(id = "user_1", serverUrl = "https://abs.example.com")),
  )

  @Test
  fun hydrateUrl_prefixes_server_url() {
    assertThat(hydrator.hydrateUrl("/api/items/li_1/play"))
      .isEqualTo("https://abs.example.com/api/items/li_1/play")
  }

  @Test
  fun library_item_cover_without_timestamp() {
    assertThat(hydrator.hydrateLibraryItem("li_1"))
      .isEqualTo("https://abs.example.com/api/items/li_1/cover")
  }

  @Test
  fun library_item_cover_appends_updated_at_as_ts() {
    assertThat(hydrator.hydrateLibraryItem("li_1", updatedAtMillis = 1_700_000_000_000))
      .isEqualTo("https://abs.example.com/api/items/li_1/cover?ts=1700000000000")
  }

  @Test
  fun author_image_appends_updated_at_as_ts() {
    assertThat(hydrator.hydrateAuthor("au_1", updatedAtMillis = 42))
      .isEqualTo("https://abs.example.com/api/authors/au_1/image?ts=42")
  }

  @Test
  fun non_positive_timestamps_are_ignored() {
    assertThat(hydrator.hydrateLibraryItem("li_1", updatedAtMillis = 0))
      .isEqualTo("https://abs.example.com/api/items/li_1/cover")
    assertThat(hydrator.hydrateAuthor("au_1", updatedAtMillis = -1))
      .isEqualTo("https://abs.example.com/api/authors/au_1/image")
  }
}
