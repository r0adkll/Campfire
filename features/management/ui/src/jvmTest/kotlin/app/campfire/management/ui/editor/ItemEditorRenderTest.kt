// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.authorMetadata
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.libraries.api.SeriesEntry
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.ImageSize
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.ui.match.MatchField
import app.campfire.management.ui.match.MatchReviewState
import app.campfire.management.ui.match.MatchSearch
import app.campfire.management.ui.match.MatchUiState
import app.campfire.management.ui.match.SelectionMode
import app.campfire.management.ui.match.reviewRows
import app.campfire.management.ui.match.selectedBy
import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.slack.circuit.overlay.ContentWithOverlays
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.jetbrains.skia.EncodedImageFormat

/**
 * The editor on a phone and in a wide workspace, each with a change made and the server's
 * details moved on, and matching the book against a source. PNGs land in `build/renders` for
 * eyeballing.
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

  @Test
  fun filledFromAMatch() = render("item-editor-filled.png", width = 412, height = 915, filled = true)

  @Test
  fun matchResults() = render("item-editor-match-phone.png", width = 412, height = 915, section = EditorSection.Match)

  @Test
  fun matchReview() = render(
    "item-editor-match-phone-review.png",
    width = 412,
    height = 915,
    section = EditorSection.Match,
    reviewing = true,
  )

  @Test
  fun matchReviewSideways() = render(
    "item-editor-match-landscape-review.png",
    width = 915,
    height = 412,
    section = EditorSection.Match,
    reviewing = true,
  )

  private val matches = listOf(
    BookMatch(
      title = "Children of Ruin",
      subtitle = "Children of Time, Book 2",
      authors = listOf("Adrian Tchaikovsky"),
      narrators = listOf("Mel Hudson"),
      publishedYear = "2019",
      publisher = "Recorded Books",
      description = "<p>Thousands of years ago, Earth's terraforming program took to the stars.</p>",
      genres = listOf("Science Fiction", "Hard Science Fiction"),
      series = listOf(SeriesEntry("Children of Time", "2")),
      asin = "B07PJV6N9T",
      coverUrl = "https://m.media-amazon.com/images/I/cover.jpg",
      language = "English",
      duration = 15.hours + 31.minutes,
      confidence = 0.97f,
    ),
    BookMatch(
      title = "Children of Ruin (Abridged)",
      authors = listOf("Adrian Tchaikovsky"),
      narrators = listOf("Kevin Hely"),
      publishedYear = "2020",
      duration = 9.hours + 4.minutes,
      confidence = 0.61f,
    ),
    BookMatch(
      title = "Children of Memory",
      authors = listOf("Adrian Tchaikovsky"),
      publishedYear = "2022",
      series = listOf(SeriesEntry("Children of Time", "3")),
      duration = 14.hours + 2.minutes,
      confidence = 0.34f,
    ),
  )

  private fun render(
    name: String,
    width: Int,
    height: Int,
    section: EditorSection = EditorSection.Details,
    reviewing: Boolean = false,
    filled: Boolean = false,
  ) {
    val renders = File("build/renders").apply { mkdirs() }
    val draft = BookDetailsDraft(details, tags).apply {
      title.setTextAndPlaceCursorAtEnd("Children of Ruin")
      publishedYear.setTextAndPlaceCursorAtEnd("2020")
      narrators.add("Gary Furlong")
      addSeries()
      series.last().sequence.setTextAndPlaceCursorAtEnd("3")
      if (filled) newCoverUrl = "https://m.media-amazon.com/images/I/cover.jpg"
    }
    val review = matches.first().takeIf { reviewing }?.let { match ->
      val rows = reviewRows(match, draft, coverUrl = "https://abs.example/api/items/book/cover")
      MatchReviewState(
        match = match,
        providerName = "Audible.com",
        rows = rows,
        selected = rows.selectedBy(SelectionMode.All, canChangeCover = true) - MatchField.Description,
        merges = emptyMap(),
        canChangeCover = true,
        currentCoverSize = ImageSize(500, 500),
        newCoverSize = ImageSize(2400, 2400),
      )
    }
    val match = MatchUiState(
      providers = listOf(MatchProvider("audible", "Audible.com"), MatchProvider("google", "Google Books")),
      provider = MatchProvider("audible", "Audible.com"),
      title = TextFieldState("B07PJV6N9T"),
      author = TextFieldState(),
      search = MatchSearch.Found(MatchProvider("audible", "Audible.com"), matches),
      bookDuration = 15.hours + 30.minutes,
      bookAsin = "B07PJV6N9T",
      review = review,
      canChangeCover = true,
      eventSink = {},
    )
    val bytes = ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f),
      content = {
        // The window the sheet a review opens in reads to choose its side
        CompositionLocalProvider(
          LocalWindowSizeClass provides WindowSizeClass(minWidthDp = width, minHeightDp = height),
        ) {
          // The app's overlay host sits inside its theme, so its sheets are themed too
          CampfireTheme(useDarkColors = false) {
            ContentWithOverlays {
              Editor(draft, section, match, filledFrom = "Audible.com".takeIf { filled })
            }
          }
        }
      },
    ).use { scene ->
      repeat(5) { frame -> scene.render(frame * 100_000_000L) }
      scene.render(nanoTime = 2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
    }
    File(renders, name).writeBytes(bytes)
    assertThat(bytes.size).isGreaterThan(1_000)
  }

  @Composable
  private fun Editor(
    draft: BookDetailsDraft,
    section: EditorSection,
    match: MatchUiState,
    filledFrom: String?,
  ) {
    ItemEditorUi(
      state = ItemEditorUiState(
        item = LoadState.Loaded(libraryItem(id = "book", media = media(metadata = details))),
        draft = draft,
        section = section,
        match = match,
        filledFrom = filledFrom,
        canEdit = true,
        canSeeExplicit = true,
        suggestions = DetailSuggestions(),
        isSaving = false,
        isBehindServer = true,
        isConfirmingDiscard = false,
        isLoadFailed = false,
        message = null,
        eventSink = {},
      ),
    )
  }
}
