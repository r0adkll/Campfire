// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.common.test.mediaProgress
import app.campfire.common.test.session
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.extensions.asDateTime
import app.campfire.core.model.LibraryItem
import app.campfire.core.time.FatherTime
import app.campfire.home.ui.libraryItem
import app.campfire.user.api.StartedAtNotChanged
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.FakeMediaProgressRepository.Invocation
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.single
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayHostData
import com.slack.circuit.overlay.ReadOnlyOverlayApi
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class ListeningDatesEditorTest {

  private val repository = FakeMediaProgressRepository()
  private val analytics = FakeAnalytics()
  private val overlays = ScriptedOverlayHost()
  private val snackbarHost = SnackbarHostState()
  private val audioPlayerHolder = FakeAudioPlayerHolder()
  private val item = libraryItem()

  @Test
  fun `the sheet leads to the picker, and the new day keeps the old time of day`() = runTest {
    repository.progressFlow.value = finishedProgress(finishedAt = at(2026, 4, 25, 18, 30))
    overlays.sheetResult = ListeningDatesResult.Edit(ListeningDate.Finished)
    overlays.picked = DatePicked.Day(LocalDate(2026, 4, 20))

    editor().edit(item, date = null, overlays, snackbarHost)

    assertThat(overlays.shown.map { it::class }).isEqualTo(
      listOf(AdaptiveSheetOverlay::class, FinishedDatePickerOverlay::class),
    )
    assertThat(repository.invocations.filterIsInstance<Invocation.SetFinishedAt>())
      .single()
      .given { saved ->
        assertThat(saved.libraryItemId).isEqualTo(item.id)
        val savedAt = saved.finishedAt.asDateTime()
        assertThat(savedAt.date).isEqualTo(LocalDate(2026, 4, 20))
        assertThat(savedAt.hour).isEqualTo(18)
        assertThat(savedAt.minute).isEqualTo(30)
      }
  }

  @Test
  fun `setting the date of a just finished item goes straight to the picker`() = runTest {
    repository.progressFlow.value = finishedProgress()
    overlays.picked = DatePicked.Day(LocalDate(2026, 4, 20))

    editor().edit(item, ListeningDate.Finished, overlays, snackbarHost)

    assertThat(overlays.shown).single().isInstanceOf<FinishedDatePickerOverlay>()
    assertThat(repository.invocations.filterIsInstance<Invocation.SetFinishedAt>()).single()
  }

  @Test
  fun `putting the sheet away changes nothing`() = runTest {
    repository.progressFlow.value = finishedProgress()
    overlays.sheetResult = ListeningDatesResult.Dismissed

    editor().edit(item, date = null, overlays, snackbarHost)

    assertThat(overlays.shown).single().isInstanceOf<AdaptiveSheetOverlay<*, *>>()
    assertThat(repository.invocations.filterIsInstance<Invocation.SetFinishedAt>()).isEmpty()
  }

  @Test
  fun `putting the picker away changes nothing`() = runTest {
    repository.progressFlow.value = finishedProgress()
    overlays.picked = DatePicked.Dismissed

    editor().edit(item, ListeningDate.Finished, overlays, snackbarHost)

    assertThat(repository.invocations.filterIsInstance<Invocation.SetFinishedAt>()).isEmpty()
  }

  @Test
  fun `a new start waits on agreeing to recreate the progress, and keeps its time of day`() = runTest {
    repository.progressFlow.value = finishedProgress(startedAt = at(2026, 4, 2, 8, 45))
    overlays.sheetResult = ListeningDatesResult.Edit(ListeningDate.Started)
    overlays.picked = DatePicked.Day(LocalDate(2026, 3, 20))
    overlays.onShow = { overlay ->
      // Nothing is recreated before the user agrees to it
      if (overlay is RecreateProgressOverlay) {
        assertThat(repository.invocations.filterIsInstance<Invocation.SetStartedAt>()).isEmpty()
      }
    }

    backgroundScope.launch { editor().edit(item, date = null, overlays, snackbarHost) }

    awaitSnackbar("Start date changed")
    assertThat(overlays.shown.map { it::class }).isEqualTo(
      listOf(AdaptiveSheetOverlay::class, StartedDatePickerOverlay::class, RecreateProgressOverlay::class),
    )
    assertThat(repository.invocations.filterIsInstance<Invocation.SetStartedAt>())
      .single()
      .given { saved ->
        val savedAt = saved.startedAt.asDateTime()
        assertThat(savedAt.date).isEqualTo(LocalDate(2026, 3, 20))
        assertThat(savedAt.hour).isEqualTo(8)
        assertThat(savedAt.minute).isEqualTo(45)
      }
  }

  @Test
  fun `backing out of recreating the progress changes nothing`() = runTest {
    repository.progressFlow.value = finishedProgress()
    overlays.picked = DatePicked.Day(LocalDate(2026, 3, 20))
    overlays.confirmed = false

    editor().edit(item, ListeningDate.Started, overlays, snackbarHost)

    assertThat(overlays.shown.last()).isInstanceOf<RecreateProgressOverlay>()
    assertThat(repository.invocations.filterIsInstance<Invocation.SetStartedAt>()).isEmpty()
  }

  @Test
  fun `while the item plays, the start can't be changed`() = runTest {
    repository.progressFlow.value = finishedProgress()
    play(item)
    overlays.sheetResult = ListeningDatesResult.Edit(ListeningDate.Started)

    editor().edit(item, date = null, overlays, snackbarHost)

    assertThat(overlays.shown).single().isInstanceOf<AdaptiveSheetOverlay<*, *>>()
  }

  @Test
  fun `another item playing doesn't stop the start changing`() = runTest {
    repository.progressFlow.value = finishedProgress()
    play(libraryItem(id = "another_item"))
    overlays.picked = DatePicked.Day(LocalDate(2026, 3, 20))

    backgroundScope.launch { editor().edit(item, ListeningDate.Started, overlays, snackbarHost) }

    awaitSnackbar("Start date changed")
  }

  @Test
  fun `playing started while the dialogs were up stops the start changing`() = runTest {
    repository.progressFlow.value = finishedProgress()
    overlays.picked = DatePicked.Day(LocalDate(2026, 3, 20))
    overlays.onShow = { overlay -> if (overlay is RecreateProgressOverlay) play(item) }

    editor().edit(item, ListeningDate.Started, overlays, snackbarHost)

    assertThat(repository.invocations.filterIsInstance<Invocation.SetStartedAt>()).isEmpty()
  }

  @Test
  fun `how far a refused start got is told`() = runTest {
    repository.progressFlow.value = finishedProgress()
    overlays.picked = DatePicked.Day(LocalDate(2026, 3, 20))
    val told = mapOf(
      StartedAtNotChanged.Restored() to "Couldn't change the start date, so your progress was put back as it was.",
      StartedAtNotChanged.ServerKept() to "Your server kept its own start date. Try again in a moment.",
      StartedAtNotChanged.Unchanged() to "Couldn't change the start date. Check your connection and try again.",
    )

    for ((failure, message) in told) {
      repository.setStartedAtResult = Result.failure(failure)
      val editing = backgroundScope.launch { editor().edit(item, ListeningDate.Started, overlays, snackbarHost) }
      awaitSnackbar(message)
      snackbarHost.currentSnackbarData?.dismiss()
      editing.join()
    }
  }

  @Test
  fun `a progress left off the server can be put back`() = runTest {
    repository.progressFlow.value = finishedProgress()
    repository.setStartedAtResult = Result.failure(StartedAtNotChanged.NotRestored())
    repository.restoreProgressResult = Result.failure(IllegalStateException("offline"))
    overlays.picked = DatePicked.Day(LocalDate(2026, 3, 20))

    backgroundScope.launch { editor().edit(item, ListeningDate.Started, overlays, snackbarHost) }

    val notRestored = awaitSnackbar("This book's progress couldn't be put back on your server")
    assertThat(notRestored.visuals.actionLabel).isEqualTo("Try again")
    assertThat(notRestored.visuals.duration).isEqualTo(SnackbarDuration.Indefinite)
    notRestored.performAction()

    // Still off the server, so it's offered again
    awaitRestoreAttempts(1)
    repository.restoreProgressResult = Result.success(Unit)
    awaitSnackbar("This book's progress couldn't be put back on your server").performAction()

    awaitSnackbar("Progress put back on your server")
    assertThat(repository.invocations.filterIsInstance<Invocation.RestoreProgress>())
      .transform { restores -> restores.map { it.libraryItemId } }
      .isEqualTo(listOf(item.id, item.id))
  }

  @Test
  fun `a finish date before the start is held to the start`() = runTest {
    val startedAt = at(2026, 4, 10, 21, 0)
    repository.progressFlow.value = finishedProgress(startedAt = startedAt, finishedAt = at(2026, 4, 25, 9, 0))
    overlays.picked = DatePicked.Day(LocalDate(2026, 4, 10))

    editor().edit(item, ListeningDate.Finished, overlays, snackbarHost)

    assertThat(repository.invocations.filterIsInstance<Invocation.SetFinishedAt>())
      .single()
      .given { assertThat(it.finishedAt).isEqualTo(startedAt) }
  }

  @Test
  fun `setting the date waits for marking the item finished to land`() = runTest {
    repository.progressFlow.value = mediaProgress(item.id, currentTime = 20f)
    overlays.picked = DatePicked.Day(LocalDate(2026, 4, 20))

    val editing = launch { editor().edit(item, ListeningDate.Finished, overlays, snackbarHost) }
    advanceTimeBy(2.seconds)
    assertThat(overlays.shown).isEmpty()

    repository.progressFlow.value = finishedProgress()
    editing.join()

    assertThat(overlays.shown).single().isInstanceOf<FinishedDatePickerOverlay>()
  }

  @Test
  fun `an item that doesn't finish opens nothing`() = runTest {
    repository.progressFlow.value = mediaProgress(item.id, currentTime = 20f)

    editor().edit(item, ListeningDate.Finished, overlays, snackbarHost)

    assertThat(overlays.shown).isEmpty()
  }

  @Test
  fun `a date the server refuses is told`() = runTest {
    repository.progressFlow.value = finishedProgress()
    repository.setFinishedAtResult = Result.failure(IllegalStateException("offline"))
    overlays.picked = DatePicked.Day(LocalDate(2026, 4, 20))

    // The snackbar waits on being put away, which nothing here does
    backgroundScope.launch { editor().edit(item, ListeningDate.Finished, overlays, snackbarHost) }

    awaitSnackbar("Couldn't save the date. Check your connection and try again.")
  }

  @Test
  fun `the save carries on when the screen is left`() = runTest {
    repository.progressFlow.value = finishedProgress()
    repository.setFinishedAtGate = CompletableDeferred()
    overlays.picked = DatePicked.Day(LocalDate(2026, 4, 20))

    val editing = launch { editor().edit(item, ListeningDate.Finished, overlays, snackbarHost) }
    runCurrent()
    editing.cancel()
    repository.setFinishedAtGate?.complete(Unit)
    runCurrent()

    assertThat(repository.setFinishedAtCompletions).isEqualTo(1)
  }

  private fun TestScope.editor() = DefaultListeningDatesEditor(
    mediaProgressRepository = repository,
    audioPlayerHolder = audioPlayerHolder,
    analytics = analytics,
    fatherTime = FixedFatherTime(TestNow),
    coroutineScopeHolder = CoroutineScopeHolder { backgroundScope },
  )

  private fun finishedProgress(
    startedAt: Long = TestNow - 30L * DAY,
    finishedAt: Long? = TestNow - DAY,
  ) = mediaProgress(
    libraryItemId = item.id,
    currentTime = 100f,
    duration = 100f,
    progress = 1f,
    isFinished = true,
    startedAt = startedAt,
    finishedAt = finishedAt,
  )

  private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    LocalDateTime(year, month, day, hour, minute).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

  private fun play(playing: LibraryItem) {
    audioPlayerHolder.currentPlayer.value = FakeAudioPlayer().apply {
      preparedSession = session(libraryItem = playing)
      state.value = AudioPlayer.State.Playing
    }
  }

  /** Waits for the snackbar saying [message]: its words load off the test's own thread. */
  private suspend fun awaitSnackbar(message: String): SnackbarData = withContext(Dispatchers.Default) {
    withTimeout(5.seconds) {
      while (snackbarHost.currentSnackbarData?.visuals?.message != message) delay(10)
      checkNotNull(snackbarHost.currentSnackbarData)
    }
  }

  private suspend fun awaitRestoreAttempts(count: Int) = withContext(Dispatchers.Default) {
    withTimeout(5.seconds) {
      while (repository.invocations.filterIsInstance<Invocation.RestoreProgress>().size < count) delay(10)
    }
  }

  private companion object {
    /** Noon UTC on 1 May 2026, when the tests take "now" to be. */
    const val TestNow = 1_777_636_800_000L
    const val DAY = 86_400_000L
  }
}

/** Answers the dates sheet, the pickers and the confirmation with whatever each is set to. */
@OptIn(ReadOnlyOverlayApi::class)
private class ScriptedOverlayHost : OverlayHost {
  val shown = mutableListOf<Overlay<*>>()
  var sheetResult: ListeningDatesResult = ListeningDatesResult.Dismissed
  var picked: DatePicked = DatePicked.Dismissed
  var confirmed: Boolean = true
  var onShow: (Overlay<*>) -> Unit = {}

  override val currentOverlayData: OverlayHostData<Any>? = null

  @Suppress("UNCHECKED_CAST")
  override suspend fun <Result : Any> show(overlay: Overlay<Result>): Result {
    shown += overlay
    onShow(overlay)
    return when (overlay) {
      is FinishedDatePickerOverlay, is StartedDatePickerOverlay -> picked
      is RecreateProgressOverlay -> confirmed
      else -> sheetResult
    } as Result
  }
}

private class FixedFatherTime(private val nowMillis: Long) : FatherTime {
  override fun now(): LocalDateTime = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(TimeZone.UTC)
  override fun today(): LocalDate = now().date
  override fun nowInEpochMillis(): Long = nowMillis
}
