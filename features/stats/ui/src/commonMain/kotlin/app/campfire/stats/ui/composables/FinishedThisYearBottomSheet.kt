// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.common.compose.widgets.LoadingState
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.model.ItemListenedTo
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.stats.api.StatsRepository
import app.campfire.user.api.MediaProgressRepository
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.finished_books_sheet_title
import campfire.features.stats.ui.generated.resources.finished_episodes_sheet_title
import campfire.features.stats.ui.generated.resources.finished_sheet_empty
import campfire.features.stats.ui.generated.resources.user_stats_error_message
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

sealed interface FinishedThisYearResult {
  data object None : FinishedThisYearResult
  data class Selected(val libraryItemId: LibraryItemId) : FinishedThisYearResult
}

data class FinishedThisYearInput(
  val mediaType: MediaType,
  val year: Int,
)

data class FinishedItem(
  val libraryItemId: LibraryItemId,
  val episodeId: String?,
  val title: String,
  val subtitle: String?,
  val coverImageUrl: String,
  val finishedAt: LocalDate,
)

suspend fun OverlayHost.showFinishedThisYearBottomSheet(
  mediaType: MediaType,
  year: Int,
): FinishedThisYearResult {
  return show(
    BottomSheetOverlay<FinishedThisYearInput, FinishedThisYearResult>(
      model = FinishedThisYearInput(
        mediaType = mediaType,
        year = year,
      ),
      onDismiss = { FinishedThisYearResult.None },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
    ) { input, overlayNavigator ->
      FinishedThisYearBottomSheet(
        input = input,
        onItemClick = { itemId ->
          overlayNavigator.finish(FinishedThisYearResult.Selected(itemId))
        },
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@ContributesTo(UserScope::class)
interface FinishedThisYearComponent {
  val statsRepository: StatsRepository
  val mediaProgressRepository: MediaProgressRepository
  val libraryItemRepository: LibraryItemRepository
  val dispatcherProvider: DispatcherProvider
}

@Composable
private fun rememberFinishedThisYearComponent(): FinishedThisYearComponent {
  return remember {
    ComponentHolder.component<FinishedThisYearComponent>()
  }
}

/**
 * State/DI layer for the finished-this-year sheet: resolves the component and loads the
 * finished items so [FinishedThisYearSheet] stays pure and previewable.
 */
@Composable
private fun FinishedThisYearBottomSheet(
  input: FinishedThisYearInput,
  onItemClick: (LibraryItemId) -> Unit,
  modifier: Modifier = Modifier,
  component: FinishedThisYearComponent = rememberFinishedThisYearComponent(),
) {
  val items by produceState<LoadState<out ImmutableList<FinishedItem>>>(LoadState.Loading, input) {
    value = try {
      LoadState.Loaded(component.loadFinishedThisYear(input.mediaType, input.year))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      LoadState.Error
    }
  }

  FinishedThisYearSheet(
    mediaType = input.mediaType,
    year = input.year,
    items = items,
    onItemClick = onItemClick,
    modifier = modifier,
  )
}

@Composable
internal fun FinishedThisYearSheet(
  mediaType: MediaType,
  year: Int,
  items: LoadState<out ImmutableList<FinishedItem>>,
  onItemClick: (LibraryItemId) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      text = when (mediaType) {
        MediaType.Book -> stringResource(Res.string.finished_books_sheet_title, year)
        MediaType.Podcast -> stringResource(Res.string.finished_episodes_sheet_title, year)
      },
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier
        .padding(
          horizontal = 24.dp,
          vertical = 8.dp,
        ),
    )

    when (items) {
      LoadState.Loading -> LoadingState(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 48.dp),
      )

      LoadState.Error -> EmptyState(
        message = stringResource(Res.string.user_stats_error_message),
      )

      is LoadState.Loaded -> if (items.data.isEmpty()) {
        EmptyState(
          message = stringResource(Res.string.finished_sheet_empty),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
        ) {
          items(
            items = items.data,
            key = { "${it.libraryItemId}:${it.episodeId.orEmpty()}" },
          ) { item ->
            FinishedItemRow(
              item = item,
              onClick = { onItemClick(item.libraryItemId) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FinishedItemRow(
  item: FinishedItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    leadingContent = {
      ItemImage(
        imageUrl = item.coverImageUrl,
        contentDescription = item.title,
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(8.dp)),
      )
    },
    headlineContent = {
      Text(
        text = item.title,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    },
    supportingContent = item.subtitle?.let { subtitle ->
      {
        Text(
          text = subtitle,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    },
    trailingContent = {
      Box(
        contentAlignment = Alignment.CenterEnd,
      ) {
        Text(
          text = item.finishedAt.asFinishedLabel(),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    colors = ListItemDefaults.colors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    modifier = modifier.clickable(onClick = onClick),
  )
}

private fun LocalDate.asFinishedLabel(): String {
  val month = month.name
    .lowercase()
    .replaceFirstChar { it.uppercase() }
    .take(3)
  return "$month $day"
}

private suspend fun FinishedThisYearComponent.loadFinishedThisYear(
  mediaType: MediaType,
  year: Int,
): ImmutableList<FinishedItem> = withContext(dispatcherProvider.io) {
  val timeZone = TimeZone.currentSystemDefault()
  fun MediaProgress.finishedDate(): LocalDate? = finishedAt
    ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).date }

  val rows = mediaProgressRepository.observeAllProgress().first()
    .filter { it.isFinished }
    .filter {
      when (mediaType) {
        MediaType.Book -> it.episodeId == null && it.mediaItemType == MediaType.Book
        MediaType.Podcast -> it.episodeId != null
      }
    }
    .filter { it.finishedDate()?.year == year }
    .sortedByDescending { it.finishedAt }

  when (mediaType) {
    MediaType.Book -> {
      // The listening stats payload already carries title/author/cover for anything the
      // user has listened to — resolve from it first and only hit the item store for
      // the rest (e.g. items marked finished without any recorded listening)
      val listenedById = runCatching { statsRepository.getUserStats().first().items }
        .getOrDefault(emptyList())
        .associateBy { it.id }
      val missingIds = rows
        .map { it.libraryItemId }
        .distinct()
        .filterNot { it in listenedById }
      val fetchedById = fetchLibraryItems(missingIds)

      rows.mapNotNull { progress ->
        val finishedDate = progress.finishedDate()!!
        listenedById[progress.libraryItemId]?.asFinishedItem(finishedDate)
          ?: fetchedById[progress.libraryItemId]?.asFinishedItem(finishedDate)
      }
    }

    MediaType.Podcast -> {
      // One fetch per podcast, not per episode — the episode titles live on the parent item
      val podcastsById = fetchLibraryItems(rows.map { it.libraryItemId }.distinct())

      rows.mapNotNull { progress ->
        val podcast = podcastsById[progress.libraryItemId] ?: return@mapNotNull null
        val episode = progress.episodeId?.let { episodeId ->
          (podcast.media as? Media.Podcast)?.episodes?.find { it.id == episodeId }
        }
        val podcastTitle = podcast.media.metadata.title

        FinishedItem(
          libraryItemId = progress.libraryItemId,
          episodeId = progress.episodeId,
          title = episode?.title ?: podcastTitle ?: return@mapNotNull null,
          subtitle = if (episode != null) podcastTitle else null,
          coverImageUrl = podcast.media.coverImageUrl,
          finishedAt = progress.finishedDate()!!,
        )
      }
    }
  }.toImmutableList()
}

private fun ItemListenedTo.asFinishedItem(finishedDate: LocalDate): FinishedItem? {
  return FinishedItem(
    libraryItemId = id,
    episodeId = null,
    title = mediaMetadata.title ?: return null,
    subtitle = mediaMetadata.authorName,
    coverImageUrl = coverImageUrl,
    finishedAt = finishedDate,
  )
}

private fun LibraryItem.asFinishedItem(finishedDate: LocalDate): FinishedItem? {
  return FinishedItem(
    libraryItemId = id,
    episodeId = null,
    title = media.metadata.title ?: return null,
    subtitle = media.metadata.authorName,
    coverImageUrl = media.coverImageUrl,
    finishedAt = finishedDate,
  )
}

private suspend fun FinishedThisYearComponent.fetchLibraryItems(
  ids: List<LibraryItemId>,
): Map<LibraryItemId, LibraryItem> = coroutineScope {
  val semaphore = Semaphore(MaxConcurrentItemFetches)
  ids
    .map { id ->
      async {
        semaphore.withPermit {
          try {
            libraryItemRepository.getLibraryItem(id)
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            null
          }
        }
      }
    }
    .awaitAll()
    .filterNotNull()
    .associateBy { it.id }
}

private const val MaxConcurrentItemFetches = 6
