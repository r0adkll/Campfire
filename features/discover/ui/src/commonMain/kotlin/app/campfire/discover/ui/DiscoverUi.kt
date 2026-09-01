// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.widgets.CampfireMediumTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.discover.api.DiscoverScanResults
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
import campfire.features.discover.ui.generated.resources.discover_idle_explainer
import campfire.features.discover.ui.generated.resources.discover_rescan_action
import campfire.features.discover.ui.generated.resources.discover_scan_action
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
        actions = {
          if (state.scanState is DiscoverScanState.Completed) {
            val rescanLabel = stringResource(Res.string.discover_rescan_action)
            IconButtonTooltip(text = rescanLabel) {
              IconButton(onClick = { state.eventSink(DiscoverUiEvent.Scan) }) {
                Icon(Icons.Rounded.Refresh, contentDescription = rescanLabel)
              }
            }
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = CampfireWindowInsets,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    when (val scan = state.scanState) {
      DiscoverScanState.Idle -> IdleContent(
        paddingValues = paddingValues,
        onScan = { state.eventSink(DiscoverUiEvent.Scan) },
      )

      is DiscoverScanState.Running -> ScanContent(
        results = scan.results,
        selectedTab = state.selectedTab,
        eventSink = state.eventSink,
        paddingValues = paddingValues,
        header = {
          ScanProgressHeader(
            done = scan.done,
            total = scan.total,
            onCancel = { state.eventSink(DiscoverUiEvent.CancelScan) },
          )
        },
        completed = null,
      )

      is DiscoverScanState.Completed -> ScanContent(
        results = scan.results,
        selectedTab = state.selectedTab,
        eventSink = state.eventSink,
        paddingValues = paddingValues,
        header = null,
        completed = scan,
      )
    }
  }
}

@Composable
private fun IdleContent(
  paddingValues: PaddingValues,
  onScan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(paddingValues)
      .padding(horizontal = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(Res.string.discover_idle_explainer),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onScan) {
      Text(stringResource(Res.string.discover_scan_action))
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
private fun ScanContent(
  results: DiscoverScanResults,
  selectedTab: DiscoverTab,
  eventSink: (DiscoverUiEvent) -> Unit,
  paddingValues: PaddingValues,
  header: (@Composable () -> Unit)?,
  completed: DiscoverScanState.Completed?,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(paddingValues),
  ) {
    header?.invoke()

    SingleChoiceSegmentedButtonRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      DiscoverTab.entries.forEachIndexed { index, tab ->
        SegmentedButton(
          selected = selectedTab == tab,
          onClick = { eventSink(DiscoverUiEvent.SelectTab(tab)) },
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

    Box(modifier = Modifier.weight(1f)) {
      when (selectedTab) {
        DiscoverTab.Missing -> if (results.missing.isEmpty() && completed != null) {
          CenteredEmptyState(stringResource(Res.string.discover_empty_missing))
        } else {
          MissingBooksList(
            books = results.missing,
            onSeriesClick = { id, name -> eventSink(DiscoverUiEvent.SeriesClick(id, name)) },
            onBookClick = { url -> eventSink(DiscoverUiEvent.BookClick(url)) },
          )
        }

        DiscoverTab.Upcoming -> if (results.upcoming.isEmpty() && completed != null) {
          CenteredEmptyState(stringResource(Res.string.discover_empty_upcoming))
        } else {
          UpcomingTimeline(
            books = results.upcoming,
            onBookClick = { url -> eventSink(DiscoverUiEvent.BookClick(url)) },
          )
        }
      }
    }

    if (completed != null) {
      CompletedFooter(completed)
    }
  }
}

@Composable
private fun CenteredEmptyState(
  message: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 32.dp),
    contentAlignment = Alignment.Center,
  ) {
    EmptyState(message)
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
