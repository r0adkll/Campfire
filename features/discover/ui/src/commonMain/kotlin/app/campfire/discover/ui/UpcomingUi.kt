// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.UpcomingRelease
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ArrowBack
import app.campfire.common.compose.icons.rounded.Radar
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.permission.PermissionState
import app.campfire.common.compose.permission.rememberPostNotificationPermissionState
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.util.withDensity
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.discover.api.DiscoverScanState
import app.campfire.discover.api.screen.UpcomingScreen
import app.campfire.discover.ui.composables.UpcomingTimeline
import campfire.features.discover.ui.generated.resources.Res
import campfire.features.discover.ui.generated.resources.action_back
import campfire.features.discover.ui.generated.resources.discover_cancel_scan
import campfire.features.discover.ui.generated.resources.discover_empty_upcoming
import campfire.features.discover.ui.generated.resources.discover_scan_action
import campfire.features.discover.ui.generated.resources.discover_scan_progress
import campfire.features.discover.ui.generated.resources.discover_scan_rate_limited
import campfire.features.discover.ui.generated.resources.discover_scanning_title
import campfire.features.discover.ui.generated.resources.upcoming_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(UpcomingScreen::class, UserScope::class)
@Composable
fun UpcomingUi(
  state: UpcomingUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val listState = rememberLazyListState()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(stringResource(Res.string.upcoming_title)) },
        navigationIcon = {
          val backLabel = stringResource(Res.string.action_back)
          IconButtonTooltip(text = backLabel) {
            IconButton(onClick = { state.eventSink(UpcomingUiEvent.Back) }) {
              Icon(CampfireIcons.Rounded.ArrowBack, contentDescription = backLabel)
            }
          }
        },
        windowInsets = WindowInsets(),
        contentPadding = WindowInsets.statusBars.asPaddingValues(),
        scrollBehavior = scrollBehavior,
      )
    },
    floatingActionButton = {
      val expanded by remember {
        derivedStateOf {
          listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset < 50
        }
      }

      val bottomMargin = withDensity {
        CampfireWindowInsets.getBottom(this) + 16.dp.roundToPx()
      }
      AnimatedVisibility(
        visible = state.scanState is DiscoverScanState.Completed,
        enter = slideInVertically { it + bottomMargin },
        exit = slideOutVertically { it + bottomMargin },
      ) {
        // On Android the scan shows a progress notification, so ask for the
        // permission before the first explicit scan; the scan itself runs
        // whether or not it's granted.
        val permissionState = rememberPostNotificationPermissionState {
          state.eventSink(UpcomingUiEvent.Refresh)
        }
        SmallExtendedFloatingActionButton(
          expanded = expanded,
          text = { Text(stringResource(Res.string.discover_scan_action)) },
          icon = { Icon(CampfireIcons.Rounded.Radar, contentDescription = null) },
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          onClick = {
            when (permissionState) {
              is PermissionState.Granted -> state.eventSink(UpcomingUiEvent.Refresh)
              else -> permissionState.launchPermissionRequest()
            }
          },
          modifier = Modifier
            .navigationBarsPadding(),
        )
      }
    },
    contentWindowInsets = CampfireWindowInsets.exclude(WindowInsets.systemBars),
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    val scan = state.scanState
    val completed = scan as? DiscoverScanState.Completed

    Column(
      modifier = Modifier
        .fillMaxSize(),
    ) {
      Spacer(Modifier.height(paddingValues.calculateTopPadding()))

      if (scan is DiscoverScanState.Running) {
        ScanProgressHeader(
          done = scan.done,
          total = scan.total,
          onCancel = { state.eventSink(UpcomingUiEvent.CancelScan) },
        )
      }

      if (completed?.rateLimited == true) {
        Text(
          text = stringResource(Res.string.discover_scan_rate_limited),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }

      Box(modifier = Modifier.weight(1f)) {
        UpcomingTimeline(
          listState = listState,
          books = state.upcoming,
          onBookClick = { url -> state.eventSink(UpcomingUiEvent.BookClick(url)) },
        )

        if (completed != null && state.upcoming.isEmpty()) {
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

      Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    Text(
      text = stringResource(Res.string.discover_scanning_title),
      style = MaterialTheme.typography.titleSmall,
    )
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { if (total == 0) 0f else done.toFloat() / total },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = stringResource(Res.string.discover_scan_progress, done, total),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.weight(1f))
      val buttonSize = ButtonDefaults.ExtraSmallContainerHeight
      TextButton(
        onClick = onCancel,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(buttonSize),
      ) {
        Text(
          text = stringResource(Res.string.discover_cancel_scan),
          style = ButtonDefaults.textStyleFor(buttonSize),
        )
      }
    }
  }
}

// region — Previews —

@Composable
private fun PreviewWrapper(
  content: @Composable () -> Unit,
) {
  CampfireTheme {
    CompositionLocalProvider(
      LocalWindowSizeClass provides currentWindowSizeClass(),
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
      DiscoverScanState.Running(done = 12, total = 87),
      upcoming = previewUpcoming(),
    ),
  )
}

@Preview
@Composable
private fun UpcomingUiPreview_Completed() = PreviewWrapper {
  UpcomingUi(
    state = previewState(previewCompleted(), upcoming = previewUpcoming()),
  )
}

@Preview
@Composable
private fun UpcomingUiPreview_RateLimited() = PreviewWrapper {
  UpcomingUi(
    state = previewState(
      previewCompleted(rateLimited = true),
      upcoming = previewUpcoming(),
    ),
  )
}

@Preview
@Composable
private fun UpcomingUiPreview_Empty() = PreviewWrapper {
  UpcomingUi(state = previewState(previewCompleted()))
}

private fun previewState(
  scanState: DiscoverScanState,
  upcoming: List<UpcomingRelease> = emptyList(),
) = UpcomingUiState(
  scanState = scanState,
  upcoming = upcoming.toImmutableList(),
  eventSink = {},
)

private fun previewCompleted(
  skippedCount: Int = 0,
  failedCount: Int = 0,
  rateLimited: Boolean = false,
) = DiscoverScanState.Completed(
  scannedAt = Instant.fromEpochMilliseconds(0),
  skippedCount = skippedCount,
  failedCount = failedCount,
  rateLimited = rateLimited,
)

private fun previewUpcoming() = persistentListOf(
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
)

private fun previewBook(
  title: String,
  seriesName: String,
  position: Double?,
  releaseDate: String?,
) = UpcomingRelease(
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
