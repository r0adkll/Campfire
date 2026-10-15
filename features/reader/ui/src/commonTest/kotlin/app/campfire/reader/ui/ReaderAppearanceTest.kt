// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import app.campfire.settings.api.ReaderFlow
import app.campfire.settings.api.ReaderFont
import app.campfire.settings.api.ReaderLayout
import app.campfire.settings.api.ReaderLayoutOverrides
import app.campfire.settings.api.ReaderMargin
import app.campfire.settings.api.ReaderPages
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class ReaderAppearanceTest {

  private val palette = ReaderPalette(Color.White, Color.Black, Color.Blue)

  @Test
  fun `the layout maps onto the page's style`() {
    val style = readerStyle(
      palette,
      ReaderLayout(
        font = ReaderFont.Literata,
        fontScale = 120,
        lineHeight = 1.8f,
        margin = ReaderMargin.Wide,
        justify = false,
        hyphenate = false,
      ),
    )

    assertThat(style.background).isEqualTo("#ffffff")
    assertThat(style.foreground).isEqualTo("#000000")
    assertThat(style.link).isEqualTo("#0000ff")
    assertThat(style.fontFamily).isEqualTo("'Literata', serif")
    assertThat(style.fontScale).isEqualTo(120)
    assertThat(style.lineHeight).isEqualTo(1.8f)
    assertThat(style.gap).isEqualTo(12)
    assertThat(style.justify).isEqualTo(false)
    assertThat(style.hyphenate).isEqualTo(false)
  }

  @Test
  fun `the publisher font keeps the book's own`() {
    assertThat(readerStyle(palette, ReaderLayout(font = ReaderFont.Publisher)).fontFamily).isNull()
  }

  @Test
  fun `pages side by side`() {
    fun columns(pages: ReaderPages) = readerStyle(palette, ReaderLayout(pages = pages)).let {
      it.columns to it.portraitColumns
    }

    assertThat(columns(ReaderPages.Auto)).isEqualTo(2 to 1)
    assertThat(columns(ReaderPages.Single)).isEqualTo(1 to 1)
    assertThat(columns(ReaderPages.Two)).isEqualTo(2 to 2)
  }

  @Test
  fun `a fold splits two pages around it, wider than the margin needs`() {
    val style = readerStyle(palette, ReaderLayout(margin = ReaderMargin.Narrow), foldGap = 9)

    assertThat(style.columns to style.portraitColumns).isEqualTo(2 to 2)
    assertThat(style.gap).isEqualTo(9)
  }

  @Test
  fun `a fold leaves one page, and scrolling, alone`() {
    val single = readerStyle(palette, ReaderLayout(pages = ReaderPages.Single), foldGap = 9)
    val scrolled = readerStyle(palette, ReaderLayout(flow = ReaderFlow.Scrolled), foldGap = 9)

    assertThat(single.columns to single.portraitColumns).isEqualTo(1 to 1)
    assertThat(scrolled.scrolled).isEqualTo(true)
    assertThat(scrolled.portraitColumns).isEqualTo(1)
    assertThat(scrolled.gap).isEqualTo(7)
  }

  @Test
  fun `the fold gap clears the hinge and a margin either side`() {
    // 24dp either side at 2x is 96px, plus a 40px hinge: 136px of a 1600px page is 8.5%
    val gap = foldGapPercent(BookFold(left = 780f, right = 820f), pageWidth = 1600f, density = Density(2f))

    assertThat(gap).isEqualTo(9)
  }

  @Test
  fun `only a vertical fold that divides the screen is a book fold`() {
    fun hinge(vertical: Boolean, flat: Boolean, separating: Boolean) =
      HingeInfo(Rect(1000f, 0f, 1040f, 2000f), flat, vertical, separating, false)

    assertThat(listOf(hinge(vertical = true, flat = false, separating = true)).bookFold())
      .isEqualTo(BookFold(1000f, 1040f))
    assertThat(listOf(hinge(vertical = true, flat = true, separating = false)).bookFold()).isNull()
    assertThat(listOf(hinge(vertical = false, flat = false, separating = true)).bookFold()).isNull()
  }

  @Test
  fun `a layout change applies to all books or as this book's override`() {
    val change = ReaderLayoutChange.FontScale(500)

    assertThat(change.applyTo(ReaderLayout()).fontScale).isEqualTo(ReaderLayout.FontScaleRange.last)
    assertThat(ReaderLayoutChange.Font(ReaderFont.Sans).applyTo(ReaderLayoutOverrides.None))
      .isEqualTo(ReaderLayoutOverrides(font = ReaderFont.Sans))
  }

  @Test
  fun `overrides replace only what they set`() {
    val layout = ReaderLayoutOverrides(fontScale = 140, pages = ReaderPages.Single).applyTo(ReaderLayout())

    assertThat(layout).isEqualTo(ReaderLayout(fontScale = 140, pages = ReaderPages.Single))
  }
}
