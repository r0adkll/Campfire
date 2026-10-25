// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.LibraryRepository
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.api.match.MetadataMatchRepository
import app.campfire.management.api.match.QuickMatchResult
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.quick_match_changed
import campfire.features.libraries.ui.generated.resources.quick_match_changed_and_cover
import campfire.features.libraries.ui.generated.resources.quick_match_changed_cover
import campfire.features.libraries.ui.generated.resources.quick_match_failed
import campfire.features.libraries.ui.generated.resources.quick_match_no_match
import campfire.features.libraries.ui.generated.resources.quick_match_not_allowed
import campfire.features.libraries.ui.generated.resources.quick_match_nothing
import campfire.features.libraries.ui.generated.resources.quick_match_undo
import campfire.features.libraries.ui.generated.resources.quick_match_undo_failed
import campfire.features.libraries.ui.generated.resources.quick_match_undone
import campfire.features.libraries.ui.generated.resources.quick_match_updated
import campfire.features.libraries.ui.generated.resources.quick_match_working
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * Quick matches books from their menus: finds the source to ask, then has the server match a book
 * and says how it went, offering to put back the details it changed. A match runs on past the
 * screen or card it started from, as its search can take a while.
 */
@Inject
class QuickMatcher(
  private val libraryRepository: LibraryRepository,
  private val matchRepository: MetadataMatchRepository,
  private val libraryItemRepository: LibraryItemRepository,
  @ForScope(UserScope::class) private val userScope: CoroutineScopeHolder,
) {

  /**
   * The source a quick match of [item] asks: its library's preferred one, as the server's own
   * web app asks, by the name the server gives it.
   */
  suspend fun sourceFor(item: LibraryItem): MatchProvider {
    val id = libraryRepository.observeAllLibraries(refresh = false).first()
      .find { it.id == item.libraryId }
      ?.provider
      ?.takeIf { it.isNotBlank() }
      // What the server searches when it isn't told
      ?: DefaultSource
    // Only for its name, so a slow list doesn't keep the question waiting
    val name = withTimeoutOrNull(SourceNameTimeout) { matchRepository.getBookProviders().getOrNull() }
      ?.find { it.id == id }
      ?.name
    return MatchProvider(id, name ?: id)
  }

  /** Matches [item] against [source] as [choice] lets it, telling [snackbarHost] how it went. */
  fun start(
    item: LibraryItem,
    source: MatchProvider,
    choice: QuickMatchChoice,
    snackbarHost: SnackbarHostState,
  ): Job = userScope.get().launch {
    val title = item.media.metadata.title.orEmpty()
    Analytics.send(ActionEvent("quick_match_item", Click, choice.analyticsExtra))

    val working = launch {
      snackbarHost.showSnackbar(
        message = getString(Res.string.quick_match_working, title, source.name),
        duration = SnackbarDuration.Indefinite,
      )
    }
    val result = matchRepository.quickMatchBook(
      itemId = item.id,
      provider = source.id,
      replaceDetails = choice.replaceDetails,
      replaceCover = choice.replaceCover,
    )
    working.cancel()

    result.fold(
      onSuccess = { matched ->
        when (matched) {
          QuickMatchResult.NoMatch -> snackbarHost.showSnackbar(
            getString(Res.string.quick_match_no_match, title, source.name),
          )
          QuickMatchResult.NothingChanged -> snackbarHost.showSnackbar(
            getString(Res.string.quick_match_nothing, source.name, title),
          )
          is QuickMatchResult.Updated -> tellUpdated(item, title, source, matched, snackbarHost)
        }
      },
      onFailure = { error ->
        bark(throwable = error) { "Failed to quick match library item ${item.id}" }
        snackbarHost.showSnackbar(
          if (error is ItemChangeNotAllowedException) {
            getString(Res.string.quick_match_not_allowed)
          } else {
            getString(Res.string.quick_match_failed, title)
          },
        )
      },
    )
  }

  private suspend fun tellUpdated(
    item: LibraryItem,
    title: String,
    source: MatchProvider,
    matched: QuickMatchResult.Updated,
    snackbarHost: SnackbarHostState,
  ) {
    val undo = matched.undo
    val changed = undo?.changeCount ?: 0
    val message = when {
      changed > 0 && matched.coverChanged ->
        getPluralString(Res.plurals.quick_match_changed_and_cover, changed, changed, title, source.name)
      changed > 0 -> getPluralString(Res.plurals.quick_match_changed, changed, changed, title, source.name)
      matched.coverChanged -> getString(Res.string.quick_match_changed_cover, title, source.name)
      // The changed book couldn't be fetched to tell what changed
      else -> getString(Res.string.quick_match_updated, title, source.name)
    }
    // A new cover can't be put back
    if (undo == null || undo.isEmpty) {
      snackbarHost.showSnackbar(message)
      return
    }

    val choice = snackbarHost.showSnackbar(
      message = message,
      actionLabel = getString(Res.string.quick_match_undo),
      duration = SnackbarDuration.Long,
    )
    if (choice != SnackbarResult.ActionPerformed) return

    Analytics.send(ActionEvent("quick_match_item", "undone"))
    libraryItemRepository.updateBookDetails(item.id, undo)
      .onSuccess { snackbarHost.showSnackbar(getString(Res.string.quick_match_undone, title)) }
      .onFailure { error ->
        bark(throwable = error) { "Failed to undo the quick match of ${item.id}" }
        snackbarHost.showSnackbar(getString(Res.string.quick_match_undo_failed))
      }
  }
}

/** How many details it changes, not counting a cover */
private val BookDetailsUpdate.changeCount: Int
  get() = listOf(
    title, subtitle, description, publisher, publishedYear, language, isbn, asin,
    explicit, abridged, authors, narrators, series, genres, tags,
  ).count { it != null }

private val QuickMatchChoice.analyticsExtra: String
  get() = listOfNotNull(
    "details".takeIf { replaceDetails },
    "cover".takeIf { replaceCover },
  ).joinToString(",").ifEmpty { "fill" }

private const val DefaultSource = "google"
private val SourceNameTimeout = 3.seconds
