// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.filters.FilteringRepository
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.management.api.screen.ItemEditorScreen
import app.campfire.management.ui.cover.CoverPresenter
import app.campfire.management.ui.match.MatchPresenter
import app.campfire.management.ui.match.fillFrom
import app.campfire.user.api.UserRepository
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.action_undo
import campfire.features.management.ui.generated.resources.editor_already_saved
import campfire.features.management.ui.generated.resources.editor_save_failed
import campfire.features.management.ui.generated.resources.editor_save_not_allowed
import campfire.features.management.ui.generated.resources.editor_saved
import campfire.features.management.ui.generated.resources.editor_undo_failed
import campfire.features.management.ui.generated.resources.editor_undone
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@CircuitInject(ItemEditorScreen::class, UserScope::class)
@Inject
class ItemEditorPresenter(
  private val screen: ItemEditorScreen,
  private val navigator: Navigator,
  private val libraryItemRepository: LibraryItemRepository,
  private val userRepository: UserRepository,
  private val filteringRepository: FilteringRepository,
  private val coverPresenter: CoverPresenter,
  private val matchPresenter: MatchPresenter,
  @ForScope(UserScope::class) private val userScope: CoroutineScopeHolder,
) : Presenter<ItemEditorUiState> {

  @Composable
  override fun present(): ItemEditorUiState {
    val scope = rememberRetainedCoroutineScope()
    // The app's own, which outlasts this screen to offer undoing a save, and says why one failed
    val snackbarHost = LocalSnackBarHost.current

    val user by userRepository.userFlow.collectAsState()
    val item by remember { observeItem() }.collectAsState(LoadState.Loading)
    val latestItem = item.dataOrNull
    val latest = latestItem?.media as? Media.Book

    val libraryId = latestItem?.libraryId
    val suggestions by remember(libraryId) { observeSuggestions(libraryId) }.collectAsState(DetailSuggestions())

    var draft by rememberRetained { mutableStateOf<BookDetailsDraft?>(null) }
    var isSaving by rememberRetained { mutableStateOf(false) }
    var isConfirmingDiscard by rememberRetained { mutableStateOf(false) }
    var keptChangesOver by rememberRetained { mutableStateOf<Media.Book?>(null) }
    var isLoadFailed by rememberRetained { mutableStateOf(false) }
    var loadAttempt by rememberRetained { mutableIntStateOf(0) }
    // When the server last changed the item editing started from
    var editingSince by rememberRetained { mutableLongStateOf(0L) }
    var section by rememberRetained { mutableStateOf(EditorSection.Details) }
    var filledFrom by rememberRetained { mutableStateOf<String?>(null) }

    // Editing starts from the server's own copy. A cached one can be a list's, with no authors,
    // narrators or series to edit, and saving a list replaces the server's whole list.
    LaunchedEffect(loadAttempt) {
      if (draft != null) return@LaunchedEffect
      isLoadFailed = false
      libraryItemRepository.fetchLibraryItem(screen.libraryItemId)
        .onSuccess { fetched ->
          val book = fetched.media as? Media.Book
          if (book == null) {
            isLoadFailed = true
            return@onSuccess
          }
          draft = book.asDraft()
          editingSince = fetched.updatedAtMillis
        }
        .onFailure { error ->
          bark(throwable = error) { "Failed to fetch ${screen.libraryItemId} to edit" }
          isLoadFailed = true
        }
    }

    // Only a copy the server changed since counts, not however an older cached one differs
    val isBehindServer = draft.let { current ->
      current != null &&
        latestItem != null &&
        latest != null &&
        !isSaving &&
        latestItem.updatedAtMillis > editingSince &&
        latest != keptChangesOver &&
        current.isBehind(latest.metadata, latest.tags)
    }

    fun save() {
      val current = draft ?: return
      val update = current.update()
      if (isSaving || !current.isValid || update.isEmpty) return

      isSaving = true
      val title = current.title.text.toString().trim()
      scope.launch {
        libraryItemRepository.updateBookDetails(screen.libraryItemId, update)
          .onSuccess { changed ->
            Analytics.send(ActionEvent("edit_item", "saved", update.analyticsFields()))
            navigator.pop()
            // On past this screen, which is gone once the save is told
            userScope.get().launch {
              tellSaved(snackbarHost, title, changed, undo = current.undoOf(update))
            }
          }
          .onFailure { error ->
            bark(throwable = error) { "Failed to save the details of ${screen.libraryItemId}" }
            isSaving = false
            snackbarHost.showSnackbar(
              getString(
                if (error is ItemChangeNotAllowedException) {
                  Res.string.editor_save_not_allowed
                } else {
                  Res.string.editor_save_failed
                },
              ),
            )
          }
      }
    }

    val cover = coverPresenter.present(
      item = latestItem,
      draft = draft,
      isActive = section == EditorSection.Cover,
      canUseAddresses = user.canUploadFiles,
      canRemove = user.canDeleteItems,
    )

    val match = matchPresenter.present(
      item = latestItem,
      draft = draft,
      isActive = section == EditorSection.Match,
      canChangeCover = user.canUploadFiles,
    ) { result, fields, merges, providerName, andSave ->
      section = EditorSection.Details
      draft?.fillFrom(result, fields, merges)
      // Once the form's filled, to say where from
      filledFrom = providerName
      if (andSave) save()
    }

    return ItemEditorUiState(
      item = item,
      draft = draft,
      section = section,
      cover = cover,
      match = match,
      filledFrom = filledFrom,
      canEdit = user.canUpdateItems,
      canSeeExplicit = user.permissions.accessExplicitContent,
      suggestions = suggestions,
      isSaving = isSaving,
      isBehindServer = isBehindServer,
      isConfirmingDiscard = isConfirmingDiscard,
      isLoadFailed = isLoadFailed,
    ) { event ->
      when (event) {
        ItemEditorUiEvent.Save -> save()

        ItemEditorUiEvent.Close -> {
          if (draft?.changes.isNullOrEmpty()) {
            navigator.pop()
          } else {
            isConfirmingDiscard = true
          }
        }

        ItemEditorUiEvent.ConfirmDiscard -> {
          isConfirmingDiscard = false
          Analytics.send(ActionEvent("edit_item", "discarded"))
          navigator.pop()
        }

        ItemEditorUiEvent.CancelDiscard -> isConfirmingDiscard = false

        ItemEditorUiEvent.ResetChanges -> {
          if (draft == null || isSaving) return@ItemEditorUiState
          Analytics.send(ActionEvent("edit_item", "reset"))
          // From the server's own copy again, as editing started, not however the cache has it
          draft = null
          keptChangesOver = null
          filledFrom = null
          loadAttempt++
        }

        ItemEditorUiEvent.KeepChanges -> keptChangesOver = latest

        ItemEditorUiEvent.RetryLoad -> loadAttempt++

        is ItemEditorUiEvent.ShowSection -> {
          if (event.section != EditorSection.Details && !user.canUpdateItems) return@ItemEditorUiState
          section = event.section
        }

        ItemEditorUiEvent.DismissFilledFrom -> filledFrom = null
      }
    }
  }

  private fun observeItem(): Flow<LoadState<out LibraryItem>> {
    return libraryItemRepository.observeLibraryItem(screen.libraryItemId)
      .map<LibraryItem, LoadState<out LibraryItem>> { LoadState.Loaded(it) }
      .catch { error ->
        bark(throwable = error) { "Failed to load ${screen.libraryItemId} to edit" }
        emit(LoadState.Error)
      }
  }

  private fun observeSuggestions(libraryId: LibraryId?): Flow<DetailSuggestions> {
    if (libraryId == null) return flowOf(DetailSuggestions())
    return filteringRepository.observeFilterData(libraryId)
      .map { it.asSuggestions() }
      .catch { error ->
        bark(throwable = error) { "Failed to load the names in $libraryId to suggest" }
        emit(DetailSuggestions())
      }
  }

  private suspend fun tellSaved(
    snackbarHost: SnackbarHostState,
    title: String,
    changed: Boolean,
    undo: BookDetailsUpdate,
  ) {
    if (!changed) {
      snackbarHost.showSnackbar(getString(Res.string.editor_already_saved))
      return
    }
    // Only a new cover changed, which can't be put back
    if (undo.isEmpty) {
      snackbarHost.showSnackbar(getString(Res.string.editor_saved, title))
      return
    }

    val result = snackbarHost.showSnackbar(
      message = getString(Res.string.editor_saved, title),
      actionLabel = getString(Res.string.action_undo),
      duration = SnackbarDuration.Long,
    )
    if (result != SnackbarResult.ActionPerformed) return

    Analytics.send(ActionEvent("edit_item", "undone"))
    libraryItemRepository.updateBookDetails(screen.libraryItemId, undo)
      .onSuccess { snackbarHost.showSnackbar(getString(Res.string.editor_undone, title)) }
      .onFailure { error ->
        bark(throwable = error) { "Failed to undo the details of ${screen.libraryItemId}" }
        snackbarHost.showSnackbar(getString(Res.string.editor_undo_failed))
      }
  }
}

/** Which details a save changed, by name, for analytics */
private fun BookDetailsUpdate.analyticsFields(): String = listOfNotNull(
  title?.let { "title" },
  subtitle?.let { "subtitle" },
  description?.let { "description" },
  publisher?.let { "publisher" },
  publishedYear?.let { "year" },
  language?.let { "language" },
  isbn?.let { "isbn" },
  asin?.let { "asin" },
  explicit?.let { "explicit" },
  abridged?.let { "abridged" },
  authors?.let { "authors" },
  narrators?.let { "narrators" },
  series?.let { "series" },
  genres?.let { "genres" },
  tags?.let { "tags" },
  cover?.let { "cover" },
).joinToString(",")

private fun Media.Book.asDraft() = BookDetailsDraft(metadata, tags)
