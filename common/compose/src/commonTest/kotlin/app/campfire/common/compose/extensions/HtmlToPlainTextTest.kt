// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.extensions

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class HtmlToPlainTextTest {

  @Test
  fun `drops tags and keeps the text`() {
    val html = """<p>In this episode, <b>Jane</b> talks <a href="https://example.com">tools</a>.</p>"""

    assertThat(html.htmlToPlainText()).isEqualTo("In this episode, Jane talks tools.")
  }

  @Test
  fun `breaks and paragraphs become spaces, and whitespace collapses`() {
    val html = "<p>First line</p><p>Second<br/>line</p>\n\n  <ul><li>One</li><li>Two</li></ul>"

    assertThat(html.htmlToPlainText()).isEqualTo("First line Second line One Two")
  }

  @Test
  fun `decodes named and numeric entities`() {
    val html = "Fish &amp; chips &mdash; &quot;great&quot; &#39;fun&#39; &#x1F525; &hearts;"

    // Unknown names are left as they are
    assertThat(html.htmlToPlainText()).isEqualTo("Fish & chips — \"great\" 'fun' 🔥 &hearts;")
  }

  @Test
  fun `strips escaped leading newlines left over from decoding`() {
    assertThat("\\n\\nHello".htmlToPlainText()).isEqualTo("Hello")
  }

  @Test
  fun `plain text passes through`() {
    assertThat("Just words, 5 < 6.".htmlToPlainText()).isEqualTo("Just words, 5 < 6.")
  }
}
