// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.extensions.asDate
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.time.FatherTime
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.api.StartedAtNotChanged
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.listening_dates_not_restored
import campfire.features.libraries.ui.generated.resources.listening_dates_not_restored_retry
import campfire.features.libraries.ui.generated.resources.listening_dates_restored
import campfire.features.libraries.ui.generated.resources.listening_dates_save_failed
import campfire.features.libraries.ui.generated.resources.listening_dates_started_changed
import campfire.features.libraries.ui.generated.resources.listening_dates_started_failed
import campfire.features.libraries.ui.generated.resources.listening_dates_started_restored
import campfire.features.libraries.ui.generated.resources.listening_dates_started_server_kept
import com.slack.circuit.overlay.OverlayHost
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString

/**
 * Changes the dates a finished item's progress keeps of when it was listened to: the sheet
 * listing them, a picker for the new day, then the save.
 */
fun interface ListeningDatesEditor {

  /**
   * Walks through changing a listening date of [item], starting at the picker for [date], or at
   * the sheet listing the dates without one. Returns once the date is saved, or put away unchanged.
   *
   * The save carries on if the caller goes away part way through, such as the screen being left,
   * though whatever [snackbarHost] would have said of it is then dropped.
   */
  suspend fun edit(
    item: LibraryItem,
    date: ListeningDate?,
    overlayHost: OverlayHost,
    snackbarHost: SnackbarHostState,
  )
}

@ContributesBinding(UserScope::class)
@Inject
class DefaultListeningDatesEditor(
  private val mediaProgressRepository: MediaProgressRepository,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val analytics: Analytics,
  private val fatherTime: FatherTime,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : ListeningDatesEditor {

  override suspend fun edit(
    item: LibraryItem,
    date: ListeningDate?,
    overlayHost: OverlayHost,
    snackbarHost: SnackbarHostState,
  ) {
    // "Set date" can be tapped while marking the item finished is still on its way to the server
    val progress = withTimeoutOrNull(FinishedWait) {
      mediaProgressRepository.observeProgress(item.id).first { it?.isFinished == true }
    } ?: return

    val picked = date
      ?: overlayHost.showListeningDatesSheet(
        itemTitle = item.media.metadata.title,
        progress = progress,
        canChangeStarted = canChangeStarted(item.id),
      )
      ?: return

    when (picked) {
      ListeningDate.Finished -> changeFinished(item, progress, overlayHost, snackbarHost)
      ListeningDate.Started -> changeStarted(item, progress, overlayHost, snackbarHost)
    }
  }

  private suspend fun changeFinished(
    item: LibraryItem,
    progress: MediaProgress,
    overlayHost: OverlayHost,
    snackbarHost: SnackbarHostState,
  ) {
    analytics.send(ActionEvent("edit_listening_date", Click, extras = mapOf("date" to "finished")))
    val day = overlayHost.showFinishedDatePicker(progress, today = fatherTime.today()) ?: return

    analytics.send(ActionEvent("change_listening_date", Click, extras = mapOf("date" to "finished")))
    val finishedAt = finishedAtOn(
      date = day,
      previously = progress.finishedAt,
      startedAt = progress.startedAt,
      now = fatherTime.nowInEpochMillis(),
    )
    save { mediaProgressRepository.setFinishedAt(item.id, finishedAt) }
      .onFailure { snackbarHost.showSnackbar(getString(Res.string.listening_dates_save_failed)) }
  }

  private suspend fun changeStarted(
    item: LibraryItem,
    progress: MediaProgress,
    overlayHost: OverlayHost,
    snackbarHost: SnackbarHostState,
  ) {
    if (!canChangeStarted(item.id)) return
    analytics.send(ActionEvent("edit_listening_date", Click, extras = mapOf("date" to "started")))
    val day = overlayHost.showStartedDatePicker(progress, today = fatherTime.today()) ?: return

    val previously = progress.startedAt.takeIf { it > 0L }?.asDate()
    if (!overlayHost.showRecreateProgressDialog(from = previously, to = day)) return
    // Playing may have started while the dialogs were up
    if (!canChangeStarted(item.id)) return

    analytics.send(ActionEvent("change_listening_date", Click, extras = mapOf("date" to "started")))
    val startedAt = startedAtOn(
      date = day,
      previously = progress.startedAt,
      finishedAt = progress.finishedAt,
      now = fatherTime.nowInEpochMillis(),
    )
    val result = save { mediaProgressRepository.setStartedAt(item.id, startedAt) }
    val message = when (result.exceptionOrNull()) {
      null -> Res.string.listening_dates_started_changed
      is StartedAtNotChanged.Restored -> Res.string.listening_dates_started_restored
      is StartedAtNotChanged.ServerKept -> Res.string.listening_dates_started_server_kept
      is StartedAtNotChanged.NotRestored -> return offerRestore(item, snackbarHost)
      else -> Res.string.listening_dates_started_failed
    }
    snackbarHost.showSnackbar(getString(message))
  }

  /**
   * Offers to put back the progress that changing the start left off the server, for as long as
   * it can't be. This device still has it, so the next sync from playback would put it back too.
   */
  private suspend fun offerRestore(item: LibraryItem, snackbarHost: SnackbarHostState) {
    while (true) {
      val choice = snackbarHost.showSnackbar(
        message = getString(Res.string.listening_dates_not_restored),
        actionLabel = getString(Res.string.listening_dates_not_restored_retry),
        withDismissAction = true,
        duration = SnackbarDuration.Indefinite,
      )
      if (choice != SnackbarResult.ActionPerformed) return

      analytics.send(ActionEvent("restore_progress", Click))
      if (save { mediaProgressRepository.restoreProgress(item.id) }.isSuccess) {
        snackbarHost.showSnackbar(getString(Res.string.listening_dates_restored))
        return
      }
    }
  }

  /**
   * Whether the start can be changed now, which it can't while the item plays here: a sync from
   * playback could create the progress while it's being created again.
   */
  private fun canChangeStarted(libraryItemId: LibraryItemId): Boolean {
    val player = audioPlayerHolder.currentPlayer.value ?: return true
    return player.preparedSession?.libraryItem?.id != libraryItemId ||
      player.state.value != AudioPlayer.State.Playing
  }

  /** Runs [block] past the caller, so leaving the screen part way through doesn't cut it short. */
  private suspend fun <T> save(block: suspend () -> T): T {
    return coroutineScopeHolder.get().async { block() }.await()
  }
}

/** How long to wait for an item just marked finished to be finished here. */
private val FinishedWait = 10.seconds
