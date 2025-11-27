package app.campfire.stats.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.LocalSupportingContentState
import app.campfire.common.compose.layout.SupportingContentState
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.widgets.CampfireMediumTopAppBar
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.LoadingState
import app.campfire.common.screens.StatisticsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.AuthorWithCount
import app.campfire.core.model.ItemListenedTo
import app.campfire.core.model.LargestItem
import app.campfire.core.model.LongestItem
import app.campfire.core.model.PlaybackSession
import app.campfire.stats.ui.StatsUiModel.ItemsListenedTo
import app.campfire.stats.ui.StatsUiModel.RecentSession
import app.campfire.stats.ui.StatsUiModel.UserTotals
import app.campfire.stats.ui.StatsUiModel.WeeklyListening
import app.campfire.stats.ui.composables.ItemsListenedToRow
import app.campfire.stats.ui.composables.LargestItemBarChart
import app.campfire.stats.ui.composables.LibraryTotalStatsCard
import app.campfire.stats.ui.composables.LongestItemBarChart
import app.campfire.stats.ui.composables.RecentSessionListItem
import app.campfire.stats.ui.composables.StatsHeader
import app.campfire.stats.ui.composables.TopAuthorsBarChart
import app.campfire.stats.ui.composables.TotalStatsCard
import app.campfire.stats.ui.composables.WeeklyListeningCard
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.stats_library
import campfire.features.stats.ui.generated.resources.stats_user
import campfire.features.stats.ui.generated.resources.user_stats_error_message
import campfire.features.stats.ui.generated.resources.user_stats_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(StatisticsScreen::class, UserScope::class)
@Composable
fun StatsUi(
  state: StatsUiState,
  modifier: Modifier = Modifier,
) {
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)

  val supportingContentState by rememberUpdatedState(LocalSupportingContentState.current)
  val isTwoPaneLayout = (
    windowSizeClass.isSupportingPaneEnabled &&
      supportingContentState == SupportingContentState.Closed
    ) ||
    windowSizeClass.widthSizeClass >= WindowWidthSizeClass.ExtraLarge

  var isUserStats by rememberSaveable { mutableStateOf(true) }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    topBar = {
      if (!isTwoPaneLayout) {
        CampfireMediumTopAppBar(
          scrollBehavior = scrollBehavior,
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(stringResource(Res.string.user_stats_title))
              Spacer(Modifier.weight(1f))
              StatsChoiceBar(
                isUser = isUserStats,
                onChange = { isUserStats = it },
              )
              Spacer(Modifier.width(16.dp))
            }
          },
          navigationIcon = {
            IconButton(
              onClick = { state.eventSink(StatsUiEvent.Back) },
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
          },
        )
      } else {
        CampfireTopAppBar(
          scrollBehavior = scrollBehavior,
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(stringResource(Res.string.user_stats_title))
//              Spacer(Modifier.weight(1f))
//              StatsChoiceBar(
//                isUser = isUserStats,
//                onChange = { isUserStats = it },
//              )
//              Spacer(Modifier.width(16.dp))
            }
          },
          navigationIcon = {
            IconButton(
              onClick = { state.eventSink(StatsUiEvent.Back) },
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
          },
        )
      }
    },
    contentWindowInsets = CampfireWindowInsets,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    if (isTwoPaneLayout) {
      TwoPaneLayout(
        userContent = {
          StatsContent(
            models = state.listeningStats,
            onEvent = state.eventSink,
            contentPadding = paddingValues,
          )
        },
        libraryContent = {
          StatsContent(
            models = state.libraryStats,
            onEvent = state.eventSink,
            contentPadding = paddingValues,
          )
        },
      )
    } else {
      StatsSwitcher(
        isUser = isUserStats,
        userContent = {
          StatsContent(
            models = state.listeningStats,
            onEvent = state.eventSink,
            contentPadding = paddingValues,
          )
        },
        libraryContent = {
          StatsContent(
            models = state.libraryStats,
            onEvent = state.eventSink,
            contentPadding = paddingValues,
          )
        },
      )
    }
  }
}

@Composable
private fun TwoPaneLayout(
  userContent: @Composable () -> Unit,
  libraryContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier.weight(1f),
    ) {
      userContent()
    }
    Box(
      modifier = Modifier.weight(1f),
    ) {
      libraryContent()
    }
  }
}

@Composable
private fun StatsChoiceBar(
  isUser: Boolean,
  onChange: (isUser: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  SingleChoiceSegmentedButtonRow(
    modifier = modifier,
  ) {
    SegmentedButton(
      shape = SegmentedButtonDefaults.itemShape(0, count = 2),
      selected = isUser,
      onClick = { onChange(true) },
    ) {
      Text(stringResource(Res.string.stats_user))
    }
    SegmentedButton(
      shape = SegmentedButtonDefaults.itemShape(1, count = 2),
      selected = !isUser,
      onClick = { onChange(false) },
    ) {
      Text(stringResource(Res.string.stats_library))
    }
  }
}

@Composable
private fun StatsSwitcher(
  isUser: Boolean,
  modifier: Modifier = Modifier,
  userContent: @Composable () -> Unit,
  libraryContent: @Composable () -> Unit,
) {
  AnimatedContent(
    targetState = isUser,
    transitionSpec = {
      slideInHorizontally {
        if (targetState) -it else it
      } togetherWith
        slideOutHorizontally {
          if (targetState) it else -it
        }
    },
    modifier = modifier,
  ) { user ->
    if (user) {
      key("user") {
        userContent()
      }
    } else {
      key("library") {
        libraryContent()
      }
    }
  }
}

@Composable
private fun StatsContent(
  models: LoadState<out List<StatsUiModel>>,
  onEvent: (StatsUiEvent) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(0.dp),
) {
  when (models) {
    LoadState.Loading -> LoadingState(modifier.padding(contentPadding).fillMaxSize())
    LoadState.Error -> EmptyState(
      message = stringResource(Res.string.user_stats_error_message),
      modifier = modifier.padding(contentPadding),
    )

    is LoadState.Loaded -> LoadedContent(
      stats = models.data,
      onItemClick = { item ->
        onEvent(StatsUiEvent.ItemClick(item.id))
      },
      onSessionClick = { session ->
        onEvent(StatsUiEvent.SessionClick(session))
      },
      onLargestItemClick = { item ->
        onEvent(StatsUiEvent.ItemClick(item.id))
      },
      onLongestItemClick = { item ->
        onEvent(StatsUiEvent.ItemClick(item.id))
      },
      onTopAuthorClick = { author ->
        onEvent(StatsUiEvent.AuthorClick(author.id, author.name))
      },
      contentPadding = contentPadding,
      modifier = modifier,
    )
  }
}

@Composable
private fun LoadedContent(
  stats: List<StatsUiModel>,
  onItemClick: (ItemListenedTo) -> Unit,
  onSessionClick: (PlaybackSession) -> Unit,
  onLargestItemClick: (LargestItem) -> Unit,
  onLongestItemClick: (LongestItem) -> Unit,
  onTopAuthorClick: (AuthorWithCount) -> Unit,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = contentPadding,
  ) {
    items(
      items = stats,
      key = { it.id },
      contentType = { it::class.simpleName },
    ) { model ->
      when (model) {
        is StatsUiModel.Header -> StatsHeader(model)
        is UserTotals -> TotalStatsCard(model)
        is ItemsListenedTo -> ItemsListenedToRow(
          itemsListenedTo = model,
          onItemClick = onItemClick,
        )

        is WeeklyListening -> WeeklyListeningCard(model)
        is RecentSession -> RecentSessionListItem(
          model = model,
          onClick = { onSessionClick(model.session) },
        )

        is StatsUiModel.LibraryTotals -> LibraryTotalStatsCard(model)
        is StatsUiModel.LargestItems -> LargestItemBarChart(
          model = model,
          onItemClick = onLargestItemClick,
        )

        is StatsUiModel.LongestItems -> LongestItemBarChart(
          model = model,
          onItemClick = onLongestItemClick,
        )

        is StatsUiModel.TopAuthors -> TopAuthorsBarChart(
          model = model,
          onItemClick = onTopAuthorClick,
        )

        is StatsUiModel.TopGenres -> TODO()
      }
    }
  }
}
