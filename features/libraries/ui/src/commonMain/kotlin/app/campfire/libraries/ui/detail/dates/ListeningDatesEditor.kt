// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import androidx.compose.material3.SnackbarHostState
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.time.FatherTime
import app.campfire.user.api.MediaProgressRepository
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.listening_dates_save_failed
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

    val picked = date ?: overlayHost.showListeningDatesSheet(item.media.metadata.title, progress) ?: return
    // Audiobookshelf only sets when a progress started as it creates it, so only the finish changes
    if (picked != ListeningDate.Finished) return

    analytics.send(ActionEvent("edit_listening_date", Click, extras = mapOf("date" to "finished")))
    val day = overlayHost.showFinishedDatePicker(progress, today = fatherTime.today()) ?: return

    analytics.send(ActionEvent("change_listening_date", Click, extras = mapOf("date" to "finished")))
    val finishedAt = finishedAtOn(
      date = day,
      previously = progress.finishedAt,
      startedAt = progress.startedAt,
      now = fatherTime.nowInEpochMillis(),
    )
    coroutineScopeHolder.get()
      .async { mediaProgressRepository.setFinishedAt(item.id, finishedAt) }
      .await()
      .onFailure { snackbarHost.showSnackbar(getString(Res.string.listening_dates_save_failed)) }
  }
}

/** How long to wait for an item just marked finished to be finished here. */
private val FinishedWait = 10.seconds
