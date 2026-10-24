// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test

/** Which of the library's names are suggested for what's typed, and in what order. */
class DetailSuggestionsTest {

  private val genres = listOf("Alien Contact", "Contemporary", "First Contact", "Fantasy", "Science Fiction")

  @Test
  fun `names starting with what's typed come first, then words starting with it, then the rest`() {
    val suggested = suggestionsFor("con", genres + "Uncontrolled")

    assertThat(suggested.map { it.name })
      .containsExactly("Contemporary", "Alien Contact", "First Contact", "Uncontrolled")
  }

  @Test
  fun `the match ignores case and marks where it is`() {
    val suggested = suggestionsFor("CONTACT", listOf("First Contact"))

    assertThat(suggested.single().match).isEqualTo(6..12)
  }

  @Test
  fun `a word match is preferred to an earlier match inside a word`() {
    val suggested = suggestionsFor("ark", listOf("Starks of the Ark"))

    assertThat(suggested.single().match).isEqualTo(14..16)
  }

  @Test
  fun `names already chosen aren't suggested, ignoring case`() {
    val suggested = suggestionsFor("contact", genres, excluding = listOf("first contact"))

    assertThat(suggested.map { it.name }).containsExactly("Alien Contact")
  }

  @Test
  fun `nothing is suggested for nothing typed`() {
    assertThat(suggestionsFor("  ", genres)).isEmpty()
  }

  @Test
  fun `only a few are suggested`() {
    val names = (1..20).map { "Series $it" }

    assertThat(suggestionsFor("series", names, limit = 6).size).isEqualTo(6)
  }
}
