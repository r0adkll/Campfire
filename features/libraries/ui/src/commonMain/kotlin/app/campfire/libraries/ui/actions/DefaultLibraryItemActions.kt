// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.actions

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.common.compose.widgets.LibraryItemActions
import app.campfire.common.compose.widgets.LibraryItemActionsFactory
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.RescanResult
import app.campfire.libraries.ui.detail.manage.ItemAction
import app.campfire.libraries.ui.detail.manage.QuickMatcher
import app.campfire.libraries.ui.detail.manage.itemActions
import app.campfire.management.api.screen.ItemEditorScreen
import app.campfire.user.api.UserRepository
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.item_delete_failed
import campfire.features.libraries.ui.generated.resources.item_delete_not_allowed
import campfire.features.libraries.ui.generated.resources.item_deleted_files
import campfire.features.libraries.ui.generated.resources.item_removed
import campfire.features.libraries.ui.generated.resources.item_rescan_failed
import campfire.features.libraries.ui.generated.resources.item_rescan_not_allowed
import campfire.features.libraries.ui.generated.resources.item_rescan_nothing
import campfire.features.libraries.ui.generated.resources.item_rescan_up_to_date
import campfire.features.libraries.ui.generated.resources.item_rescan_updated
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Makes [DefaultLibraryItemActions] for the app's navigator. One per user, so the actions the root
 * makes from it, and provides to everything under it, stay the same while the root recomposes.
 */
@ContributesBinding(UserScope::class)
@SingleIn(UserScope::class)
@Inject
class DefaultLibraryItemActionsFactory(
  private val userRepository: UserRepository,
  private val libraryItemRepository: LibraryItemRepository,
  private val quickMatcher: QuickMatcher,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : LibraryItemActionsFactory {

  override fun create(navigator: Navigator): LibraryItemActions = DefaultLibraryItemActions(
    navigator = navigator,
    userRepository = userRepository,
    libraryItemRepository = libraryItemRepository,
    quickMatcher = quickMatcher,
    coroutineScopeHolder = coroutineScopeHolder,
  )
}

/**
 * The actions a library item's card offers on a long press or a right click: the ones the server
 * would take from the signed-in account, as the detail screen's menu has. The work runs on past
 * the card, which a delete takes out of its list.
 */
class DefaultLibraryItemActions(
  private val navigator: Navigator,
  private val userRepository: UserRepository,
  private val libraryItemRepository: LibraryItemRepository,
  private val quickMatcher: QuickMatcher,
  private val coroutineScopeHolder: CoroutineScopeHolder,
) : LibraryItemActions {

  @Composable
  override fun canAct(item: LibraryItem): Boolean {
    val user by userRepository.userFlow.collectAsState()
    return user.itemActions(item).isNotEmpty()
  }

  override suspend fun show(
    item: LibraryItem,
    pointer: Offset?,
    overlayHost: OverlayHost,
    snackbarHost: SnackbarHostState,
  ) {
    val actions = userRepository.userFlow.value.itemActions(item)
    if (actions.isEmpty()) return

    val action = if (pointer != null) {
      overlayHost.showItemActionsMenu(actions, pointer)
    } else {
      overlayHost.showItemActionsSheet(item, actions)
    } ?: return

    when (action) {
      ItemAction.Edit -> {
        Analytics.send(ActionEvent("edit_item", Click))
        navigator.goTo(ItemEditorScreen(item.id))
      }

      ItemAction.QuickMatch -> {
        val source = quickMatcher.sourceFor(item)
        val choice = overlayHost.showQuickMatchDialog(item.media.metadata.title.orEmpty(), source.name) ?: return
        quickMatcher.start(item, source, choice, snackbarHost)
      }

      ItemAction.Rescan -> {
        Analytics.send(ActionEvent("rescan_item", Click))
        coroutineScopeHolder.get().launch {
          snackbarHost.showSnackbar(rescan(item))
        }
      }

      ItemAction.Delete -> {
        val hardDelete = overlayHost.showDeleteItemDialog(item.media.metadata.title.orEmpty()) ?: return
        Analytics.send(ActionEvent("delete_item", Click, if (hardDelete) "hard" else "soft"))
        coroutineScopeHolder.get().launch {
          snackbarHost.showSnackbar(delete(item, hardDelete))
        }
      }
    }
  }

  private suspend fun rescan(item: LibraryItem): String {
    return libraryItemRepository.rescanLibraryItem(item.id).fold(
      onSuccess = { result ->
        getString(
          when (result) {
            RescanResult.Updated -> Res.string.item_rescan_updated
            RescanResult.UpToDate -> Res.string.item_rescan_up_to_date
            RescanResult.Nothing -> Res.string.item_rescan_nothing
          },
        )
      },
      onFailure = { error ->
        bark(throwable = error) { "Failed to rescan library item ${item.id}" }
        getString(
          if (error is ItemChangeNotAllowedException) {
            Res.string.item_rescan_not_allowed
          } else {
            Res.string.item_rescan_failed
          },
        )
      },
    )
  }

  private suspend fun delete(item: LibraryItem, hardDelete: Boolean): String {
    val title = item.media.metadata.title.orEmpty()
    return libraryItemRepository.deleteLibraryItem(item.id, hardDelete).fold(
      onSuccess = {
        getString(if (hardDelete) Res.string.item_deleted_files else Res.string.item_removed, title)
      },
      onFailure = { error ->
        bark(throwable = error) { "Failed to delete library item ${item.id}" }
        getString(
          if (error is ItemChangeNotAllowedException) {
            Res.string.item_delete_not_allowed
          } else {
            Res.string.item_delete_failed
          },
        )
      },
    )
  }
}
