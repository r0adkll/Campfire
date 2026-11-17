// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.downloads.ui.composables.DownloadRow
import app.campfire.downloads.ui.composables.DownloadSectionHeader
import app.campfire.downloads.ui.composables.DownloadsSummaryHeader
import app.campfire.downloads.ui.composables.ShowDownloadsCard
import app.campfire.downloads.ui.composables.title
import campfire.features.downloads.ui.generated.resources.Res
import campfire.features.downloads.ui.generated.resources.action_undo_delete
import campfire.features.downloads.ui.generated.resources.downloads_empty
import campfire.features.downloads.ui.generated.resources.downloads_error
import campfire.features.downloads.ui.generated.resources.downloads_removed
import campfire.features.downloads.ui.generated.resources.downloads_section_audiobooks
import campfire.features.downloads.ui.generated.resources.downloads_section_ebooks
import campfire.features.downloads.ui.generated.resources.downloads_section_in_progress
import campfire.features.downloads.ui.generated.resources.downloads_section_podcasts
import campfire.features.downloads.ui.generated.resources.downloads_section_unavailable
import campfire.features.downloads.ui.generated.resources.downloads_title
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.StringResource
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

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(stringResource(Res.string.downloads_title)) },
        navigationIcon = {
          NavigationBackButton(onClick = { state.eventSink(DownloadsUiEvent.Back) })
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = CampfireWindowInsets,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
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
          message = stringResource(Res.string.downloads_empty),
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        )
      } else {
        DownloadList(
          content = downloads.data,
          onFilterChange = { state.eventSink(DownloadsUiEvent.FilterChange(it)) },
          onShowClick = { state.eventSink(DownloadsUiEvent.ShowClick(it)) },
          onClick = { state.eventSink(DownloadsUiEvent.DownloadClick(it)) },
          onPlayClick = { state.eventSink(DownloadsUiEvent.PlayClick(it)) },
          onRetryClick = { state.eventSink(DownloadsUiEvent.RetryDownload(it)) },
          onDelete = { state.eventSink(DownloadsUiEvent.DeleteDownload(it)) },
          contentPadding = paddingValues,
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

/**
 * The summary, then each section that has anything in it under a heading that sticks to the top
 * while its rows scroll by.
 */
@Composable
internal fun DownloadList(
  content: DownloadsContent,
  onFilterChange: (DownloadKind?) -> Unit,
  onShowClick: (LibraryItem) -> Unit,
  onClick: (DownloadEntry) -> Unit,
  onPlayClick: (DownloadEntry) -> Unit,
  onRetryClick: (DownloadEntry) -> Unit,
  onDelete: (DownloadEntry) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  val sections = content.sections
  val layoutDirection = LocalLayoutDirection.current
  LazyColumn(
    contentPadding = PaddingValues(
      start = contentPadding.calculateStartPadding(layoutDirection) + ListPadding,
      end = contentPadding.calculateEndPadding(layoutDirection) + ListPadding,
      top = contentPadding.calculateTopPadding() + RowSpacing,
      bottom = contentPadding.calculateBottomPadding() + ListPadding,
    ),
    verticalArrangement = Arrangement.spacedBy(RowSpacing),
    modifier = modifier.fillMaxSize(),
  ) {
    item(key = "summary", contentType = "summary") {
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
        ShowDownloadsCard(
          show = show,
          onShowClick = { onShowClick(show.libraryItem) },
          onEpisodeClick = onClick,
          onPlayClick = onPlayClick,
          onDelete = onDelete,
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

private fun LazyListScope.sectionHeader(title: StringResource, count: Int, sizeInBytes: Long) {
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
