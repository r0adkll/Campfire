package app.campfire.podcasts.ui.builder

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.podcasts.api.screen.AddPodcastBuilderScreen
import app.campfire.podcasts.ui.AddPodcastSharedTransitionKey
import app.campfire.podcasts.ui.builder.composables.AutoDownloadRow
import app.campfire.podcasts.ui.builder.composables.CoverPreview
import app.campfire.podcasts.ui.builder.composables.EpisodeTypeRow
import app.campfire.podcasts.ui.builder.composables.ErrorDialog
import app.campfire.podcasts.ui.builder.composables.ExplicitRow
import app.campfire.podcasts.ui.builder.composables.FolderSection
import app.campfire.podcasts.ui.builder.composables.SubmitBar
import app.campfire.podcasts.ui.builder.composables.episodesSection
import app.campfire.ui.navigation.bar.CampfireNavigationBarWindowInsets
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_back
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_field_author
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_field_description
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_field_title
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@CircuitInject(AddPodcastBuilderScreen::class, UserScope::class)
@Composable
fun AddPodcastBuilderUi(
  state: AddPodcastBuilderUiState,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val listState = rememberLazyListState()

  Scaffold(
    modifier = modifier
      .sharedBounds(
        sharedContentState = rememberSharedContentState(
          AddPodcastSharedTransitionKey(
            id = state.sharedTransitionKey,
            type = AddPodcastSharedTransitionKey.ElementType.Bounds,
          ),
        ),
        animatedVisibilityScope = requireAnimatedScope(
          SharedElementTransitionScope.AnimatedScope.Navigation,
        ),
      )
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      CampfireTopAppBar(
        title = { Text(stringResource(Res.string.add_podcast_builder_title)) },
        navigationIcon = {
          val backLabel = stringResource(Res.string.add_podcast_builder_back)
          IconButtonTooltip(text = backLabel) {
            IconButton(onClick = { state.eventSink(AddPodcastBuilderUiEvent.Back) }) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backLabel)
            }
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    bottomBar = {
      SubmitBar(
        isSubmitting = state.isSubmitting,
        canSubmit = state.foldersState is FoldersState.Loaded,
        onSubmit = { state.eventSink(AddPodcastBuilderUiEvent.Submit) },
      )
    },
    contentWindowInsets = CampfireNavigationBarWindowInsets,
  ) { paddingValues ->
    @Composable
    fun ItemSpacer(space: Dp = 16.dp) {
      Spacer(Modifier.height(space))
    }

    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = paddingValues,
    ) {
      item("cover") {
        CoverPreview(
          coverUrl = state.coverUrl,
          title = state.titleState.text.toString(),
          sharedTransitionKey = state.sharedTransitionKey,
        )
      }
      item("title") {
        ItemSpacer()
        OutlinedTextField(
          state = state.titleState,
          label = { Text(stringResource(Res.string.add_podcast_builder_field_title)) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        )
      }
      item("author") {
        ItemSpacer()
        OutlinedTextField(
          state = state.authorState,
          label = { Text(stringResource(Res.string.add_podcast_builder_field_author)) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        )
      }
      item("description") {
        ItemSpacer()
        OutlinedTextField(
          state = state.descriptionState,
          label = { Text(stringResource(Res.string.add_podcast_builder_field_description)) },
          lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 6),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        )
      }
      item("folder") {
        ItemSpacer()
        FolderSection(
          title = state.titleState.text.toString(),
          foldersState = state.foldersState,
          onSelect = { state.eventSink(AddPodcastBuilderUiEvent.FolderSelected(it)) },
          onRetry = { state.eventSink(AddPodcastBuilderUiEvent.RetryFolders) },
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
      item("episode-type") {
        ItemSpacer()
        EpisodeTypeRow(
          selected = state.episodeType,
          onSelect = { state.eventSink(AddPodcastBuilderUiEvent.EpisodeTypeSelected(it)) },
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
      item("explicit") {
        ItemSpacer(8.dp)
        ExplicitRow(
          enabled = state.explicitEnabled,
          onToggle = { state.eventSink(AddPodcastBuilderUiEvent.ExplicitToggled(it)) },
          shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp,
          ),
          modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(2.dp))
        AutoDownloadRow(
          enabled = state.autoDownloadEnabled,
          onToggle = { state.eventSink(AddPodcastBuilderUiEvent.AutoDownloadToggled(it)) },
          shape = RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 4.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp,
          ),
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }

      item {
        ItemSpacer(24.dp)
      }

      episodesSection(
        feedState = state.feedState,
        selectedUrls = state.selectedEpisodeUrls,
        onToggle = { state.eventSink(AddPodcastBuilderUiEvent.EpisodeSelectionToggled(it)) },
        onSelectAll = { state.eventSink(AddPodcastBuilderUiEvent.SelectAllEpisodes) },
        onClear = { state.eventSink(AddPodcastBuilderUiEvent.ClearEpisodeSelection) },
        onRetry = { state.eventSink(AddPodcastBuilderUiEvent.RetryFeed) },
      )
    }
  }

  state.submitError?.let { error ->
    ErrorDialog(
      error = error,
      onDismiss = { state.eventSink(AddPodcastBuilderUiEvent.DismissError) },
    )
  }
}
