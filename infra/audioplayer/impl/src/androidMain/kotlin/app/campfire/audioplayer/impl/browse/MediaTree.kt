package app.campfire.audioplayer.impl.browse

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import app.campfire.audioplayer.impl.asPlatformMediaItem
import app.campfire.audioplayer.impl.mediaitem.MediaItemBuilder
import app.campfire.author.api.AuthorRepository
import app.campfire.collections.api.CollectionsRepository
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.Author
import app.campfire.core.model.Collection
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Series
import app.campfire.core.model.SeriesId
import app.campfire.core.model.loggableId
import app.campfire.home.api.HomeFeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.infra.audioplayer.impl.R
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.search.api.SearchRepository
import app.campfire.search.api.SearchResult
import app.campfire.series.api.SeriesRepository
import kotlin.collections.firstOrNull
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import me.tatarka.inject.annotations.Inject

@Inject
@SingleIn(UserScope::class)
class MediaTree(
  private val application: Application,
  private val homeRepository: HomeRepository,
  private val libraryItemRepository: LibraryItemRepository,
  private val seriesRepository: SeriesRepository,
  private val collectionsRepository: CollectionsRepository,
  private val authorRepository: AuthorRepository,
  private val searchRepository: SearchRepository,
) {

  val root
    get() = MediaItem.Builder()
      .setMediaId(ROOT_ID)
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setIsBrowsable(true)
          .setIsPlayable(false)
          .build(),
      )
      .build()

  suspend fun getChildren(parentId: String): List<MediaItem> {
    return when (parentId) {
      ROOT_ID -> TopLevelMediaItem.All.map {
        it.asBrowsableMediaItem(
          context = application,
          isGridLayout = it.isGridLayout,
        )
      }

      HOME_ID -> loadHome()
      SERIES_ID -> loadSeries()
      COLLECTIONS_ID -> loadCollections()
      AUTHORS_ID -> loadAuthors()

      else -> when {
        parentId.startsWith(SERIES_PREFIX) -> getSeriesItems(parentId.removePrefix(SERIES_PREFIX))
        parentId.startsWith(COLLECTIONS_PREFIX) -> getCollectionItems(parentId.removePrefix(COLLECTIONS_PREFIX))
        parentId.startsWith(AUTHORS_PREFIX) -> getAuthorItems(parentId.removePrefix(AUTHORS_PREFIX))

        else -> emptyList()
      }
    }
  }

  suspend fun resolveMediaItem(libraryItemId: LibraryItemId): List<MediaItem> {
    return try {
      val item = libraryItemRepository.getLibraryItem(libraryItemId)
      MediaItemBuilder.build(item).map { it.asPlatformMediaItem() }
    } catch (e: Throwable) {
      bark(LogPriority.ERROR, throwable = e) {
        "Unable to find item for ${libraryItemId.loggableId}"
      }
      emptyList()
    }
  }

  private suspend fun loadHome(): List<MediaItem> {
    val homeFeed = homeRepository.observeHomeFeed()
      .filterNot { it is HomeFeedResponse.Loading }
      .firstOrNull()
      ?: return emptyList()
    return homeFeed.dataOrNull
      ?.flatMap {
        it.entities.mapNotNull { item ->
          when (item) {
            is LibraryItem -> item.asBrowsableMediaItem(titleHint = it.label)
            is Series -> item.asBrowsableMediaItem(titleHint = it.label)
            is Author -> item.asBrowsableMediaItem(titleHint = it.label)
            else -> null
          }
        }
      } ?: emptyList()
  }

  private suspend fun loadSeries(): List<MediaItem> {
    val series = seriesRepository.observeAllSeries()
      .firstOrNull { it.isNotEmpty() }
      ?: return emptyList()

    return series.map { item ->
      item.asBrowsableMediaItem()
    }
  }

  private suspend fun getSeriesItems(seriesId: SeriesId): List<MediaItem> {
    val items = seriesRepository.observeSeriesLibraryItems(seriesId)
      .firstOrNull()
      ?: return emptyList()

    return items.map { item ->
      item.asBrowsableMediaItem()
    }
  }

  private suspend fun loadCollections(): List<MediaItem> {
    val collections = collectionsRepository.observeAllCollections().firstOrNull() ?: return emptyList()

    return collections.map { collection ->
      collection.asBrowsableMediaItem()
    }
  }

  private suspend fun getCollectionItems(collectionId: CollectionId): List<MediaItem> {
    val items = collectionsRepository.observeCollectionItems(collectionId)
      .firstOrNull()
      ?: return emptyList()

    return items.map { item ->
      item.asBrowsableMediaItem()
    }
  }

  private suspend fun loadAuthors(): List<MediaItem> {
    val authors = authorRepository.observeAuthors().firstOrNull { it.isNotEmpty() } ?: return emptyList()

    return authors.map { author ->
      author.asBrowsableMediaItem()
    }
  }

  private suspend fun getAuthorItems(authorId: String): List<MediaItem> {
    val items = authorRepository.observeAuthor(authorId)
      .firstOrNull()
      ?.libraryItems
      ?: return emptyList()

    return items.map { item ->
      item.asBrowsableMediaItem()
    }
  }

  suspend fun getItem(mediaId: String): MediaItem? {
    // Don't attempt to fetch our folder media items.
    if (
      mediaId != ROOT_ID ||
      mediaId.startsWith(SERIES_PREFIX) ||
      mediaId.startsWith(COLLECTIONS_PREFIX) ||
      mediaId.startsWith(AUTHORS_PREFIX) ||
      TopLevelMediaItem.All.any { it.mediaId == mediaId }
    ) {
      return null
    }

    try {
      return libraryItemRepository.getLibraryItem(mediaId).asBrowsableMediaItem()
    } catch (e: Throwable) {
      bark(LogPriority.ERROR, throwable = e) {
        "Error getting item for ${mediaId.loggableId}"
      }
    }
    return null
  }

  suspend fun search(query: String): List<MediaItem> {
    val result = searchRepository.searchCurrentLibrary(query)
      .firstOrNull { it !is SearchResult.Loading }
    bark { "Search result: $result" }
    return if (result is SearchResult.Success) {
      result.books.map { it.asBrowsableMediaItem(titleHint = "Books") } +
        result.series.map { it.asBrowsableMediaItem(titleHint = "Series") } +
        result.authors.map { it.asBrowsableMediaItem(titleHint = "Authors") }
    } else {
      emptyList()
    }
  }

  @OptIn(UnstableApi::class)
  private fun LibraryItem.asBrowsableMediaItem(
    titleHint: String? = null,
  ) = MediaItem.Builder()
    .setMediaId(id)
    .setMediaMetadata(
      MediaMetadata.Builder()
        .setTitle(media.metadata.title)
        .setArtist(media.metadata.authorName)
        .setArtworkUri(media.coverImageUrl.toUri())
        .setDescription(media.metadata.description)
        .setDurationMs(media.durationInMillis)
        .setGenre(media.metadata.genres.firstOrNull())
        .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
        .setTotalTrackCount(media.numChapters)
        .fluentIf(titleHint != null) {
          setExtras(
            bundleOf(
              MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE to titleHint,
            ),
          )
        }
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build(),
    )
    .build()

  @OptIn(UnstableApi::class)
  private fun Series.asBrowsableMediaItem(
    titleHint: String? = null,
  ) = MediaItem.Builder()
    .setMediaId("$SERIES_PREFIX$id")
    .setMediaMetadata(
      MediaMetadata.Builder()
        .setTitle(name)
        .setArtworkUri(
          books
            ?.sortedBy { it.media.metadata.seriesSequence?.id }
            ?.firstOrNull()
            ?.media?.coverImageUrl?.toUri(),
        )
        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .fluentIf(titleHint != null) {
          setExtras(
            bundleOf(
              MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE to titleHint,
            ),
          )
        }
        .build(),
    )
    .build()

  private fun Collection.asBrowsableMediaItem() = MediaItem.Builder()
    .setMediaId("$COLLECTIONS_PREFIX$id")
    .setMediaMetadata(
      MediaMetadata.Builder()
        .setTitle(name)
        .setDescription(description)
        .setArtworkUri(books.firstOrNull()?.media?.coverImageUrl?.toUri())
        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build(),
    )
    .build()

  @OptIn(UnstableApi::class)
  private fun Author.asBrowsableMediaItem(
    titleHint: String? = null,
  ) = MediaItem.Builder()
    .setMediaId("$AUTHORS_PREFIX$id")
    .setMediaMetadata(
      MediaMetadata.Builder()
        .setTitle(name)
        .setDescription(description)
        .setArtworkUri(imagePath?.toUri())
        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .fluentIf(titleHint != null) {
          setExtras(
            bundleOf(
              MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE to titleHint,
            ),
          )
        }
        .build(),
    )
    .build()
}

enum class TopLevelMediaItem(
  val mediaId: String,
  @get:StringRes val title: Int,
  val isGridLayout: Boolean = false,
) {
  Home(HOME_ID, R.string.folder_home_title, true),
  Series(SERIES_ID, R.string.folder_series_title),
  Collections(COLLECTIONS_ID, R.string.folder_collections_title),
  Authors(AUTHORS_ID, R.string.folder_authors_title),
  ;

  @OptIn(UnstableApi::class)
  fun asBrowsableMediaItem(
    context: Context,
    isGridLayout: Boolean = false,
  ): MediaItem = MediaItem.Builder()
    .setMediaId(mediaId)
    .setMediaMetadata(
      MediaMetadata.Builder()
        .setTitle(context.getString(title))
        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .fluentIf(isGridLayout) {
          setExtras(
            bundleOf(
              MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE to
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
            ),
          )
        }
        .build(),
    )
    .build()

  companion object {
    val All = entries.toList()
  }
}

private const val ROOT_ID = "root-campfire"

private const val HOME_ID = "home-campfire"
private const val SERIES_ID = "series-campfire"
private const val SERIES_PREFIX = "series_"
private const val COLLECTIONS_ID = "collections-campfire"
private const val COLLECTIONS_PREFIX = "collections_"
private const val AUTHORS_ID = "authors-campfire"
private const val AUTHORS_PREFIX = "authors_"
