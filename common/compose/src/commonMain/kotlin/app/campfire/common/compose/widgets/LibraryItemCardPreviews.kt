// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import app.campfire.core.offline.OfflineStatus
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout

// region Sample axes

/**
 * Every kind of item a [LibraryItemCard] is drawn for, each picking a different branch of the
 * card's cover decoration or information lines.
 */
internal enum class CardSampleItem(val label: String) {
  Audiobook("Audiobook"),
  LongMetadata("Long title + author"),
  MissingMetadata("No title, author or cover"),
  Ebook("Ebook only"),
  Podcast("Podcast"),
  PodcastUnfinished("Podcast, 12 unfinished"),
  PodcastUnfinishedCapped("Podcast, 300 unfinished"),
  Episode("Episode"),
  EpisodeUnnumbered("Episode, no number"),
  ;

  /** A podcast as a whole, which carries no progress of its own. */
  val isPodcastShow: Boolean
    get() = this == Podcast || this == PodcastUnfinished || this == PodcastUnfinishedCapped

  val isEpisode: Boolean
    get() = this == Episode || this == EpisodeUnnumbered
}

/**
 * The progress and download states layered onto a card's cover, alone and in the combinations
 * that share it (a finished check top-start, a download badge top-end, a progress bar along the
 * bottom).
 */
internal enum class CardSampleState(
  val label: String,
  val progress: MediaProgress?,
  val offlineStatus: OfflineStatus,
) {
  Plain("Plain", null, OfflineStatus.None),
  InProgress("In progress", mediaProgress(progress = 0.64f), OfflineStatus.None),
  Finished("Finished", mediaProgress(progress = 1f, isFinished = true), OfflineStatus.None),
  Queued("Download queued", null, OfflineStatus.Queued),
  Downloading("Downloading", null, OfflineStatus.Downloading(0.4f)),
  Downloaded("Downloaded", null, OfflineStatus.Available),
  Failed("Download failed", null, OfflineStatus.Failed),
  InProgressDownloaded("In progress + downloaded", mediaProgress(progress = 0.64f), OfflineStatus.Available),
  FinishedDownloaded(
    "Finished + downloaded",
    mediaProgress(progress = 1f, isFinished = true),
    OfflineStatus.Available,
  ),
  ;

  fun appliesTo(item: CardSampleItem): Boolean = progress == null || !item.isPodcastShow
}

/**
 * Where the app puts a [LibraryItemCard], at the width it gets on a 412dp-wide phone. Author
 * detail draws the card exactly as [LibraryGrid] does, so it has no entry of its own.
 */
internal enum class CardSampleContext(
  val label: String,
  val width: Dp,
) {
  HomeShelf("Home shelf", 180.dp),
  LibraryGrid("Library grid", 121.dp),
  LibraryGridDense("Library grid, dense", 89.dp),
  Search("Search results", 121.dp),
  SeriesDetail("Series detail", 121.dp),
  DetailPane("Series detail, wide detail pane", 121.dp),
  CollectionEditing("Collection, editing", 121.dp),
  CollectionSelected("Collection, editing + selected", 121.dp),
  ;

  val dense: Boolean get() = this == LibraryGridDense

  val contentLayout: ContentLayout
    get() = if (this == DetailPane) ContentLayout.Supporting else ContentLayout.Root

  /** Whether the app ever shows [item] here. */
  fun shows(item: CardSampleItem): Boolean = when (item) {
    CardSampleItem.Episode, CardSampleItem.EpisodeUnnumbered -> this == HomeShelf
    // Only the library grid counts a podcast's unfinished episodes
    CardSampleItem.PodcastUnfinished, CardSampleItem.PodcastUnfinishedCapped ->
      this == LibraryGrid || this == LibraryGridDense
    // Series and collections hold books only
    CardSampleItem.Podcast -> this == HomeShelf || this == LibraryGrid || this == LibraryGridDense || this == Search
    else -> true
  }
}

/** One card as the app would draw it. */
internal data class CardSample(
  val item: CardSampleItem,
  val state: CardSampleState,
  val context: CardSampleContext,
) {
  val name: String get() = "${context.label} / ${item.label} / ${state.label}"
}

internal val AllCardSamples: List<CardSample> =
  CardSampleContext.entries.flatMap { context ->
    CardSampleItem.entries.filter(context::shows).flatMap { item ->
      CardSampleState.entries.filter { it.appliesTo(item) }.map { state -> CardSample(item, state, context) }
    }
  }

// endregion

// region Sample rendering

/**
 * [sample]'s card, configured the way its context's screen configures it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LibraryItemCardSample(
  sample: CardSample,
  modifier: Modifier = Modifier,
) {
  val (item, state, context) = sample
  val editing = context == CardSampleContext.CollectionEditing ||
    context == CardSampleContext.CollectionSelected
  CompositionLocalProvider(LocalContentLayout provides context.contentLayout) {
    LibraryItemCard(
      item = item.libraryItem,
      episode = item.episode,
      // Collections handle the click around the card, so it gets none of its own
      onClick = if (editing) null else ({}),
      modifier = modifier.width(context.width),
      sharedTransitionKey = sample.name,
      isSelectable = editing,
      selected = context == CardSampleContext.CollectionSelected,
      showInformation = !context.dense,
      subtitle = if (context.isSeries) "Book 2 · 2014" else null,
      marqueeEnabled = false,
      offlineStatus = state.offlineStatus,
      unfinishedEpisodes = item.unfinishedEpisodes,
      progress = state.progress,
      shape = if (context.dense) MaterialTheme.shapes.medium else MaterialTheme.shapes.largeIncreased,
      colors = CardDefaults.elevatedCardColors(
        containerColor = if (context == CardSampleContext.Search) {
          MaterialTheme.colorScheme.surfaceContainerLow
        } else {
          MaterialTheme.colorScheme.surfaceContainerHigh
        },
      ),
    )
  }
}

private val CardSampleContext.isSeries: Boolean
  get() = this == CardSampleContext.SeriesDetail || this == CardSampleContext.DetailPane

/**
 * Every state of every item [context] shows: a row per [CardSampleState], a column per
 * [CardSampleItem]. Combinations the app never draws are left empty.
 */
@Composable
internal fun LibraryItemCardStateGrid(
  context: CardSampleContext,
  modifier: Modifier = Modifier,
) {
  val items = CardSampleItem.entries.filter(context::shows)
  SampleGrid(
    title = "LibraryItemCard · ${context.label} (${context.width.value.toInt()}dp)",
    columns = items.map { it.label },
    rows = CardSampleState.entries.map { it.label },
    cellWidth = context.width,
    rowBackground = { context.contentLayout },
    modifier = modifier,
  ) { column, row ->
    val item = items[column]
    val state = CardSampleState.entries[row]
    if (state.appliesTo(item)) {
      LibraryItemCardSample(CardSample(item, state, context))
    }
  }
}

/**
 * Every context against every item, in [state]: a row per [CardSampleContext], a column per
 * [CardSampleItem]. Combinations the app never draws are left empty.
 */
@Composable
internal fun LibraryItemCardContextGrid(
  modifier: Modifier = Modifier,
  state: CardSampleState = CardSampleState.Plain,
) {
  val items = CardSampleItem.entries
  val contexts = CardSampleContext.entries
  SampleGrid(
    title = "LibraryItemCard · every context (${state.label})",
    columns = items.map { it.label },
    rows = contexts.map { "${it.label} (${it.width.value.toInt()}dp)" },
    cellWidth = contexts.maxOf { it.width },
    rowBackground = { row -> contexts[row].contentLayout },
    modifier = modifier,
  ) { column, row ->
    val item = items[column]
    val context = contexts[row]
    if (context.shows(item) && state.appliesTo(item)) {
      LibraryItemCardSample(CardSample(item, state, context))
    }
  }
}

@Composable
private fun SampleGrid(
  title: String,
  columns: List<String>,
  rows: List<String>,
  cellWidth: Dp,
  rowBackground: (row: Int) -> ContentLayout,
  modifier: Modifier = Modifier,
  cell: @Composable (column: Int, row: Int) -> Unit,
) {
  Column(
    modifier = modifier.padding(SampleGridGutter),
    verticalArrangement = Arrangement.spacedBy(SampleGridGutter),
  ) {
    Text(title, style = MaterialTheme.typography.headlineSmall)

    Row(horizontalArrangement = Arrangement.spacedBy(SampleGridGutter)) {
      Box(Modifier.width(SampleRowLabelWidth))
      columns.forEach { label ->
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.width(cellWidth),
        )
      }
    }

    rows.forEachIndexed { row, label ->
      // A detail pane's cards sit on its tonal surface, as they do in the app
      val supporting = rowBackground(row) == ContentLayout.Supporting
      Surface(
        tonalElevation = if (supporting) 1.dp else 0.dp,
        color = if (supporting) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.medium,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(SampleGridGutter),
          modifier = Modifier.padding(vertical = 8.dp),
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(SampleRowLabelWidth),
          )
          columns.indices.forEach { column ->
            Box(Modifier.width(cellWidth)) {
              cell(column, row)
            }
          }
        }
      }
    }
  }
}

/**
 * Hosts card samples the way a screen does: in a shared-transition layout, with stand-in cover
 * art. Covers only resolve through the preview handler while `LocalInspectionMode` is on, which
 * an IDE preview sets and a render test must provide.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalCoilApi::class)
@Composable
internal fun CardSampleHost(
  darkTheme: Boolean,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalAsyncImagePreviewHandler provides SampleCoverHandler,
    LocalContentLayout provides ContentLayout.Root,
  ) {
    CampfireTheme(useDarkColors = darkTheme) {
      PreviewSharedElementTransitionLayout {
        Surface(color = MaterialTheme.colorScheme.background) {
          content()
        }
      }
    }
  }
}

private val SampleGridGutter = 16.dp
private val SampleRowLabelWidth = 132.dp

// endregion

// region Previews

@Preview(name = "Home shelf · light", widthDp = 1900, heightDp = 2600)
@Composable
internal fun LibraryItemCardHomeShelfPreview() = CardSampleHost(darkTheme = false) {
  LibraryItemCardStateGrid(CardSampleContext.HomeShelf)
}

@Preview(name = "Home shelf · dark", widthDp = 1900, heightDp = 2600)
@Composable
internal fun LibraryItemCardHomeShelfDarkPreview() = CardSampleHost(darkTheme = true) {
  LibraryItemCardStateGrid(CardSampleContext.HomeShelf)
}

@Preview(name = "Library grid · light", widthDp = 1100, heightDp = 1900)
@Composable
internal fun LibraryItemCardLibraryGridPreview() = CardSampleHost(darkTheme = false) {
  LibraryItemCardStateGrid(CardSampleContext.LibraryGrid)
}

@Preview(name = "Library grid · dark", widthDp = 1100, heightDp = 1900)
@Composable
internal fun LibraryItemCardLibraryGridDarkPreview() = CardSampleHost(darkTheme = true) {
  LibraryItemCardStateGrid(CardSampleContext.LibraryGrid)
}

@Preview(name = "Library grid, dense · light", widthDp = 900, heightDp = 1200)
@Composable
internal fun LibraryItemCardDenseGridPreview() = CardSampleHost(darkTheme = false) {
  LibraryItemCardStateGrid(CardSampleContext.LibraryGridDense)
}

@Preview(name = "Library grid, dense · dark", widthDp = 900, heightDp = 1200)
@Composable
internal fun LibraryItemCardDenseGridDarkPreview() = CardSampleHost(darkTheme = true) {
  LibraryItemCardStateGrid(CardSampleContext.LibraryGridDense)
}

@Preview(name = "Every context · light", widthDp = 1900, heightDp = 1900)
@Composable
internal fun LibraryItemCardContextsPreview() = CardSampleHost(darkTheme = false) {
  LibraryItemCardContextGrid()
}

@Preview(name = "Every context · dark", widthDp = 1900, heightDp = 1900)
@Composable
internal fun LibraryItemCardContextsDarkPreview() = CardSampleHost(darkTheme = true) {
  LibraryItemCardContextGrid()
}

/** One preview per configuration in [AllCardSamples], for iterating on a single card. */
internal class CardSampleProvider : PreviewParameterProvider<CardSample> {
  override val values: Sequence<CardSample> = AllCardSamples.asSequence()

  override fun getDisplayName(index: Int): String = AllCardSamples[index].name
}

@Preview
@Composable
internal fun LibraryItemCardConfigurationPreview(
  @PreviewParameter(CardSampleProvider::class) sample: CardSample,
) = CardSampleHost(darkTheme = false) {
  Box(Modifier.padding(16.dp)) {
    LibraryItemCardSample(sample)
  }
}

// endregion

// region Fixtures

private val CardSampleItem.libraryItem: LibraryItem
  get() = when (this) {
    CardSampleItem.Audiobook -> sampleBook(id = "sample_book", cover = this)
    CardSampleItem.LongMetadata -> sampleBook(
      id = "sample_long",
      cover = this,
      title = "The Extraordinarily Long and Winding Chronicles of Everything That Happened Next",
      author = "Bartholomew Montgomery-Fitzgerald III",
    )
    CardSampleItem.MissingMetadata -> sampleBook(id = "sample_missing", cover = null, title = null, author = null)
    CardSampleItem.Ebook -> sampleBook(id = "sample_ebook", cover = this, ebookFormat = "epub")
    CardSampleItem.Podcast,
    CardSampleItem.Episode,
    CardSampleItem.EpisodeUnnumbered,
    -> samplePodcast(id = "sample_podcast", cover = CardSampleItem.Podcast)
    CardSampleItem.PodcastUnfinished -> samplePodcast(id = "sample_podcast_unfinished", cover = this)
    CardSampleItem.PodcastUnfinishedCapped -> samplePodcast(id = "sample_podcast_capped", cover = this)
  }

private val CardSampleItem.episode: PodcastEpisode?
  get() = when (this) {
    CardSampleItem.Episode -> sampleEpisode(number = "214")
    CardSampleItem.EpisodeUnnumbered -> sampleEpisode(number = null)
    else -> null
  }

private val CardSampleItem.unfinishedEpisodes: Int
  get() = when (this) {
    CardSampleItem.PodcastUnfinished -> 12
    CardSampleItem.PodcastUnfinishedCapped -> 300
    else -> 0
  }

private fun sampleBook(
  id: String,
  cover: CardSampleItem?,
  title: String? = "Dungeon Crawler Carl",
  author: String? = "Matt Dinniman",
  ebookFormat: String? = null,
): LibraryItem {
  val base = libraryItem(
    id = id,
    ebookFormat = ebookFormat,
    numTracks = if (ebookFormat != null) 0 else 10,
  )
  val media = base.media as Media.Book
  return base.copy(
    media = media.copy(
      coverImageUrl = cover?.coverUrl.orEmpty(),
      metadata = media.metadata.copy(title = title, authorName = author),
    ),
  )
}

private fun samplePodcast(id: String, cover: CardSampleItem) = libraryItem(id = id).copy(
  mediaType = MediaType.Podcast,
  media = Media.Podcast(
    id = "${id}_media",
    metadata = Media.Metadata.Podcast(
      title = "The Campfire Hour",
      author = "Ada Ember",
      description = null,
      releaseDate = null,
      genres = emptyList(),
      feedUrl = null,
      imageUrl = null,
      itunesPageUrl = null,
      itunesId = null,
      itunesArtistId = null,
      isExplicit = false,
      language = null,
      podcastType = null,
    ),
    coverImageUrl = cover.coverUrl,
    coverPath = null,
    tags = emptyList(),
    sizeInBytes = 0L,
  ),
)

private fun sampleEpisode(number: String?) = PodcastEpisode(
  id = "sample_episode_${number ?: "none"}",
  libraryItemId = "sample_podcast",
  podcastId = "sample_podcast_media",
  episode = number,
  title = "Stories Told Around the Fire",
  addedAtMillis = 0L,
  updatedAtMillis = 0L,
  durationInMillis = 58L * 60L * 1000L,
  sizeInBytes = 0L,
)

private val CardSampleItem.coverUrl: String
  get() = "https://preview.campfire.app/cover/$name"

/**
 * Stand-in cover art, picked to put the card's badges over both light and dark corners: a
 * vertical gradient per item, keyed by the cover URL. A book without a cover falls back to the
 * card's placeholder, as it does in the app.
 */
@OptIn(ExperimentalCoilApi::class)
private val SampleCoverHandler = AsyncImagePreviewHandler { _, request ->
  // The sized request appends query parameters to the cover URL
  val key = request.data.toString().substringAfter("/cover/", "").substringBefore('?')
  val colors = SampleCoverColors.entries.firstOrNull { (item, _) -> item.name == key }?.value
  if (colors == null) {
    AsyncImagePainter.State.Error(
      painter = null,
      result = ErrorResult(image = null, request = request, throwable = IllegalStateException("No cover")),
    )
  } else {
    val (top, bottom) = colors
    AsyncImagePainter.State.Success(
      painter = SampleCoverPainter(top, bottom),
      result = SuccessResult(image = ColorImage(top.toArgb()), request = request),
    )
  }
}

private val SampleCoverColors = mapOf(
  // Dark all over
  CardSampleItem.Audiobook to (Color(0xFF8E2C1F) to Color(0xFF1E0F0C)),
  // Light all over
  CardSampleItem.LongMetadata to (Color(0xFFF4E9D0) to Color(0xFFC9A66B)),
  // Near-white top, where the format badge sits
  CardSampleItem.Ebook to (Color(0xFFFFFFFF) to Color(0xFF7FA7C9)),
  // Dark top, bright bottom under the progress bar
  CardSampleItem.Podcast to (Color(0xFF3B1C5A) to Color(0xFFF29E4C)),
  // Bright yellow
  CardSampleItem.PodcastUnfinished to (Color(0xFFFFE082) to Color(0xFFFFB300)),
  // Deep teal
  CardSampleItem.PodcastUnfinishedCapped to (Color(0xFF0D4D4D) to Color(0xFF062626)),
)

private class SampleCoverPainter(
  private val top: Color,
  private val bottom: Color,
) : Painter() {
  override val intrinsicSize: Size = Size(SampleCoverPx, SampleCoverPx)

  override fun DrawScope.onDraw() {
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
  }
}

private const val SampleCoverPx = 400f

// endregion
