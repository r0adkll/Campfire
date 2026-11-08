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
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.management.api.screen.ItemEditorScreen
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
  @ForScope(UserScope::class) private val userScope: CoroutineScopeHolder,
) : Presenter<ItemEditorUiState> {

  @Composable
  override fun present(): ItemEditorUiState {
    val scope = rememberRetainedCoroutineScope()
    // The app's own, which outlasts this screen to offer undoing a save, and says why one failed
    val snackbarHost = LocalSnackBarHost.current

    val user by userRepository.userFlow.collectAsState()
    val item by remember { observeItem() }.collectAsState(LoadState.Loading)
    val latest = item.dataOrNull?.media?.takeIf { it is Media.Book }?.metadata

    var draft by rememberRetained { mutableStateOf<BookDetailsDraft?>(null) }
    var isSaving by rememberRetained { mutableStateOf(false) }
    var isConfirmingDiscard by rememberRetained { mutableStateOf(false) }
    var keptChangesOver by rememberRetained { mutableStateOf<Media.Metadata?>(null) }

    LaunchedEffect(latest) {
      if (draft == null && latest != null) draft = BookDetailsDraft(latest)
    }

    val isBehindServer = draft.let { current ->
      current != null && latest != null && !isSaving && latest != keptChangesOver && current.isBehind(latest)
    }

    return ItemEditorUiState(
      item = item,
      draft = draft,
      canEdit = user.canUpdateItems,
      canSeeExplicit = user.permissions.accessExplicitContent,
      isSaving = isSaving,
      isBehindServer = isBehindServer,
      isConfirmingDiscard = isConfirmingDiscard,
    ) { event ->
      when (event) {
        ItemEditorUiEvent.Save -> {
          val current = draft ?: return@ItemEditorUiState
          val update = current.update()
          if (isSaving || !current.isValid || update.isEmpty) return@ItemEditorUiState

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
          val details = latest ?: draft?.base ?: return@ItemEditorUiState
          Analytics.send(ActionEvent("edit_item", "reset"))
          draft = BookDetailsDraft(details)
          keptChangesOver = null
        }

        ItemEditorUiEvent.KeepChanges -> keptChangesOver = latest
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
).joinToString(",")
