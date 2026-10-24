// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withCompositionLocal
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.management.api.screen.ItemEditorScreen
import app.campfire.user.test.FakeUserRepository
import app.campfire.user.test.fixtures.user
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class ItemEditorPresenterTest {

  @BeforeTest
  fun setUpMain() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDownMain() {
    Dispatchers.resetMain()
  }

  private val details = mediaMetadata(title = "Children of Ruin", subtitle = null, publisher = "Pan Macmillan")
  private val book = libraryItem(id = BookId, media = media(metadata = details))

  private val libraryItemRepository = FakeLibraryItemRepository()
  private val userRepository = FakeUserRepository()
  private val navigator = FakeNavigator(ItemEditorScreen(BookId))
  private val snackbarHost = SnackbarHostState()

  @Test
  fun `the book's details are ready to edit`() = runTest {
    libraryItemRepository.libraryItemFlow.emit(book)

    presenter().test {
      val state = awaitDraft()

      assertThat(state.draft?.base).isEqualTo(details)
      assertThat(state.canEdit).isTrue()
      assertThat(state.draft?.changes.orEmpty()).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `saving sends only the changes, closes, and offers to undo`() = runTest {
    libraryItemRepository.libraryItemFlow.emit(book)

    presenter().test {
      val state = awaitDraft()
      state.draft!!.title.setTextAndPlaceCursorAtEnd("Children of Memory")

      state.eventSink(ItemEditorUiEvent.Save)

      navigator.awaitPop()
      assertThat(libraryItemRepository.detailUpdates)
        .containsExactly(BookId to BookDetailsUpdate(title = "Children of Memory"))
      val saved = awaitSnackbar()
      assertThat(saved.visuals.message).isEqualTo("Saved the details of Children of Memory")
      assertThat(saved.visuals.actionLabel).isEqualTo("Undo")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `undoing a save puts the details back`() = runTest {
    libraryItemRepository.libraryItemFlow.emit(book)

    presenter().test {
      val state = awaitDraft()
      state.draft!!.title.setTextAndPlaceCursorAtEnd("Children of Memory")
      state.eventSink(ItemEditorUiEvent.Save)
      navigator.awaitPop()

      awaitSnackbar().performAction()

      awaitSnackbar { it.visuals.message == "Put the details of Children of Memory back" }
      assertThat(libraryItemRepository.detailUpdates.last())
        .isEqualTo(BookId to BookDetailsUpdate(title = "Children of Ruin"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a save the server refuses stays open and says why`() = runTest {
    libraryItemRepository.libraryItemFlow.emit(book)
    libraryItemRepository.updateBookDetailsResult = Result.failure(ItemChangeNotAllowedException())

    presenter().test {
      val state = awaitDraft()
      state.draft!!.publisher.setTextAndPlaceCursorAtEnd("Orbit")

      state.eventSink(ItemEditorUiEvent.Save)

      val failed = awaitState { it.message != null }
      assertThat(failed.message).isEqualTo("Your account isn't allowed to change this. Ask your server's admin.")
      assertThat(failed.isSaving).isFalse()
      assertThat(failed.draft?.changes.orEmpty()).containsOnly(BookDetail.Publisher)
      navigator.expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `nothing is sent without a change, or with a missing title`() = runTest {
    libraryItemRepository.libraryItemFlow.emit(book)

    presenter().test {
      val state = awaitDraft()
      state.eventSink(ItemEditorUiEvent.Save)

      state.draft!!.title.setTextAndPlaceCursorAtEnd("")
      state.eventSink(ItemEditorUiEvent.Save)

      assertThat(libraryItemRepository.detailUpdates).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `closing without changes just closes`() = runTest {
    libraryItemRepository.libraryItemFlow.emit(book)

    presenter().test {
      awaitDraft().eventSink(ItemEditorUiEvent.Close)

      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `closing with changes asks first`() = runTest {
    libraryItemRepository.libraryItemFlow.emit(book)

    presenter().test {
      val state = awaitDraft()
      state.draft!!.subtitle.setTextAndPlaceCursorAtEnd("A subtitle")

      state.eventSink(ItemEditorUiEvent.Close)
      val asking = awaitState { it.isConfirmingDiscard }
      navigator.expectNoEvents()

      asking.eventSink(ItemEditorUiEvent.CancelDiscard)
      val editing = awaitState { !it.isConfirmingDiscard }
      editing.eventSink(ItemEditorUiEvent.Close)
      awaitState { it.isConfirmingDiscard }.eventSink(ItemEditorUiEvent.ConfirmDiscard)

      navigator.awaitPop()
      assertThat(libraryItemRepository.detailUpdates).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `details changed on the server meanwhile are pointed out`() = runTest {
    libraryItemRepository.libraryItemFlow.emit(book)

    presenter().test {
      val state = awaitDraft()
      state.draft!!.title.setTextAndPlaceCursorAtEnd("Children of Memory")

      val changedOnServer = details.copy(publisher = "Orbit")
      libraryItemRepository.libraryItemFlow.emit(book.copy(media = media(metadata = changedOnServer)))

      awaitState { it.isBehindServer }.eventSink(ItemEditorUiEvent.KeepChanges)
      val kept = awaitState { !it.isBehindServer }
      // Keeping them leaves the edits alone
      assertThat(kept.draft?.changes.orEmpty()).containsOnly(BookDetail.Title)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `reloading starts again from the server's details`() = runTest {
    libraryItemRepository.libraryItemFlow.emit(book)

    presenter().test {
      val state = awaitDraft()
      state.draft!!.title.setTextAndPlaceCursorAtEnd("Children of Memory")

      val changedOnServer = details.copy(publisher = "Orbit")
      libraryItemRepository.libraryItemFlow.emit(book.copy(media = media(metadata = changedOnServer)))

      awaitState { it.isBehindServer }.eventSink(ItemEditorUiEvent.ResetChanges)
      val reloaded = awaitState { it.draft?.base == changedOnServer }
      assertThat(reloaded.isBehindServer).isFalse()
      assertThat(reloaded.draft?.changes.orEmpty()).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `an account that can't change items is told so`() = runTest {
    userRepository.currentStatefulUserFlow.value = user("user-1").let {
      it.copy(permissions = it.permissions.copy(update = false))
    }
    libraryItemRepository.libraryItemFlow.emit(book)

    presenter().test {
      assertThat(awaitDraft().canEdit).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun TestScope.presenter(): Presenter<ItemEditorUiState> {
    val presenter = ItemEditorPresenter(
      screen = ItemEditorScreen(BookId),
      navigator = navigator,
      libraryItemRepository = libraryItemRepository,
      userRepository = userRepository,
      userScope = CoroutineScopeHolder { backgroundScope },
    )
    return object : Presenter<ItemEditorUiState> {
      @Composable
      override fun present(): ItemEditorUiState = withCompositionLocal(LocalSnackBarHost provides snackbarHost) {
        presenter.present()
      }
    }
  }

  private suspend fun ReceiveTurbine<ItemEditorUiState>.awaitDraft(): ItemEditorUiState =
    awaitState { it.draft != null }

  private suspend fun ReceiveTurbine<ItemEditorUiState>.awaitState(
    predicate: (ItemEditorUiState) -> Boolean,
  ): ItemEditorUiState {
    var state = awaitItem()
    while (!predicate(state)) state = awaitItem()
    return state
  }

  // The snackbar is shown past the screen, on the user's scope, after resources load
  private suspend fun awaitSnackbar(
    predicate: (androidx.compose.material3.SnackbarData) -> Boolean = { true },
  ): androidx.compose.material3.SnackbarData = withContext(Dispatchers.Default) {
    withTimeout(5.seconds) {
      var data = snackbarHost.currentSnackbarData
      while (data == null || !predicate(data)) {
        delay(10)
        data = snackbarHost.currentSnackbarData
      }
      assertThat(data).isNotNull()
      data
    }
  }

  private companion object {
    const val BookId = "book"
  }
}
