package app.campfire.podcasts.ui.latest

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.icons.filled.MarkFinished
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.widgets.ContentPagingScaffold
import app.campfire.common.compose.widgets.EpisodeListItem
import app.campfire.common.compose.widgets.EpisodeListItemDefaults
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.capitalized
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.mediaProgress
import app.campfire.playlists.api.dialog.PlaylistDialogResult
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.api.screen.LatestEpisodesScreen
import app.campfire.ui.appbar.CampfireAppBar
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import app.campfire.user.api.MediaProgressKey
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.action_add_episode_to_playlist
import campfire.features.podcasts.ui.generated.resources.action_mark_episode_finished
import campfire.features.podcasts.ui.generated.resources.action_mark_episode_not_finished
import campfire.features.podcasts.ui.generated.resources.latest_episodes_empty
import campfire.features.podcasts.ui.generated.resources.latest_episodes_image_content_description
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@CircuitInject(LatestEpisodesScreen::class, UserScope::class)
@Composable
fun LatestEpisodesUi(
  state: LatestEpisodesUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val appBarBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  AttachScrollBehaviorToLocalNavigationBar(appBarBehavior)

  var pendingPlaylistEpisode by remember { mutableStateOf<LatestEpisode?>(null) }
  pendingPlaylistEpisode?.let { request ->
    state.addToPlaylistDialog.Content(
      libraryItemId = request.episode.libraryItemId,
      itemTitle = request.podcastTitle ?: request.episode.title,
      episode = request.episode,
      onDismiss = { _: PlaylistDialogResult ->
        pendingPlaylistEpisode = null
      },
      modifier = Modifier,
    )
  }

  Scaffold(
    topBar = { campfireAppBar(Modifier, appBarBehavior) },
    modifier = modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    ContentPagingScaffold(
      lazyPagingItems = state.episodes,
      emptyMessage = stringResource(Res.string.latest_episodes_empty),
      indicatorPadding = paddingValues.calculateTopPadding(),
    ) {
      LatestEpisodesList(
        items = state.episodes,
        onPlayClick = { episode ->
          state.eventSink(
            LatestEpisodesUiEvent.PlayEpisode(
              libraryItemId = episode.episode.libraryItemId,
              episodeId = episode.episode.id,
            ),
          )
        },
        onRowClick = { episode ->
          state.eventSink(
            LatestEpisodesUiEvent.OpenPodcast(episode),
          )
        },
        onAddToPlaylistClick = { episode ->
          pendingPlaylistEpisode = episode
        },
        onMarkFinishedClick = { episode ->
          state.eventSink(LatestEpisodesUiEvent.MarkFinished(episode))
        },
        onMarkNotFinishedClick = { episode ->
          state.eventSink(LatestEpisodesUiEvent.MarkNotFinished(episode))
        },
        progressForEpisode = { episode ->
          state.mediaProgress[MediaProgressKey(episode.libraryItemId, episode.id)]
        },
        hasSessionForEpisode = { episode ->
          state.currentSession?.libraryItem?.id == episode.libraryItemId &&
            state.currentSession.episodeId == episode.id
        },
        contentPadding = paddingValues + PaddingValues(horizontal = 16.dp),
      )
    }
  }
}

@Composable
private fun LatestEpisodesList(
  items: LazyPagingItems<LatestEpisode>,
  onPlayClick: (LatestEpisode) -> Unit,
  onRowClick: (LatestEpisode) -> Unit,
  onAddToPlaylistClick: (LatestEpisode) -> Unit,
  onMarkFinishedClick: (LatestEpisode) -> Unit,
  onMarkNotFinishedClick: (LatestEpisode) -> Unit,
  progressForEpisode: (PodcastEpisode) -> MediaProgress?,
  hasSessionForEpisode: (PodcastEpisode) -> Boolean,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  val listState = rememberLazyListState()
  LazyColumn(
    state = listState,
    contentPadding = contentPadding,
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    items(
      count = items.itemCount,
      key = items.itemKey { it.episode.id },
      contentType = items.itemContentType { "latest-episode" },
    ) { index ->
      val item = items[index]
      if (item != null) {
        val previous = items.peekPrevious(index)
        val next = items.peekNext(index)
        val sectionDate = item.monthSectionDate()
        val isFirst = sectionDate != null &&
          (previous == null || previous.monthSectionDate() != sectionDate)
        val isLast = sectionDate != null &&
          (next == null || next.monthSectionDate() != sectionDate)

        Column(modifier = Modifier.fillMaxWidth().animateItem()) {
          if (isFirst) {
            MonthSectionHeader(
              date = sectionDate,
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 16.dp),
            )
          }

          val episodeProgress = progressForEpisode(item.episode)
          val isCurrentSession = hasSessionForEpisode(item.episode)
          val isFinished = episodeProgress?.isFinished == true
          EpisodeListItem(
            episode = item.episode,
            mediaProgress = episodeProgress,
            isCurrentSession = isCurrentSession,
            onClick = { onRowClick(item) },
            onPlayClick = { onPlayClick(item) },
            modifier = Modifier.fillMaxWidth(),
            leading = {
              Box(
                modifier = Modifier
                  .size(72.dp)
                  .clip(MaterialTheme.shapes.medium)
                  .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.medium,
                  ),
              ) {
                item.coverImageUrl?.let { url ->
                  ItemImage(
                    imageUrl = url,
                    contentDescription = stringResource(
                      Res.string.latest_episodes_image_content_description,
                      item.podcastTitle ?: "",
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
            },
            actions = {
              val addToPlaylistLabel = stringResource(Res.string.action_add_episode_to_playlist)
              IconButtonTooltip(text = addToPlaylistLabel) {
                IconButton(
                  onClick = {
                    onAddToPlaylistClick(item)
                  },
                  modifier = Modifier
                    .size(
                      IconButtonDefaults.extraSmallContainerSize(
                        IconButtonDefaults.IconButtonWidthOption.Uniform,
                      ),
                    ),
                  shape = IconButtonDefaults.extraSmallSquareShape,
                ) {
                  Icon(
                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = addToPlaylistLabel,
                    modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
                  )
                }
              }

              val finishedLabel = stringResource(
                if (isFinished) {
                  Res.string.action_mark_episode_not_finished
                } else {
                  Res.string.action_mark_episode_finished
                },
              )
              IconButtonTooltip(text = finishedLabel) {
                IconButton(
                  onClick = {
                    if (isFinished) {
                      onMarkNotFinishedClick(item)
                    } else {
                      onMarkFinishedClick(item)
                    }
                  },
                  modifier = Modifier
                    .size(
                      IconButtonDefaults.extraSmallContainerSize(
                        IconButtonDefaults.IconButtonWidthOption.Uniform,
                      ),
                    ),
                  shape = IconButtonDefaults.extraSmallSquareShape,
                ) {
                  Icon(
                    if (isFinished) {
                      Icons.Filled.MarkFinished
                    } else {
                      Icons.Rounded.MarkFinished
                    },
                    contentDescription = finishedLabel,
                    modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
                  )
                }
              }
            },
            shape = if (isFirst && !isLast) {
              EpisodeListItemDefaults.topItemShape()
            } else if (!isFirst && !isLast) {
              EpisodeListItemDefaults.middleItemShape()
            } else if (!isFirst && isLast) {
              EpisodeListItemDefaults.bottomItemShape()
            } else {
              EpisodeListItemDefaults.singleItemShape()
            },
            colors = EpisodeListItemDefaults.colors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
          )
        }
      }
    }
  }
}

@Composable
private fun MonthSectionHeader(
  date: LocalDate,
  modifier: Modifier = Modifier,
  currentYear: Int = remember {
    Clock.System.now().toLocalDateTime(TimeZone.UTC).year
  },
) {
  val label = if (date.year == currentYear) {
    date.month.name.capitalized()
  } else {
    "${date.month.name.capitalized()} ${date.year}"
  }
  Text(
    text = label,
    style = MaterialTheme.typography.labelLarge,
    modifier = modifier,
  )
}

private fun LatestEpisode.monthSectionDate(): LocalDate? {
  val date = episode.publishedAtMillis?.asDate() ?: return null
  return LocalDate(date.year, date.month, 1)
}

private fun LazyPagingItems<LatestEpisode>.peekPrevious(index: Int): LatestEpisode? {
  return if (index > 0) peek(index - 1) else null
}

private fun LazyPagingItems<LatestEpisode>.peekNext(index: Int): LatestEpisode? {
  return if (index < itemCount - 1) peek(index + 1) else null
}
