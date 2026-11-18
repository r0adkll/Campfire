// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.DeleteForever
import app.campfire.common.compose.icons.rounded.DeleteSweep
import app.campfire.common.compose.icons.rounded.Settings
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.compose.widgets.adaptiveEnterAlwaysScrollBehavior
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.LibraryItem
import app.campfire.downloads.ui.composables.DownloadRow
import app.campfire.downloads.ui.composables.DownloadSectionHeader
import app.campfire.downloads.ui.composables.DownloadsSummaryHeader
import app.campfire.downloads.ui.composables.ShowDownloadsCard
import app.campfire.downloads.ui.composables.title
import campfire.features.downloads.ui.generated.resources.Res
import campfire.features.downloads.ui.generated.resources.action_delete_all
import campfire.features.downloads.ui.generated.resources.action_delete_finished
import campfire.features.downloads.ui.generated.resources.action_download_settings
import campfire.features.downloads.ui.generated.resources.action_undo_delete
import campfire.features.downloads.ui.generated.resources.dialog_action_cancel
import campfire.features.downloads.ui.generated.resources.dialog_action_delete
import campfire.features.downloads.ui.generated.resources.dialog_delete_all_title
import campfire.features.downloads.ui.generated.resources.dialog_delete_finished_title
import campfire.features.downloads.ui.generated.resources.dialog_delete_message
import campfire.features.downloads.ui.generated.resources.dialog_delete_show_title
import campfire.features.downloads.ui.generated.resources.downloads_empty
import campfire.features.downloads.ui.generated.resources.downloads_empty_hint
import campfire.features.downloads.ui.generated.resources.downloads_error
import campfire.features.downloads.ui.generated.resources.downloads_removed
import campfire.features.downloads.ui.generated.resources.downloads_section_audiobooks
import campfire.features.downloads.ui.generated.resources.downloads_section_ebooks
import campfire.features.downloads.ui.generated.resources.downloads_section_in_progress
import campfire.features.downloads.ui.generated.resources.downloads_section_podcasts
import campfire.features.downloads.ui.generated.resources.downloads_section_unavailable
import campfire.features.downloads.ui.generated.resources.downloads_title
import campfire.features.downloads.ui.generated.resources.downloads_unknown_podcast
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@CircuitInject(DownloadsScreen::class, UserScope::class)
@Composable
fun DownloadsUi(
  state: DownloadsUiState,
  modifier: Modifier = Modifier,
) {
  UndoDeleteSnackbar(
    pendingDelete = state.pendingDelete,
    onUndo = { state.eventSink(DownloadsUiEvent.UndoDelete(it)) },
    onCommit = { state.eventSink(DownloadsUiEvent.CommitDelete(it)) },
  )

  var confirming by remember { mutableStateOf<BulkDelete?>(null) }
  confirming?.let { delete ->
    ConfirmBulkDeleteDialog(
      delete = delete,
      onConfirm = {
        confirming = null
        state.eventSink(DownloadsUiEvent.DeleteDownloads(delete.entries))
      },
      onDismiss = { confirming = null },
    )
  }

  val scrollBehavior = adaptiveEnterAlwaysScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(stringResource(Res.string.downloads_title)) },
        navigationIcon = {
          NavigationBackButton(onClick = { state.eventSink(DownloadsUiEvent.Back) })
        },
        actions = {
          val content = state.downloads.dataOrNull
          val entries = content?.entries.orEmpty()
          val deleteFinishedTitle = stringResource(Res.string.dialog_delete_finished_title)
          val deleteAllTitle = stringResource(Res.string.dialog_delete_all_title)
          DownloadsActions(
            // Only enabled when there's something for them to delete
            onDeleteFinished = content?.finishedBooks?.takeIf { it.isNotEmpty() }?.let { finished ->
              { confirming = BulkDelete(deleteFinishedTitle, finished) }
            },
            onDeleteAll = entries.takeIf { it.isNotEmpty() }?.let { all ->
              { confirming = BulkDelete(deleteAllTitle, all) }
            },
            onSettings = { state.eventSink(DownloadsUiEvent.OpenSettings) },
          )
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = CampfireWindowInsets,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    val layoutDirection = LocalLayoutDirection.current
    when (val downloads = state.downloads) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))

      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.downloads_error),
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      )

      is LoadState.Loaded -> if (downloads.data.summary.count == 0) {
        EmptyState(
          message = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(stringResource(Res.string.downloads_empty))
              Text(
                text = stringResource(Res.string.downloads_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          },
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        )
      } else {
        DownloadList(
          content = downloads.data,
          onFilterChange = { state.eventSink(DownloadsUiEvent.FilterChange(it)) },
          onShowClick = { state.eventSink(DownloadsUiEvent.ShowClick(it)) },
          onDeleteShow = { show, title -> confirming = BulkDelete(title, show.episodes) },
          onClick = { state.eventSink(DownloadsUiEvent.DownloadClick(it)) },
          onPlayClick = { state.eventSink(DownloadsUiEvent.PlayClick(it)) },
          onRetryClick = { state.eventSink(DownloadsUiEvent.RetryDownload(it)) },
          onDelete = { state.eventSink(DownloadsUiEvent.DeleteDownload(it)) },
          // Starts under the top bar rather than scrolling beneath it, where a heading stuck to
          // the top of the list would be hidden. The bar gives up its height as it scrolls away,
          // so the headings then stick below the status bar
          modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
          contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection),
            end = paddingValues.calculateEndPadding(layoutDirection),
            bottom = paddingValues.calculateBottomPadding(),
          ),
        )
      }
    }
  }
}

/**
 * Tells that [pendingDelete] was taken off the list and offers to put it back. It's removed for
 * good, by [onCommit], once the message goes without Undo being chosen.
 */
@Composable
private fun UndoDeleteSnackbar(
  pendingDelete: DownloadEntry?,
  onUndo: (DownloadEntry) -> Unit,
  onCommit: (DownloadEntry) -> Unit,
) {
  val snackbarHost = LocalSnackBarHost.current
  val message = pendingDelete?.let { stringResource(Res.string.downloads_removed, it.title()) }
  val undoLabel = stringResource(Res.string.action_undo_delete)
  val currentOnUndo by rememberUpdatedState(onUndo)
  val currentOnCommit by rememberUpdatedState(onCommit)
  LaunchedEffect(pendingDelete?.key) {
    if (pendingDelete == null || message == null) return@LaunchedEffect
    snackbarHost.currentSnackbarData?.dismiss()
    val result = snackbarHost.showSnackbar(
      message = message,
      actionLabel = undoLabel,
      duration = SnackbarDuration.Long,
    )
    when (result) {
      SnackbarResult.ActionPerformed -> currentOnUndo(pendingDelete)
      SnackbarResult.Dismissed -> currentOnCommit(pendingDelete)
    }
  }
}

/** Several downloads to delete at once, once it's been asked about under [title]. */
private data class BulkDelete(
  val title: String,
  val entries: List<DownloadEntry>,
)

@Composable
private fun ConfirmBulkDeleteDialog(
  delete: BulkDelete,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text(delete.title) },
    text = {
      Text(
        pluralStringResource(
          Res.plurals.dialog_delete_message,
          delete.entries.size,
          delete.entries.size,
          delete.entries.sumOf { it.sizeOnDevice }.asReadableBytes(),
        ),
      )
    },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(Res.string.dialog_action_delete))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.dialog_action_cancel))
      }
    },
  )
}

/**
 * What can be done to the downloads as a whole, each an action on the top bar. A delete with
 * nothing to delete, null here, is disabled rather than left out, so the others keep their places.
 */
@Composable
private fun DownloadsActions(
  onDeleteFinished: (() -> Unit)?,
  onDeleteAll: (() -> Unit)?,
  onSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier) {
    DownloadsAction(
      label = stringResource(Res.string.action_delete_finished),
      icon = CampfireIcons.Rounded.DeleteSweep,
      onClick = onDeleteFinished,
    )
    DownloadsAction(
      label = stringResource(Res.string.action_delete_all),
      icon = CampfireIcons.Rounded.DeleteForever,
      onClick = onDeleteAll,
    )
    DownloadsAction(
      label = stringResource(Res.string.action_download_settings),
      icon = CampfireIcons.Rounded.Settings,
      onClick = onSettings,
    )
  }
}

@Composable
private fun DownloadsAction(
  label: String,
  icon: ImageVector,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  IconButtonTooltip(text = label, modifier = modifier) {
    IconButton(
      onClick = { onClick?.invoke() },
      enabled = onClick != null,
    ) {
      Icon(icon, contentDescription = label)
    }
  }
}

/**
 * The summary, then each section that has anything in it under a heading that sticks to the top
 * while its rows scroll by. Rows sit in as many columns as fit, one on a phone, with the summary
 * and the headings across them all.
 *
 * @param onDeleteShow called to delete every downloaded episode of a show, with the title to ask
 *   about it under.
 */
@Composable
internal fun DownloadList(
  content: DownloadsContent,
  onFilterChange: (DownloadKind?) -> Unit,
  onShowClick: (LibraryItem) -> Unit,
  onDeleteShow: (ShowDownloads, title: String) -> Unit,
  onClick: (DownloadEntry) -> Unit,
  onPlayClick: (DownloadEntry) -> Unit,
  onRetryClick: (DownloadEntry) -> Unit,
  onDelete: (DownloadEntry) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  val sections = content.sections
  val layoutDirection = LocalLayoutDirection.current
  LazyVerticalGrid(
    columns = GridCells.Adaptive(RowMinWidth),
    contentPadding = PaddingValues(
      start = contentPadding.calculateStartPadding(layoutDirection) + ListPadding,
      end = contentPadding.calculateEndPadding(layoutDirection) + ListPadding,
      top = contentPadding.calculateTopPadding() + RowSpacing,
      bottom = contentPadding.calculateBottomPadding() + ListPadding,
    ),
    verticalArrangement = Arrangement.spacedBy(RowSpacing),
    horizontalArrangement = Arrangement.spacedBy(RowSpacing),
    modifier = modifier.fillMaxSize(),
  ) {
    item(key = "summary", span = { GridItemSpan(maxLineSpan) }, contentType = "summary") {
      DownloadsSummaryHeader(
        summary = content.summary,
        filter = content.filter,
        onFilterChange = onFilterChange,
      )
    }

    fun rows(entries: List<DownloadEntry>) = items(
      items = entries,
      key = { it.key },
      contentType = { "download" },
    ) { entry ->
      DownloadRow(
        entry = entry,
        onClick = { onClick(entry) },
        onPlayClick = { onPlayClick(entry) },
        onRetryClick = { onRetryClick(entry) },
        onDelete = { onDelete(entry) },
        modifier = Modifier.animateItem(),
      )
    }

    if (sections.inProgress.isNotEmpty()) {
      // What's been fetched so far says little, so these are only counted
      sectionHeader(Res.string.downloads_section_in_progress, sections.inProgress.size, sizeInBytes = 0L)
      rows(sections.inProgress)
    }

    if (sections.audiobooks.isNotEmpty()) {
      sectionHeader(
        title = Res.string.downloads_section_audiobooks,
        count = sections.audiobooks.size,
        sizeInBytes = sections.audiobooks.sumOf { it.sizeInBytes },
      )
      rows(sections.audiobooks)
    }

    if (sections.shows.isNotEmpty()) {
      sectionHeader(
        title = Res.string.downloads_section_podcasts,
        count = sections.shows.sumOf { it.episodes.size },
        sizeInBytes = sections.shows.sumOf { it.sizeInBytes },
      )
      items(
        items = sections.shows,
        key = { "show:${it.libraryItem.id}" },
        contentType = { "show" },
      ) { show ->
        val deleteTitle = stringResource(
          Res.string.dialog_delete_show_title,
          show.libraryItem.media.metadata.title ?: stringResource(Res.string.downloads_unknown_podcast),
        )
        ShowDownloadsCard(
          show = show,
          onShowClick = { onShowClick(show.libraryItem) },
          onEpisodeClick = onClick,
          onPlayClick = onPlayClick,
          onDelete = onDelete,
          onDeleteAll = { onDeleteShow(show, deleteTitle) },
          modifier = Modifier.animateItem(),
        )
      }
    }

    if (sections.ebooks.isNotEmpty()) {
      sectionHeader(
        title = Res.string.downloads_section_ebooks,
        count = sections.ebooks.size,
        sizeInBytes = sections.ebooks.sumOf { it.sizeInBytes },
      )
      rows(sections.ebooks)
    }

    if (sections.unavailable.isNotEmpty()) {
      sectionHeader(
        title = Res.string.downloads_section_unavailable,
        count = sections.unavailable.size,
        sizeInBytes = sections.unavailable.sumOf { it.sizeInBytes },
      )
      rows(sections.unavailable)
    }
  }
}

private fun LazyGridScope.sectionHeader(title: StringResource, count: Int, sizeInBytes: Long) {
  stickyHeader(key = "header:${title.key}", contentType = "header") {
    DownloadSectionHeader(
      title = stringResource(title),
      count = count,
      sizeInBytes = sizeInBytes,
      modifier = Modifier.animateItem(),
    )
  }
}

private val ListPadding = 16.dp
private val RowSpacing = 8.dp

/** The narrowest a row gets before the list takes fewer columns: a phone is one column wide. */
private val RowMinWidth = 340.dp
