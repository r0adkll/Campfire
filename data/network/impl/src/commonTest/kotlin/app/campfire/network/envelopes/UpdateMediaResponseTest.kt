// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.serialization.json.Json

/** The server's answers to a media update, as the user client's JSON reads them. */
class UpdateMediaResponseTest {

  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
  }

  @Test
  fun `a change or none is said plainly`() {
    assertThat(decode("""{"updated":true,"libraryItem":{}}""").updated).isTrue()
    assertThat(decode("""{"updated":false,"libraryItem":{}}""").updated).isFalse()
  }

  @Test
  fun `a new cover alone is answered with its address`() {
    val response = decode("""{"updated":"https://m.media-amazon.com/images/I/cover.jpg","libraryItem":{}}""")

    assertThat(response.updated).isTrue()
  }

  private fun decode(body: String) = json.decodeFromString<UpdateMediaResponse>(body)
}
