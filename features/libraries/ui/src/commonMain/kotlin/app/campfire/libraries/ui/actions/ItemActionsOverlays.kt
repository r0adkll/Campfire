// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.collections.api.ui.AddToCollectionDialog
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.DownloadDone
import app.campfire.common.compose.icons.rounded.LibraryAdd
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.icons.rounded.MenuBook
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.PlaylistAdd
import app.campfire.common.compose.icons.rounded.PlaylistAddCheck
import app.campfire.common.compose.icons.rounded.QueuePlayNext
import app.campfire.common.compose.icons.rounded.Replay
import app.campfire.common.compose.icons.rounded.StopCircle
import app.campfire.common.compose.permission.PermissionState
import app.campfire.common.compose.permission.rememberPostNotificationPermissionState
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.coverImageShape
import app.campfire.common.compose.widgets.dialog.ConfirmDownloadDialog
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.common.compose.widgets.sheets.LocalSheetPresentation
import app.campfire.common.compose.widgets.sheets.SheetPresentation
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.ui.detail.manage.DeleteItemDialog
import app.campfire.libraries.ui.detail.manage.ItemAction
import app.campfire.libraries.ui.detail.manage.ItemActionsMenu
import app.campfire.libraries.ui.detail.manage.QuickMatchChoice
import app.campfire.libraries.ui.detail.manage.QuickMatchDialog
import app.campfire.libraries.ui.detail.manage.icon
import app.campfire.libraries.ui.detail.manage.label
import app.campfire.libraries.ui.detail.manage.testTag
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.playlists.api.dialog.PlaylistDialogResult
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_add_to_playlist_long
import campfire.features.libraries.ui.generated.resources.action_add_to_playlist_short
import campfire.features.libraries.ui.generated.resources.action_play
import campfire.features.libraries.ui.generated.resources.action_read
import campfire.features.libraries.ui.generated.resources.action_resume_listening
import campfire.features.libraries.ui.generated.resources.action_resume_reading
import campfire.features.libraries.ui.generated.resources.action_stop_downloading
import campfire.features.libraries.ui.generated.resources.ebook_format_and_size
import campfire.features.libraries.ui.generated.resources.item_byline
import campfire.features.libraries.ui.generated.resources.menu_item_add_collection
import campfire.features.libraries.ui.generated.resources.menu_item_download
import campfire.features.libraries.ui.generated.resources.menu_item_mark_finished
import campfire.features.libraries.ui.generated.resources.menu_item_mark_finished_short
import campfire.features.libraries.ui.generated.resources.menu_item_mark_not_finished
import campfire.features.libraries.ui.generated.resources.quick_action_collection
import campfire.features.libraries.ui.generated.resources.quick_action_not_finished
import campfire.features.libraries.ui.generated.resources.quick_action_queue
import campfire.features.libraries.ui.generated.resources.quick_action_queue_long
import campfire.features.libraries.ui.generated.resources.quick_action_remove_download
import campfire.features.libraries.ui.generated.resources.quick_action_remove_download_long
import campfire.features.libraries.ui.generated.resources.quick_action_resume
import campfire.features.libraries.ui.generated.resources.quick_action_stop_download
import campfire.features.libraries.ui.generated.resources.quick_action_unqueue
import campfire.features.libraries.ui.generated.resources.quick_action_unqueue_long
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What was picked from an item's actions, or none when they were put away. */
internal sealed interface ActionChoice {
  data object Dismissed : ActionChoice
  data class Picked(val action: ItemAction) : ActionChoice
  data class Quick(val action: QuickAction) : ActionChoice
}

/**
 * Shows what can be done with [item] as a sheet: the [quickActions] it has as they change, then
 * the server's [actions]. Returns what was picked.
 */
internal suspend fun OverlayHost.showItemActionsSheet(
  item: LibraryItem,
  quickActions: @Composable (LibraryItem) -> List<QuickAction>,
  actions: List<ItemAction>,
): ActionChoice = show(
  AdaptiveSheetOverlay<LibraryItem, ActionChoice>(
    model = item,
    onDismiss = { ActionChoice.Dismissed },
    skipPartiallyExpanded = true,
  ) { model, navigator ->
    Impression {
      ScreenViewEvent("ItemActions", ScreenType.Overlay)
    }

    ItemActionsSheet(
      item = model,
      quickActions = quickActions(model),
      actions = actions,
      onQuickAction = { navigator.finish(ActionChoice.Quick(it)) },
      onAction = { navigator.finish(ActionChoice.Picked(it)) },
    )
  },
)

/**
 * Shows what can be done with [item] as a menu at [pointer], a position in the window: its
 * [quickActions], then the server's [actions]. Returns what was picked.
 */
internal suspend fun OverlayHost.showItemActionsMenu(
  item: LibraryItem,
  quickActions: @Composable (LibraryItem) -> List<QuickAction>,
  actions: List<ItemAction>,
  pointer: Offset,
): ActionChoice = show(ItemActionsMenuOverlay(item, quickActions, actions, pointer))

/**
 * Asks how to delete the item titled [itemTitle], starting with deleting its files chosen if
 * [deleteFilesByDefault], and returns whether its files go too, or null when the user backs out.
 */
internal suspend fun OverlayHost.showDeleteItemDialog(itemTitle: String, deleteFilesByDefault: Boolean): Boolean? {
  return when (val choice = show(DeleteItemOverlay(itemTitle, deleteFilesByDefault))) {
    DeleteChoice.Cancel -> null
    is DeleteChoice.Delete -> choice.hardDelete
  }
}

/**
 * Asks whether to quick match the item titled [itemTitle] against [sourceName], and returns what
 * the match may replace, or null when the user backs out.
 */
internal suspend fun OverlayHost.showQuickMatchDialog(itemTitle: String, sourceName: String): QuickMatchChoice? {
  return when (val choice = show(QuickMatchOverlay(itemTitle, sourceName))) {
    QuickMatchAnswer.Cancel -> null
    is QuickMatchAnswer.Match -> choice.choice
  }
}

/**
 * Asks to confirm downloading [item], as its detail screen does, with permission to notify of how
 * it goes. Returns whether to stop asking, or null when the user backs out.
 */
internal suspend fun OverlayHost.showConfirmDownloadDialog(item: LibraryItem): Boolean? {
  return (show(ConfirmDownloadOverlay(item)) as? DownloadAnswer.Download)?.doNotShowAgain
}

/** Offers [item] to a playlist with [dialog], and returns the playlist it went to, if one */
internal suspend fun OverlayHost.showAddToPlaylistDialog(
  item: LibraryItem,
  dialog: AddToPlaylistDialog,
): PlaylistDialogResult = show(
  DialogOverlay { finish ->
    dialog.Content(
      libraryItemId = item.id,
      itemTitle = item.media.metadata.title.orEmpty(),
      onDismiss = finish,
      modifier = Modifier,
      episode = null,
    )
  },
)

/** Offers [item] to a collection with [dialog], returning once it's put away */
internal suspend fun OverlayHost.showAddToCollectionDialog(
  item: LibraryItem,
  dialog: AddToCollectionDialog,
) {
  show(
    DialogOverlay<Unit> { finish ->
      dialog.Content(item = item, onDismiss = { finish(Unit) }, modifier = Modifier)
    },
  )
}

/**
 * The item, then what can be done with it: the [quickActions] for its own listening as tiles, and
 * what the server lets the account do to it, [actions], as rows under them.
 */
@Composable
internal fun ItemActionsSheet(
  item: LibraryItem,
  quickActions: List<QuickAction>,
  actions: List<ItemAction>,
  onQuickAction: (QuickAction) -> Unit,
  onAction: (ItemAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isSideSheet = LocalSheetPresentation.current == SheetPresentation.Side
  Column(
    modifier = modifier
      .then(if (isSideSheet) Modifier.width(SideSheetWidth) else Modifier.fillMaxWidth())
      // A side sheet is as tall as a short window, which the rows can outgrow
      .then(if (isSideSheet) Modifier.verticalScroll(rememberScrollState()) else Modifier)
      .padding(bottom = 16.dp),
  ) {
    ItemHeader(
      item = item,
      coverSize = if (isSideSheet) SideSheetCoverSize else CoverSize,
      // A bottom sheet has its drag handle above; a side sheet starts at the window's edge
      modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = if (isSideSheet) 24.dp else 8.dp, bottom = 20.dp),
    )

    if (quickActions.isNotEmpty()) {
      QuickActionTiles(
        actions = quickActions,
        onAction = onQuickAction,
        modifier = Modifier.padding(bottom = 12.dp),
      )
    }

    if (actions.isNotEmpty()) {
      if (quickActions.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 8.dp))
      actions.forEachIndexed { index, action ->
        // Deleting stands apart from the rest
        if (action == ItemAction.Delete && index > 0) HorizontalDivider()
        ActionRow(action = action, onClick = { onAction(action) })
      }
    }

    Spacer(Modifier.navigationBarsPadding())
  }
}

/** The item's cover, title, and who it's by beside how long it is or what it's read in */
@Composable
private fun ItemHeader(
  item: LibraryItem,
  coverSize: Dp,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.testTag("item_actions_header"),
  ) {
    CoverImage(
      imageUrl = item.media.coverImageUrl,
      contentDescription = null,
      size = coverSize,
      placeholder = placeholderBookPainter(),
      shape = coverImageShape(coverSize),
      modifier = Modifier.size(coverSize),
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.media.metadata.title.orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      val byline = itemByline(item)
      if (byline.isNotEmpty()) {
        Text(
          text = byline,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("item_actions_byline"),
        )
      }
    }
  }
}

/**
 * Who [item]'s by, beside how long it is, or for an ebook with nothing to listen to, what it's
 * read in and how big it is, as its detail screen says.
 */
@Composable
internal fun itemByline(item: LibraryItem): String {
  val author = item.media.metadata.authorName ?: item.media.metadata.author
  val ebookFormat = item.media.ebookFormat?.takeIf { item.isEbookOnly }?.uppercase()
  val size = item.media.sizeInBytes
  val detail = when {
    ebookFormat != null && size > 0L -> stringResource(
      Res.string.ebook_format_and_size,
      ebookFormat,
      size.asReadableBytes(),
    )
    ebookFormat != null -> ebookFormat
    item.media.durationInMillis > 0L -> item.media.durationInMillis.milliseconds.readoutFormat().trim()
    else -> null
  }
  return when {
    author != null && detail != null -> stringResource(Res.string.item_byline, author, detail)
    else -> author ?: detail.orEmpty()
  }
}

/**
 * The quick actions side by side, sharing the width where they fit at a size their names can be
 * read at, or scrolling sideways where they don't
 */
@Composable
private fun QuickActionTiles(
  actions: List<QuickAction>,
  onAction: (QuickAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val fits = maxWidth - TilesPadding * 2 >= TileMinWidth * actions.size
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .then(if (fits) Modifier else Modifier.horizontalScroll(rememberScrollState()))
        .padding(horizontal = TilesPadding)
        .testTag("quick_actions"),
    ) {
      actions.forEach { action ->
        QuickActionTile(
          action = action,
          onClick = { onAction(action) },
          modifier = if (fits) Modifier.weight(1f) else Modifier.width(TileMinWidth),
        )
      }
    }
  }
}

/** A quick action as an icon in a rounded square, with what it does under it */
@Composable
private fun QuickActionTile(
  action: QuickAction,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val description = stringResource(action.description)
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .clickable(role = Role.Button, onClick = onClick)
      .semantics(mergeDescendants = true) { contentDescription = description }
      .padding(vertical = 4.dp)
      .testTag(action.testTag),
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
      contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier
        .widthIn(max = TileSize)
        .fillMaxWidth()
        .aspectRatio(1f),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(action.icon, contentDescription = null)
      }
    }
    Text(
      text = stringResource(action.label),
      style = MaterialTheme.typography.labelMedium,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ActionRow(
  action: ItemAction,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val color = if (action == ItemAction.Delete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
  ListItem(
    headlineContent = { Text(text = stringResource(action.label), color = color) },
    leadingContent = { Icon(action.icon, contentDescription = null, tint = color) },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = modifier
      .clickable(onClick = onClick)
      .padding(start = 8.dp)
      .testTag(action.testTag),
  )
}

/** What a quick action says on its tile */
private val QuickAction.label: StringResource
  get() = when (this) {
    is QuickAction.Play -> if (resume) Res.string.quick_action_resume else Res.string.action_play
    is QuickAction.Read -> if (resume) Res.string.quick_action_resume else Res.string.action_read
    QuickAction.Download -> Res.string.menu_item_download
    QuickAction.StopDownload -> Res.string.quick_action_stop_download
    QuickAction.RemoveDownload -> Res.string.quick_action_remove_download
    QuickAction.MarkFinished -> Res.string.menu_item_mark_finished_short
    QuickAction.MarkNotFinished -> Res.string.quick_action_not_finished
    QuickAction.AddToQueue -> Res.string.quick_action_queue
    QuickAction.RemoveFromQueue -> Res.string.quick_action_unqueue
    QuickAction.AddToPlaylist -> Res.string.action_add_to_playlist_short
    QuickAction.AddToCollection -> Res.string.quick_action_collection
  }

/** What a quick action does in full, as a menu or a screen reader says it */
private val QuickAction.description: StringResource
  get() = when (this) {
    is QuickAction.Play -> if (resume) Res.string.action_resume_listening else Res.string.action_play
    is QuickAction.Read -> if (resume) Res.string.action_resume_reading else Res.string.action_read
    QuickAction.Download -> Res.string.menu_item_download
    QuickAction.StopDownload -> Res.string.action_stop_downloading
    QuickAction.RemoveDownload -> Res.string.quick_action_remove_download_long
    QuickAction.MarkFinished -> Res.string.menu_item_mark_finished
    QuickAction.MarkNotFinished -> Res.string.menu_item_mark_not_finished
    QuickAction.AddToQueue -> Res.string.quick_action_queue_long
    QuickAction.RemoveFromQueue -> Res.string.quick_action_unqueue_long
    QuickAction.AddToPlaylist -> Res.string.action_add_to_playlist_long
    QuickAction.AddToCollection -> Res.string.menu_item_add_collection
  }

private val QuickAction.icon: ImageVector
  get() = when (this) {
    is QuickAction.Play -> CampfireIcons.Rounded.PlayArrow
    is QuickAction.Read -> CampfireIcons.Rounded.MenuBook
    QuickAction.Download -> CampfireIcons.Rounded.Download
    QuickAction.StopDownload -> CampfireIcons.Rounded.StopCircle
    QuickAction.RemoveDownload -> CampfireIcons.Rounded.DownloadDone
    QuickAction.MarkFinished -> CampfireIcons.Rounded.MarkFinished
    QuickAction.MarkNotFinished -> CampfireIcons.Rounded.Replay
    QuickAction.AddToQueue -> CampfireIcons.Rounded.QueuePlayNext
    QuickAction.RemoveFromQueue -> CampfireIcons.Rounded.PlaylistAddCheck
    QuickAction.AddToPlaylist -> CampfireIcons.Rounded.PlaylistAdd
    QuickAction.AddToCollection -> CampfireIcons.Rounded.LibraryAdd
  }

internal val QuickAction.testTag: String
  get() = when (this) {
    is QuickAction.Play -> "quick_action_play"
    is QuickAction.Read -> "quick_action_read"
    QuickAction.Download -> "quick_action_download"
    QuickAction.StopDownload -> "quick_action_stop_download"
    QuickAction.RemoveDownload -> "quick_action_remove_download"
    QuickAction.MarkFinished -> "quick_action_mark_finished"
    QuickAction.MarkNotFinished -> "quick_action_mark_not_finished"
    QuickAction.AddToQueue -> "quick_action_add_to_queue"
    QuickAction.RemoveFromQueue -> "quick_action_remove_from_queue"
    QuickAction.AddToPlaylist -> "quick_action_add_to_playlist"
    QuickAction.AddToCollection -> "quick_action_add_to_collection"
  }

/** The menu a right click opens, anchored where the click was */
private class ItemActionsMenuOverlay(
  private val item: LibraryItem,
  private val quickActions: @Composable (LibraryItem) -> List<QuickAction>,
  private val actions: List<ItemAction>,
  private val pointer: Offset,
) : Overlay<ActionChoice> {

  @Composable
  override fun Content(navigator: OverlayNavigator<ActionChoice>) {
    val quick = quickActions(item)
    // The pointer is in the window; the overlay may not start at the window's corner
    var origin by remember { mutableStateOf<Offset?>(null) }
    Box(
      Modifier
        .fillMaxSize()
        .onGloballyPositioned { origin = it.positionInWindow() },
    ) {
      origin?.let { origin ->
        Box(Modifier.offset { (pointer - origin).round() }) {
          ItemActionsMenu(
            expanded = true,
            actions = actions,
            onAction = { navigator.finish(ActionChoice.Picked(it)) },
            onDismissRequest = { navigator.finish(ActionChoice.Dismissed) },
            leading = if (quick.isEmpty()) {
              null
            } else {
              {
                quick.forEach { action ->
                  DropdownMenuItem(
                    text = { Text(stringResource(action.description)) },
                    leadingIcon = { Icon(action.icon, contentDescription = null) },
                    onClick = { navigator.finish(ActionChoice.Quick(action)) },
                    modifier = Modifier.testTag(action.testTag),
                  )
                }
              }
            },
          )
        }
      }
    }
  }
}

/** Shows a dialog that finishes with what it hands back */
private class DialogOverlay<R : Any>(
  private val content: @Composable (finish: (R) -> Unit) -> Unit,
) : Overlay<R> {

  @Composable
  override fun Content(navigator: OverlayNavigator<R>) {
    content(navigator::finish)
  }
}

private sealed interface DownloadAnswer {
  data object Cancel : DownloadAnswer
  data class Download(val doNotShowAgain: Boolean) : DownloadAnswer
}

private class ConfirmDownloadOverlay(
  private val item: LibraryItem,
) : Overlay<DownloadAnswer> {

  @Composable
  override fun Content(navigator: OverlayNavigator<DownloadAnswer>) {
    var doNotShowAgain by remember { mutableStateOf(false) }
    val notificationPermission = rememberPostNotificationPermissionState { granted ->
      if (granted) navigator.finish(DownloadAnswer.Download(doNotShowAgain))
    }
    ConfirmDownloadDialog(
      item = item,
      onConfirm = {
        if (notificationPermission is PermissionState.Granted) {
          navigator.finish(DownloadAnswer.Download(it))
        } else {
          doNotShowAgain = it
          notificationPermission.launchPermissionRequest()
        }
      },
      onDismissRequest = { navigator.finish(DownloadAnswer.Cancel) },
    )
  }
}

private sealed interface DeleteChoice {
  data object Cancel : DeleteChoice
  data class Delete(val hardDelete: Boolean) : DeleteChoice
}

private class DeleteItemOverlay(
  private val itemTitle: String,
  private val deleteFilesByDefault: Boolean,
) : Overlay<DeleteChoice> {

  @Composable
  override fun Content(navigator: OverlayNavigator<DeleteChoice>) {
    DeleteItemDialog(
      itemTitle = itemTitle,
      deleteFilesByDefault = deleteFilesByDefault,
      onConfirm = { navigator.finish(DeleteChoice.Delete(it)) },
      onDismissRequest = { navigator.finish(DeleteChoice.Cancel) },
    )
  }
}

private sealed interface QuickMatchAnswer {
  data object Cancel : QuickMatchAnswer
  data class Match(val choice: QuickMatchChoice) : QuickMatchAnswer
}

private class QuickMatchOverlay(
  private val itemTitle: String,
  private val sourceName: String,
) : Overlay<QuickMatchAnswer> {

  @Composable
  override fun Content(navigator: OverlayNavigator<QuickMatchAnswer>) {
    QuickMatchDialog(
      itemTitle = itemTitle,
      sourceName = sourceName,
      onConfirm = { navigator.finish(QuickMatchAnswer.Match(it)) },
      onDismissRequest = { navigator.finish(QuickMatchAnswer.Cancel) },
    )
  }
}

private val SideSheetWidth = 360.dp
private val CoverSize = 56.dp
private val SideSheetCoverSize = 44.dp
private val TileSize = 56.dp

/** Wide enough for a tile's name to sit on its own lines, unbroken */
private val TileMinWidth = 72.dp
private val TilesPadding = 12.dp
