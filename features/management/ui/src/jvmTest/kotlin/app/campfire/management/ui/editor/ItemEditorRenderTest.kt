// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.authorMetadata
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import assertk.assertThat
import assertk.assertions.isGreaterThan
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * The editor on a phone and in a wide workspace, each with a change made and the server's
 * details moved on. PNGs land in `build/renders` for eyeballing.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ItemEditorRenderTest {

  private val details = mediaMetadata(
    title = "Children of Ruin",
    subtitle = null,
    authorName = "Adrian Tchaikovsky",
    description = "<p>Generations after humanity's terraforming ships reached the stars, an " +
      "expedition follows an <b>old signal</b> to a world where something else is already waiting.</p>",
    publisher = "Pan Macmillan",
    publishedYear = "2019",
    language = "English",
    ISBN = "9781509865857",
    ASIN = "B07PJV6N9T",
    authors = listOf(authorMetadata(id = "author-1", name = "Adrian Tchaikovsky")),
    narrators = listOf("Mel Hudson"),
    seriesSequence = SeriesSequence(id = "series-1", name = "Children of Time", sequence = 2.0, label = "2"),
    genres = listOf("Science Fiction", "Space Opera"),
  )
  private val tags = listOf("Book club")

  @Test
  fun phone() = render("item-editor-phone.png", width = 412, height = 915)

  @Test
  fun phoneWhole() = render("item-editor-phone-whole.png", width = 412, height = 2300)

  @Test
  fun wide() = render("item-editor-wide.png", width = 960, height = 800)

  @Test
  fun wideWhole() = render("item-editor-wide-whole.png", width = 960, height = 1500)

  private fun render(name: String, width: Int, height: Int) {
    val renders = File("build/renders").apply { mkdirs() }
    val draft = BookDetailsDraft(details, tags).apply {
      title.setTextAndPlaceCursorAtEnd("Children of Ruin")
      publishedYear.setTextAndPlaceCursorAtEnd("2020")
      narrators.add("Gary Furlong")
      addSeries()
      series.last().sequence.setTextAndPlaceCursorAtEnd("3")
    }
    val bytes = ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f),
      content = { Editor(draft) },
    ).use { scene ->
      repeat(5) { frame -> scene.render(frame * 100_000_000L) }
      scene.render(nanoTime = 2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
    }
    File(renders, name).writeBytes(bytes)
    assertThat(bytes.size).isGreaterThan(1_000)
  }

  @Composable
  private fun Editor(draft: BookDetailsDraft) {
    CampfireTheme(useDarkColors = false) {
      ItemEditorUi(
        state = ItemEditorUiState(
          item = LoadState.Loaded(libraryItem(id = "book", media = media(metadata = details))),
          draft = draft,
          canEdit = true,
          canSeeExplicit = true,
          suggestions = DetailSuggestions(),
          isSaving = false,
          isBehindServer = true,
          isConfirmingDiscard = false,
          isLoadFailed = false,
          eventSink = {},
        ),
      )
    }
  }
}
