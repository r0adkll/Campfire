// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItemId
import app.campfire.reader.api.Ebook
import app.campfire.reader.api.EbookContentsEntry
import app.campfire.reader.api.EbookContentsRepository
import app.campfire.reader.api.EbookRepository
import app.campfire.reader.api.ReaderBookSettingsRepository
import app.campfire.reader.api.screen.ReaderScreen
import app.campfire.reader.ui.web.ReaderStyle
import app.campfire.settings.api.ReaderFont
import app.campfire.settings.api.ReaderLayout
import app.campfire.settings.api.ReaderLayoutOverrides
import app.campfire.settings.api.ReaderTheme
import app.campfire.settings.test.FakeReaderSettings
import app.campfire.user.test.FakeMediaProgressRepository
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ReaderPresenterTest {

  private val screen = ReaderScreen(ITEM)
  private val navigator = FakeNavigator(screen)
  private val ebooks = FakeEbookRepository()
  private val mediaProgress = FakeMediaProgressRepository()
  private val readerSettings = FakeReaderSettings()
  private val bookSettings = FakeReaderBookSettingsRepository()
  private val contents = FakeEbookContentsRepository()

  private val presenter = presenter(screen)

  private fun presenter(screen: ReaderScreen) = ReaderPresenter(
    screen = screen,
    navigator = navigator,
    repository = ebooks,
    mediaProgressRepository = mediaProgress,
    audioPlayerHolder = FakeAudioPlayerHolder(),
    readerSettings = readerSettings,
    bookSettings = bookSettings,
    contentsRepository = contents,
  )

  @Test
  fun `the book's contents are kept for its details`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      state.bridge.onMessage(
        """{"type":"opened","toc":[{"label":"Chapter 1","href":"ch1.xhtml","depth":0,"fraction":0.1}]}""",
      )

      awaitUntil { it.toc.isNotEmpty() }
      assertThat(contents.saved.await()).containsExactly(EbookContentsEntry("Chapter 1", "ch1.xhtml", 0, 0.1f))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `opened at an entry in the contents, it goes there and offers the way back`() = runTest {
    presenter(ReaderScreen(ITEM, href = "ch9.xhtml")).test {
      val state = awaitLoaded()
      state.bridge.onMessage("""{"type":"restored","restored":true}""")

      val jumped = awaitUntil { it.jumpBack != null }
      assertThat(jumped.jumpBack).isEqualTo(JumpBack(LOCATION, 0.25f))
      assertThat(jumped.chromeVisible).isTrue()
      assertThat(state.bridge.drainScripts()).contains("""globalThis.campfire.goTo("ch9.xhtml")""")

      // A renderer replaced later restores where the reader is, without jumping again
      state.bridge.onMessage("""{"type":"restored","restored":true}""")
      assertThat(state.bridge.drainScripts()).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `opens the book where it was left once the page is ready`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      state.bridge.onMessage("""{"type":"ready"}""")

      val open = state.bridge.awaitScript()
      assertThat(open).contains("globalThis.campfire.open(")
      assertThat(open).contains(LOCATION)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a page the reader moved to is saved, a page laid out again is not`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      state.bridge.onMessage("""{"type":"relocate","location":"layout","progress":0.1,"save":false}""")
      state.bridge.onMessage("""{"type":"relocate","location":"turned","progress":0.2,"save":true}""")

      val moved = awaitUntil { it.position.progress == 0.2f }
      assertThat(moved.position.progress).isEqualTo(0.2f)
      assertThat(ebooks.saved).containsExactly("turned" to 0.2f)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the position names the chapter and the part it is in`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      state.bridge.onMessage(
        """
          {"type":"relocate","location":"x","progress":0.4,
          "part":"Part One","chapter":"Chapter Two","chapterHref":"ch2.xhtml","pagesLeft":3}
        """.trimIndent(),
      )

      val moved = awaitUntil { it.position.progress == 0.4f }
      assertThat(moved.position).isEqualTo(
        ReaderPosition(
          part = "Part One",
          chapter = "Chapter Two",
          chapterHref = "ch2.xhtml",
          progress = 0.4f,
          pagesLeftInChapter = 3,
        ),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a tap shows and hides the bars, and back closes the book`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      assertThat(state.chromeVisible).isFalse()

      state.bridge.onMessage("""{"type":"tap"}""")
      val shown = awaitUntil { it.chromeVisible }
      shown.eventSink(ReaderUiEvent.Back)

      assertThat(navigator.awaitPop()).isNotNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `jumping through the contents offers the way back`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      state.bridge.onMessage("""{"type":"relocate","location":"here","progress":0.3,"save":true}""")
      awaitUntil { it.position.progress == 0.3f }.eventSink(
        ReaderUiEvent.GoTo(app.campfire.reader.ui.web.TocEntry("Chapter 9", "ch9.xhtml", 0)),
      )

      val jumped = awaitUntil { it.jumpBack != null }
      assertThat(jumped.jumpBack).isEqualTo(JumpBack("here", 0.3f))
      assertThat(state.bridge.drainScripts()).contains("""globalThis.campfire.goTo("ch9.xhtml")""")

      jumped.eventSink(ReaderUiEvent.JumpBack)
      awaitUntil { it.jumpBack == null }
      assertThat(state.bridge.drainScripts()).contains("""globalThis.campfire.goToLocation("here")""")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the last page offers to mark the book finished`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      state.bridge.onMessage("""{"type":"relocate","location":"end","progress":1.0,"atEnd":true,"save":true}""")

      val end = awaitUntil { it.endOfBook != null }
      end.eventSink(ReaderUiEvent.MarkFinished)
      end.eventSink(ReaderUiEvent.DismissEndOfBook)

      awaitUntil { it.endOfBook == null }
      assertThat(mediaProgress.invocations)
        .contains(FakeMediaProgressRepository.Invocation.MarkFinished(ITEM))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `only an error opening the book replaces it`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      state.bridge.onMessage("""{"type":"error","message":"ResizeObserver loop","fatal":false}""")
      state.bridge.onMessage("""{"type":"relocate","location":"x","progress":0.5}""")
      assertThat(awaitUntil { it.position.progress == 0.5f }.bookError).isNull()

      state.bridge.onMessage("""{"type":"error","message":"Bad book","fatal":true}""")
      assertThat(awaitUntil { it.bookError != null }.bookError).isEqualTo("Bad book")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the appearance panel changes every book, or this one`() = runTest {
    presenter.test {
      awaitLoaded().eventSink(ReaderUiEvent.ShowAppearance)
      val allBooks = awaitUntil { it.appearance != null }
      assertThat(allBooks.appearance?.scope).isEqualTo(AppearanceScope.AllBooks)

      allBooks.eventSink(ReaderUiEvent.ThemeChanged(ReaderTheme.Sepia))
      allBooks.eventSink(ReaderUiEvent.LayoutChanged(ReaderLayoutChange.FontScale(130)))
      assertThat(readerSettings.theme).isEqualTo(ReaderTheme.Sepia)
      assertThat(readerSettings.layout.fontScale).isEqualTo(130)

      allBooks.eventSink(ReaderUiEvent.AppearanceScopeChanged(AppearanceScope.ThisBook))
      val thisBook = awaitUntil { it.appearance?.scope == AppearanceScope.ThisBook }
      thisBook.eventSink(ReaderUiEvent.LayoutChanged(ReaderLayoutChange.Font(ReaderFont.OpenDyslexic)))

      val overridden = awaitUntil { it.layout.font == ReaderFont.OpenDyslexic }
      assertThat(overridden.appearance?.hasOverrides).isEqualTo(true)
      assertThat(overridden.layout.fontScale).isEqualTo(130)
      assertThat(readerSettings.layout.font).isEqualTo(ReaderFont.Publisher)

      overridden.eventSink(ReaderUiEvent.ResetBookLayout)
      awaitUntil { it.layout.font == ReaderFont.Publisher }
      overridden.eventSink(ReaderUiEvent.HideAppearance)
      awaitUntil { it.appearance == null }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `page turns and style changes reach the page`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      val style = ReaderStyle(background = "#000000", foreground = "#ffffff", link = "#0000ff")
      state.eventSink(ReaderUiEvent.StyleChanged(style))
      state.eventSink(ReaderUiEvent.StyleChanged(style.copy(fontScale = 150)))
      state.eventSink(ReaderUiEvent.NextPage)
      state.eventSink(ReaderUiEvent.PreviousPage)
      state.eventSink(ReaderUiEvent.Scrub(0.5f))

      val scripts = state.bridge.drainScripts()
      assertThat(scripts.count { it.startsWith("globalThis.campfire.setStyle(") }).isEqualTo(1)
      assertThat(scripts).contains("globalThis.campfire.next()")
      assertThat(scripts).contains("globalThis.campfire.prev()")
      assertThat(scripts).contains("globalThis.campfire.goToFraction(0.5)")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the screen setting and volume keys are saved`() = runTest {
    presenter.test {
      val state = awaitLoaded()
      state.eventSink(ReaderUiEvent.KeepScreenOnChanged(false))
      state.eventSink(ReaderUiEvent.VolumeKeysChanged(false))

      val changed = awaitUntil { !it.keepScreenOn && !it.volumeKeysTurnPages }
      assertThat(changed.keepScreenOn).isFalse()
      assertThat(readerSettings.volumeKeysTurnPages).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `reset puts every book's theme and layout back, but not the preferences`() = runTest {
    readerSettings.theme = ReaderTheme.Black
    readerSettings.layout = ReaderLayout(font = ReaderFont.Literata, fontScale = 150)
    readerSettings.keepScreenOn = false

    presenter.test {
      awaitLoaded().eventSink(ReaderUiEvent.ResetToDefaults)

      val reset = awaitUntil { it.theme == ReaderTheme.FollowApp && it.layout == ReaderLayout() }
      assertThat(reset.keepScreenOn).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a book that can't be opened shows as an error`() = runTest {
    ebooks.openResult = Result.failure(IllegalStateException("No ebook"))

    presenter.test {
      assertThat(awaitUntil { it.content !is LoadState.Loading }.content).isInstanceOf<LoadState.Error>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  private suspend fun com.slack.circuit.test.CircuitReceiveTurbine<ReaderUiState>.awaitLoaded(): ReaderUiState =
    awaitUntil { it.content is LoadState.Loaded }.also { assertThat(it.title).isEqualTo("A Book") }

  private suspend fun com.slack.circuit.test.CircuitReceiveTurbine<ReaderUiState>.awaitUntil(
    predicate: (ReaderUiState) -> Boolean,
  ): ReaderUiState {
    while (true) {
      val state = awaitItem()
      if (predicate(state)) return state
    }
  }

  private companion object {
    const val ITEM = "item-1"
    const val LOCATION = "epubcfi(/6/4!/4/2/1:0)"
  }

  private class FakeEbookRepository : EbookRepository {
    var openResult: Result<Ebook> = Result.success(
      Ebook(
        itemId = ITEM,
        title = "A Book",
        author = "An Author",
        format = "epub",
        filePath = "/books/item-1.epub",
        location = LOCATION,
        progress = 0.25f,
      ),
    )
    val saved = mutableListOf<Pair<String, Float>>()

    override suspend fun open(itemId: LibraryItemId): Result<Ebook> = openResult

    override suspend fun saveProgress(itemId: LibraryItemId, location: String, progress: Float): Result<Unit> {
      saved += location to progress
      return Result.success(Unit)
    }
  }

  private class FakeEbookContentsRepository : EbookContentsRepository {
    val saved = CompletableDeferred<List<EbookContentsEntry>>()

    override fun observeContents(itemId: LibraryItemId): Flow<List<EbookContentsEntry>> = flowOf(emptyList())

    override suspend fun saveContents(itemId: LibraryItemId, entries: List<EbookContentsEntry>) {
      saved.complete(entries)
    }
  }

  private class FakeReaderBookSettingsRepository : ReaderBookSettingsRepository {
    private val overrides = MutableStateFlow(ReaderLayoutOverrides.None)

    override fun observeOverrides(itemId: LibraryItemId): Flow<ReaderLayoutOverrides> = overrides

    override suspend fun setOverrides(itemId: LibraryItemId, overrides: ReaderLayoutOverrides) {
      this.overrides.value = overrides
    }
  }
}

private fun app.campfire.reader.ui.web.ReaderBridge.drainScripts(): List<String> = buildList {
  while (true) add(scripts.tryReceive().getOrNull() ?: break)
}

private suspend fun app.campfire.reader.ui.web.ReaderBridge.awaitScript(): String = scripts.receive()
