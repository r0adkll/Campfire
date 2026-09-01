// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.widgets.CampfireLoadingIndicator
import app.campfire.common.compose.widgets.CampfireMediumTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.discover.api.DiscoverScanState
import app.campfire.discover.api.screen.DiscoverScreen
import app.campfire.discover.ui.composables.MissingBooksList
import app.campfire.discover.ui.composables.UpcomingTimeline
import campfire.features.discover.ui.generated.resources.Res
import campfire.features.discover.ui.generated.resources.action_back
import campfire.features.discover.ui.generated.resources.discover_cancel_scan
import campfire.features.discover.ui.generated.resources.discover_empty_missing
import campfire.features.discover.ui.generated.resources.discover_empty_upcoming
import campfire.features.discover.ui.generated.resources.discover_failed_series
import campfire.features.discover.ui.generated.resources.discover_scan_progress
import campfire.features.discover.ui.generated.resources.discover_skipped_series
import campfire.features.discover.ui.generated.resources.discover_source_attribution
import campfire.features.discover.ui.generated.resources.discover_tab_missing
import campfire.features.discover.ui.generated.resources.discover_tab_upcoming
import campfire.features.discover.ui.generated.resources.discover_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(DiscoverScreen::class, UserScope::class)
@Composable
fun DiscoverUi(
  state: DiscoverUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireMediumTopAppBar(
        title = { Text(stringResource(Res.string.discover_title)) },
        navigationIcon = {
          val backLabel = stringResource(Res.string.action_back)
          IconButtonTooltip(text = backLabel) {
            IconButton(onClick = { state.eventSink(DiscoverUiEvent.Back) }) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backLabel)
            }
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = CampfireWindowInsets,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    val scan = state.scanState
    val completed = scan as? DiscoverScanState.Completed
    val results = when (scan) {
      DiscoverScanState.Idle -> null
      is DiscoverScanState.Running -> scan.results
      is DiscoverScanState.Completed -> scan.results
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
    ) {
      if (scan is DiscoverScanState.Running) {
        ScanProgressHeader(
          done = scan.done,
          total = scan.total,
          onCancel = { state.eventSink(DiscoverUiEvent.CancelScan) },
        )
      }

      SingleChoiceSegmentedButtonRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
      ) {
        DiscoverTab.entries.forEachIndexed { index, tab ->
          SegmentedButton(
            selected = state.selectedTab == tab,
            onClick = { state.eventSink(DiscoverUiEvent.SelectTab(tab)) },
            shape = SegmentedButtonDefaults.itemShape(index, DiscoverTab.entries.size),
          ) {
            Text(
              when (tab) {
                DiscoverTab.Missing -> stringResource(Res.string.discover_tab_missing)
                DiscoverTab.Upcoming -> stringResource(Res.string.discover_tab_upcoming)
              },
            )
          }
        }
      }

      val ptrState = rememberPullToRefreshState()
      PullToRefreshBox(
        state = ptrState,
        // The scan's own header carries the determinate progress, so the pull
        // indicator only tracks the gesture itself.
        isRefreshing = false,
        onRefresh = { state.eventSink(DiscoverUiEvent.Refresh) },
        indicator = {
          CampfireLoadingIndicator(
            state = ptrState,
            isRefreshing = false,
            modifier = Modifier.align(Alignment.TopCenter),
          )
        },
        modifier = Modifier.weight(1f),
      ) {
        when (state.selectedTab) {
          DiscoverTab.Missing -> MissingBooksList(
            books = results?.missing.orEmpty(),
            onSeriesClick = { id, name -> state.eventSink(DiscoverUiEvent.SeriesClick(id, name)) },
            onBookClick = { url -> state.eventSink(DiscoverUiEvent.BookClick(url)) },
          )

          DiscoverTab.Upcoming -> UpcomingTimeline(
            books = results?.upcoming.orEmpty(),
            onBookClick = { url -> state.eventSink(DiscoverUiEvent.BookClick(url)) },
          )
        }

        val emptyMessage = when (state.selectedTab) {
          DiscoverTab.Missing -> stringResource(Res.string.discover_empty_missing)
            .takeIf { completed != null && completed.results.missing.isEmpty() }
          DiscoverTab.Upcoming -> stringResource(Res.string.discover_empty_upcoming)
            .takeIf { completed != null && completed.results.upcoming.isEmpty() }
        }
        if (emptyMessage != null) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
          ) {
            EmptyState(emptyMessage)
          }
        }
      }

      if (completed != null) {
        CompletedFooter(completed)
      }
    }
  }
}

@Composable
private fun ScanProgressHeader(
  done: Int,
  total: Int,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
  ) {
    LinearProgressIndicator(
      progress = { if (total == 0) 0f else done.toFloat() / total },
      modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = stringResource(Res.string.discover_scan_progress, done, total),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.weight(1f))
      TextButton(onClick = onCancel) {
        Text(stringResource(Res.string.discover_cancel_scan))
      }
    }
  }
}

@Composable
private fun CompletedFooter(
  completed: DiscoverScanState.Completed,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    completed.results.providerName?.let { providerName ->
      Text(
        text = stringResource(Res.string.discover_source_attribution, providerName),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (completed.skippedCount > 0) {
      Text(
        text = stringResource(Res.string.discover_skipped_series, completed.skippedCount),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (completed.failedCount > 0) {
      Text(
        text = stringResource(Res.string.discover_failed_series, completed.failedCount),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
      )
    }
  }
}
