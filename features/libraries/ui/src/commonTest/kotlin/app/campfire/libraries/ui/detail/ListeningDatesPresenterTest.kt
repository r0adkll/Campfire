// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.audioplayer.AudioPlayer
import app.campfire.common.test.mediaProgress
import app.campfire.common.test.session
import app.campfire.common.test.user
import app.campfire.core.coroutines.LoadState
import app.campfire.core.extensions.asDateTime
import app.campfire.libraries.ui.detail.composables.slots.ProgressSlot
import app.campfire.libraries.ui.detail.dates.ListeningDate
import app.campfire.user.api.StartedAtNotChanged
import app.campfire.user.test.FakeMediaProgressRepository.Invocation
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
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
      while (withError.message == null) {
        withError = awaitItem()
      }
      assertThat(withError.message).isNotNull()

      withError.eventSink(LibraryItemUiEvent.ClearMessage)
      var cleared = awaitItem()
      while (cleared.message != null) {
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

  @Test
  fun aNewStart_waitsOnConfirmationBeforeRecreatingTheProgress() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    val startedAt = LocalDateTime(2026, 4, 2, 8, 45).toInstant(TimeZone.currentSystemDefault())
    mediaProgressRepository.progressFlow.value = finishedProgress(startedAt = startedAt.toEpochMilliseconds())

    presenter.test {
      assertThat(awaitProgressSlot { true }.canChangeStarted).isTrue()

      latest.eventSink(LibraryItemUiEvent.EditListeningDate(ListeningDate.Started))
      awaitProgressSlot { it.editingDate == ListeningDate.Started }

      latest.eventSink(LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Started, LocalDate(2026, 3, 20)))
      awaitProgressSlot { it.confirmingStartedOn == LocalDate(2026, 3, 20) && it.editingDate == null }
      assertThat(mediaProgressRepository.invocations.filterIsInstance<Invocation.SetStartedAt>()).isEmpty()

      latest.eventSink(LibraryItemUiEvent.ConfirmStartedDate)
      val confirmed = awaitUntil { it.message != null }
      assertThat(confirmed.message).isEqualTo("Start date changed")

      assertThat(mediaProgressRepository.invocations.filterIsInstance<Invocation.SetStartedAt>())
        .single()
        .given { invocation ->
          val saved = invocation.startedAt.asDateTime()
          assertThat(saved.date).isEqualTo(LocalDate(2026, 3, 20))
          assertThat(saved.hour).isEqualTo(8)
          assertThat(saved.minute).isEqualTo(45)
        }

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun backingOutOfTheConfirmation_changesNothing() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    mediaProgressRepository.progressFlow.value = finishedProgress()

    presenter.test {
      awaitProgressSlot { true }
      latest.eventSink(LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Started, LocalDate(2026, 3, 20)))
      awaitProgressSlot { it.confirmingStartedOn != null }

      latest.eventSink(LibraryItemUiEvent.DismissListeningDate)
      awaitProgressSlot { it.confirmingStartedOn == null }
      latest.eventSink(LibraryItemUiEvent.ConfirmStartedDate)

      assertThat(mediaProgressRepository.invocations.filterIsInstance<Invocation.SetStartedAt>()).isEmpty()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun whilePlaying_theStartCantBeChanged() = runTest {
    val item = emptyLibraryItem()
    libraryItemRepository.libraryItemFlow.emit(item)
    mediaProgressRepository.progressFlow.value = finishedProgress()
    audioPlayer.preparedSession = session(libraryItem = item)
    audioPlayer.state.value = AudioPlayer.State.Playing
    audioPlayerHolder.currentPlayer.value = audioPlayer

    presenter.test {
      val playing = awaitProgressSlot { !it.canChangeStarted }

      latest.eventSink(LibraryItemUiEvent.EditListeningDate(ListeningDate.Started))
      audioPlayer.state.value = AudioPlayer.State.Paused

      // Paused again, the start can change, but the tap made while playing opened nothing
      val paused = awaitProgressSlot { it.canChangeStarted }
      assertThat(paused.editingDate).isNull()
      assertThat(playing.canEditDates).isTrue()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun aProgressLeftOffTheServer_canBePutBack() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    mediaProgressRepository.progressFlow.value = finishedProgress()
    mediaProgressRepository.setStartedAtResult = Result.failure(StartedAtNotChanged.NotRestored())

    presenter.test {
      awaitProgressSlot { true }
      latest.eventSink(LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Started, LocalDate(2026, 3, 20)))
      awaitProgressSlot { it.confirmingStartedOn != null }
      latest.eventSink(LibraryItemUiEvent.ConfirmStartedDate)

      val notRestored = awaitProgressSlot { it.progressNotRestored }
      // The row under the bar says so, rather than a snackbar too
      assertThat(latest.message).isNull()
      assertThat(notRestored.canEditDates).isTrue()

      latest.eventSink(LibraryItemUiEvent.RestoreProgress)
      awaitProgressSlot { !it.progressNotRestored }

      assertThat(mediaProgressRepository.invocations.filterIsInstance<Invocation.RestoreProgress>())
        .single()
        .prop(Invocation.RestoreProgress::libraryItemId)
        .isEqualTo(TestLibraryItemId)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun aStartPutBackAsItWas_isExplained() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    mediaProgressRepository.progressFlow.value = finishedProgress()
    mediaProgressRepository.setStartedAtResult = Result.failure(StartedAtNotChanged.Restored())

    presenter.test {
      awaitProgressSlot { true }
      latest.eventSink(LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Started, LocalDate(2026, 3, 20)))
      awaitProgressSlot { it.confirmingStartedOn != null }
      latest.eventSink(LibraryItemUiEvent.ConfirmStartedDate)

      val explained = awaitUntil { it.message != null }
      assertThat(explained.message).isEqualTo(
        "Couldn't change the start date, so your progress was put back as it was.",
      )

      cancelAndIgnoreRemainingEvents()
    }
  }

  /** Waits for a state matching [predicate], and keeps it as [latest]. */
  private suspend fun CircuitReceiveTurbine<LibraryItemUiState>.awaitUntil(
    predicate: (LibraryItemUiState) -> Boolean,
  ): LibraryItemUiState {
    while (true) {
      val state = awaitItem()
      latest = state
      if (predicate(state)) return state
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
