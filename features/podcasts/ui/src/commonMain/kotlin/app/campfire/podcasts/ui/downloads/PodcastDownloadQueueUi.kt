// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.screen.PodcastDownloadQueueScreen
import app.campfire.podcasts.ui.downloads.composables.PodcastDownloadGroupCard
import app.campfire.ui.appbar.CampfireAppBar
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import app.campfire.ui.navigation.bar.CampfireNavigationBarWindowInsets
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.download_queue_clear_dialog_body
import campfire.features.podcasts.ui.generated.resources.download_queue_clear_dialog_cancel
import campfire.features.podcasts.ui.generated.resources.download_queue_clear_dialog_confirm
import campfire.features.podcasts.ui.generated.resources.download_queue_clear_dialog_title
import campfire.features.podcasts.ui.generated.resources.download_queue_empty
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

@CircuitInject(PodcastDownloadQueueScreen::class, UserScope::class)
@Composable
fun PodcastDownloadQueueUi(
  state: PodcastDownloadQueueUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val appBarBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  AttachScrollBehaviorToLocalNavigationBar(appBarBehavior)

  var pendingClearLibraryItemId by remember { mutableStateOf<LibraryItemId?>(null) }
  pendingClearLibraryItemId?.let { libraryItemId ->
    ClearQueueConfirmDialog(
      onConfirm = {
        state.eventSink(PodcastDownloadQueueUiEvent.ClearQueue(libraryItemId))
        pendingClearLibraryItemId = null
      },
      onDismiss = { pendingClearLibraryItemId = null },
    )
  }

  Scaffold(
    topBar = { campfireAppBar(Modifier, appBarBehavior) },
    modifier = modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireNavigationBarWindowInsets,
  ) { paddingValues ->
    when {
      state.groups.isEmpty() -> EmptyState(
        paddingValues = paddingValues,
        showProgress = state.isRefreshing,
      )

      else -> LazyColumn(
        contentPadding = PaddingValues(
          top = paddingValues.calculateTopPadding() + 16.dp,
          bottom = paddingValues.calculateBottomPadding() + 24.dp,
          start = 16.dp,
          end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(items = state.groups, key = { it.libraryItemId }) { group ->
          PodcastDownloadGroupCard(
            group = group,
            showClearQueue = state.isAdmin,
            onClick = {
              state.eventSink(PodcastDownloadQueueUiEvent.OpenPodcast(group.libraryItemId))
            },
            onClearQueueClick = {
              pendingClearLibraryItemId = group.libraryItemId
            },
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyState(
  paddingValues: PaddingValues,
  showProgress: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(paddingValues)
      .padding(horizontal = 32.dp),
    contentAlignment = Alignment.Center,
  ) {
    if (showProgress) {
      LinearProgressIndicator()
    } else {
      EmptyState(stringResource(Res.string.download_queue_empty))
    }
  }
}

@Composable
private fun ClearQueueConfirmDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.download_queue_clear_dialog_title)) },
    text = { Text(stringResource(Res.string.download_queue_clear_dialog_body)) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(Res.string.download_queue_clear_dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.download_queue_clear_dialog_cancel))
      }
    },
    modifier = modifier,
  )
}

// region — Previews —

private val NoOpAppBar: CampfireAppBar = { _, _ -> }

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
private fun PodcastDownloadQueueUiPreview_Empty() = PreviewWrapper {
  PodcastDownloadQueueUi(
    state = previewState(groups = persistentListOf()),
    campfireAppBar = NoOpAppBar,
  )
}

@Preview
@Composable
private fun PodcastDownloadQueueUiPreview_Loading() = PreviewWrapper {
  PodcastDownloadQueueUi(
    state = previewState(
      groups = persistentListOf(),
      isRefreshing = true,
    ),
    campfireAppBar = NoOpAppBar,
  )
}

@Preview
@Composable
private fun PodcastDownloadQueueUiPreview_PopulatedAdmin() = PreviewWrapper {
  PodcastDownloadQueueUi(
    state = previewState(
      groups = previewGroups(),
      isAdmin = true,
    ),
    campfireAppBar = NoOpAppBar,
  )
}

@Preview
@Composable
private fun PodcastDownloadQueueUiPreview_PopulatedNonAdmin() = PreviewWrapper {
  PodcastDownloadQueueUi(
    state = previewState(
      groups = previewGroups(),
      isAdmin = false,
    ),
    campfireAppBar = NoOpAppBar,
  )
}

private fun previewState(
  groups: ImmutableList<DownloadGroup>,
  isAdmin: Boolean = true,
  isRefreshing: Boolean = false,
) = PodcastDownloadQueueUiState(
  groups = groups,
  isAdmin = isAdmin,
  isRefreshing = isRefreshing,
  eventSink = {},
)

private fun previewGroups() = persistentListOf(
  DownloadGroup(
    libraryItemId = "li_ezra",
    podcastTitle = "The Ezra Klein Show",
    downloads = listOf(
      previewDownload(
        id = "d_ezra_1",
        libraryItemId = "li_ezra",
        episodeDisplayTitle = "The view from inside the administration",
        state = RemoteEpisodeDownload.State.Downloading,
      ),
      previewDownload(
        id = "d_ezra_2",
        libraryItemId = "li_ezra",
        episodeDisplayTitle = "What we lose when we lose attention",
      ),
      previewDownload(
        id = "d_ezra_3",
        libraryItemId = "li_ezra",
        episodeDisplayTitle = "Best of: the climate question we keep avoiding",
      ),
    ).toImmutableList(),
  ),
  DownloadGroup(
    libraryItemId = "li_daily",
    podcastTitle = "The Daily",
    downloads = listOf(
      previewDownload(
        id = "d_daily_1",
        libraryItemId = "li_daily",
        episodeDisplayTitle = "Tuesday, May 21, 2026",
      ),
    ).toImmutableList(),
  ),
  DownloadGroup(
    libraryItemId = "li_short",
    podcastTitle = "Short Wave",
    downloads = listOf(
      previewDownload(
        id = "d_short_1",
        libraryItemId = "li_short",
        episodeDisplayTitle = null, // exercises the "Untitled episode" fallback
      ),
      previewDownload(
        id = "d_short_2",
        libraryItemId = "li_short",
        episodeDisplayTitle = "Why your dog's gut microbiome matters",
      ),
    ).toImmutableList(),
  ),
)

private fun previewDownload(
  id: String,
  libraryItemId: String,
  episodeDisplayTitle: String?,
  state: RemoteEpisodeDownload.State = RemoteEpisodeDownload.State.Queued,
) = RemoteEpisodeDownload(
  id = id,
  libraryItemId = libraryItemId,
  libraryId = "lib_podcasts",
  url = "https://feed.example.com/$id.mp3",
  episodeDisplayTitle = episodeDisplayTitle,
  podcastTitle = null,
  state = state,
  createdAt = 0L,
  startedAt = if (state == RemoteEpisodeDownload.State.Downloading) 0L else null,
)

// endregion
