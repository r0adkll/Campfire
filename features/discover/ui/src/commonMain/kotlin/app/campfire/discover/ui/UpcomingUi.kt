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
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.CampfireMediumTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.discover.api.DiscoverScanResults
import app.campfire.discover.api.DiscoverScanState
import app.campfire.discover.api.DiscoveredBook
import app.campfire.discover.api.screen.UpcomingScreen
import app.campfire.discover.ui.composables.UpcomingTimeline
import campfire.features.discover.ui.generated.resources.Res
import campfire.features.discover.ui.generated.resources.action_back
import campfire.features.discover.ui.generated.resources.discover_cancel_scan
import campfire.features.discover.ui.generated.resources.discover_empty_upcoming
import campfire.features.discover.ui.generated.resources.discover_failed_series
import campfire.features.discover.ui.generated.resources.discover_scan_action
import campfire.features.discover.ui.generated.resources.discover_scan_progress
import campfire.features.discover.ui.generated.resources.discover_skipped_series
import campfire.features.discover.ui.generated.resources.discover_source_attribution
import campfire.features.discover.ui.generated.resources.upcoming_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(UpcomingScreen::class, UserScope::class)
@Composable
fun UpcomingUi(
  state: UpcomingUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireMediumTopAppBar(
        title = { Text(stringResource(Res.string.upcoming_title)) },
        navigationIcon = {
          val backLabel = stringResource(Res.string.action_back)
          IconButtonTooltip(text = backLabel) {
            IconButton(onClick = { state.eventSink(UpcomingUiEvent.Back) }) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backLabel)
            }
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    floatingActionButton = {
      if (state.scanState is DiscoverScanState.Completed) {
        SmallExtendedFloatingActionButton(
          text = { Text(stringResource(Res.string.discover_scan_action)) },
          icon = { Icon(Icons.Rounded.Radar, contentDescription = null) },
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          onClick = { state.eventSink(UpcomingUiEvent.Refresh) },
        )
      }
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
          onCancel = { state.eventSink(UpcomingUiEvent.CancelScan) },
        )
      }

      Box(modifier = Modifier.weight(1f)) {
        UpcomingTimeline(
          books = results?.upcoming.orEmpty(),
          onBookClick = { url -> state.eventSink(UpcomingUiEvent.BookClick(url)) },
        )

        if (completed != null && completed.results.upcoming.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
          ) {
            EmptyState(stringResource(Res.string.discover_empty_upcoming))
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

// region — Previews —

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun PreviewWrapper(
  content: @Composable () -> Unit,
) {
  CampfireTheme {
    CompositionLocalProvider(
      LocalWindowSizeClass provides calculateWindowSizeClass(),
      LocalContentLayout provides ContentLayout.Root,
    ) {
      content()
    }
  }
}

@Preview
@Composable
private fun UpcomingUiPreview_Idle() = PreviewWrapper {
  UpcomingUi(state = previewState(DiscoverScanState.Idle))
}

@Preview
@Composable
private fun UpcomingUiPreview_Scanning() = PreviewWrapper {
  UpcomingUi(
    state = previewState(
      DiscoverScanState.Running(
        done = 12,
        total = 87,
        results = previewResults(),
      ),
    ),
  )
}

@Preview
@Composable
private fun UpcomingUiPreview_Completed() = PreviewWrapper {
  UpcomingUi(
    state = previewState(previewCompleted(skippedCount = 3, failedCount = 1)),
  )
}

@Preview
@Composable
private fun UpcomingUiPreview_Empty() = PreviewWrapper {
  UpcomingUi(
    state = previewState(
      DiscoverScanState.Completed(
        results = DiscoverScanResults(providerName = "Audible"),
        scannedAt = Instant.fromEpochMilliseconds(0),
        skippedCount = 0,
        failedCount = 0,
      ),
    ),
  )
}

private fun previewState(
  scanState: DiscoverScanState,
) = UpcomingUiState(
  scanState = scanState,
  eventSink = {},
)

private fun previewCompleted(
  skippedCount: Int = 0,
  failedCount: Int = 0,
) = DiscoverScanState.Completed(
  results = previewResults(),
  scannedAt = Instant.fromEpochMilliseconds(0),
  skippedCount = skippedCount,
  failedCount = failedCount,
)

private fun previewResults() = DiscoverScanResults(
  providerName = "Audible",
  upcoming = listOf(
    previewBook(
      "Wind and Truth",
      "The Stormlight Archive",
      position = 5.0,
      releaseDate = "2026-12-06",
    ),
    previewBook(
      "Isles of the Emberdark",
      "The Cosmere",
      position = null,
      releaseDate = "2026-11-04",
    ),
    previewBook(
      "Untitled Stormlight 6",
      "The Stormlight Archive",
      position = 6.0,
      releaseDate = null, // exercises the "To be announced" bucket
    ),
  ),
)

private fun previewBook(
  title: String,
  seriesName: String,
  position: Double?,
  releaseDate: String?,
) = DiscoveredBook(
  seriesId = "series_$seriesName",
  seriesName = seriesName,
  entry = ProviderSeriesEntry(
    providerBookId = "asin_$title",
    position = position,
    title = title,
    releaseDate = releaseDate,
    isReleased = false,
    providerUrl = "https://www.audible.com/pd/asin_$title",
    coverUrl = null, // exercises the book placeholder
  ),
  providerId = ProviderId.Audible,
)

// endregion
