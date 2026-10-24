// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.common.test.mediaProgress
import app.campfire.common.test.user
import app.campfire.core.coroutines.LoadState
import app.campfire.core.extensions.asDateTime
import app.campfire.libraries.ui.detail.composables.slots.ProgressSlot
import app.campfire.libraries.ui.detail.dates.ListeningDate
import app.campfire.user.test.FakeMediaProgressRepository.Invocation
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.single
import com.slack.circuit.test.CircuitReceiveTurbine
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class ListeningDatesPresenterTest : BaseLibraryItemPresenterTest() {

  @Test
  fun finishedProgress_opensAndPutsAwayTheFinishedDatePicker() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    mediaProgressRepository.progressFlow.value = finishedProgress()

    presenter.test {
      val initial = awaitProgressSlot { true }
      assertThat(initial.canEditDates).isTrue()
      assertThat(initial.editingDate).isNull()

      latest.eventSink(LibraryItemUiEvent.EditListeningDate(ListeningDate.Finished))
      awaitProgressSlot { it.editingDate == ListeningDate.Finished }

      latest.eventSink(LibraryItemUiEvent.DismissListeningDate)
      awaitProgressSlot { it.editingDate == null }

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun unfinishedProgress_hasNoDatesToChange() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    mediaProgressRepository.progressFlow.value = mediaProgress(TestLibraryItemId, currentTime = 20f)

    presenter.test {
      val unfinished = awaitProgressSlot { true }
      assertThat(unfinished.canEditDates).isFalse()

      // "Set date" can be tapped before the item is finished; nothing opens for it
      latest.eventSink(LibraryItemUiEvent.EditListeningDate(ListeningDate.Finished))
      mediaProgressRepository.progressFlow.value = finishedProgress()

      val finished = awaitProgressSlot { it.canEditDates }
      assertThat(finished.editingDate).isNull()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun changingTheFinishedDate_keepsItsTimeOfDayOnTheNewDay() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    val finishedAt = LocalDateTime(2026, 4, 25, 18, 30).toInstant(TimeZone.currentSystemDefault())
    mediaProgressRepository.progressFlow.value = finishedProgress(finishedAt = finishedAt.toEpochMilliseconds())

    presenter.test {
      awaitProgressSlot { true }
      latest.eventSink(LibraryItemUiEvent.EditListeningDate(ListeningDate.Finished))
      awaitProgressSlot { it.editingDate == ListeningDate.Finished }

      latest.eventSink(LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Finished, LocalDate(2026, 4, 20)))
      awaitProgressSlot { it.editingDate == null }

      assertThat(mediaProgressRepository.invocations.filterIsInstance<Invocation.SetFinishedAt>())
        .single()
        .given { invocation ->
          assertThat(invocation.libraryItemId).isEqualTo(TestLibraryItemId)
          val saved = invocation.finishedAt.asDateTime()
          assertThat(saved.date).isEqualTo(LocalDate(2026, 4, 20))
          assertThat(saved.hour).isEqualTo(18)
          assertThat(saved.minute).isEqualTo(30)
        }

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun aDateTheServerRefuses_isExplained() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    mediaProgressRepository.progressFlow.value = finishedProgress()
    mediaProgressRepository.setFinishedAtResult = Result.failure(IllegalStateException("offline"))

    presenter.test {
      awaitProgressSlot { true }
      latest.eventSink(LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Finished, LocalDate(2026, 4, 20)))

      var withError = awaitItem()
      while (withError.errorMessage == null) {
        withError = awaitItem()
      }
      assertThat(withError.errorMessage).isNotNull()

      withError.eventSink(LibraryItemUiEvent.ClearError)
      var cleared = awaitItem()
      while (cleared.errorMessage != null) {
        cleared = awaitItem()
      }

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun aFinishDateBeforeTheStart_isHeldToTheStart() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    val startedAt = LocalDateTime(2026, 4, 10, 21, 0).toInstant(TimeZone.currentSystemDefault())
    mediaProgressRepository.progressFlow.value = finishedProgress(
      startedAt = startedAt.toEpochMilliseconds(),
      finishedAt = LocalDateTime(2026, 4, 25, 9, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
    )

    presenter.test {
      awaitProgressSlot { true }
      latest.eventSink(LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Finished, LocalDate(2026, 4, 10)))

      assertThat(mediaProgressRepository.invocations.filterIsInstance<Invocation.SetFinishedAt>())
        .single()
        .given { assertThat(it.finishedAt).isEqualTo(startedAt.toEpochMilliseconds()) }

      cancelAndIgnoreRemainingEvents()
    }
  }

  private var latest: LibraryItemUiState = LibraryItemUiState(
    user = user("user_id"),
    libraryItem = null,
    contentState = LoadState.Loading,
    eventSink = {},
  )

  /** Waits for a state whose item progress bar matches [predicate], and keeps it as [latest]. */
  private suspend fun CircuitReceiveTurbine<LibraryItemUiState>.awaitProgressSlot(
    predicate: (ProgressSlot) -> Boolean,
  ): ProgressSlot {
    while (true) {
      val state = awaitItem()
      latest = state
      val slot = state.contentState.dataOrNull?.slots?.filterIsInstance<ProgressSlot>()?.firstOrNull()
      if (slot != null && predicate(slot)) return slot
    }
  }

  private fun finishedProgress(
    startedAt: Long = TestNow - 30L * DAY,
    finishedAt: Long? = TestNow - DAY,
  ) = mediaProgress(
    libraryItemId = TestLibraryItemId,
    currentTime = 100f,
    duration = 100f,
    progress = 1f,
    isFinished = true,
    startedAt = startedAt,
    finishedAt = finishedAt,
  )

  private companion object {
    const val DAY = 86_400_000L
  }
}
