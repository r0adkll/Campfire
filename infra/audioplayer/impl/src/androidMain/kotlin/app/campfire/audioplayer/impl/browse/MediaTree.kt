package app.campfire.audioplayer.impl.browse

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import app.campfire.audioplayer.impl.asPlatformMediaItem
import app.campfire.audioplayer.impl.content.coverContentUriForAuthor
import app.campfire.audioplayer.impl.content.coverContentUriForItem
import app.campfire.audioplayer.impl.mediaitem.MediaItemBuilder
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
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
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Series
import app.campfire.core.model.SeriesId
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.loggableId
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.infra.audioplayer.impl.R
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.LibraryRepository
import app.campfire.playlists.api.PlaylistsRepository
import app.campfire.search.api.SearchRepository
import app.campfire.search.api.SearchResult
import app.campfire.series.api.SeriesRepository
import app.campfire.settings.api.AndroidAutoCategory
import app.campfire.settings.api.AndroidAutoSettings
import kotlin.collections.firstOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject

@Inject
@SingleIn(UserScope::class)
class MediaTree(
  private val application: Application,
  private val homeRepository: HomeRepository,
  private val libraryRepository: LibraryRepository,
  private val libraryItemRepository: LibraryItemRepository,
  private val seriesRepository: SeriesRepository,
  private val playlistsRepository: PlaylistsRepository,
  private val collectionsRepository: CollectionsRepository,
  private val authorRepository: AuthorRepository,
  private val searchRepository: SearchRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val androidAutoSettings: AndroidAutoSettings,
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

  /**
   * Return one page of a parent's children. The full child list is loaded (and cached)
   * on the first page request; later pages slice the cached list so paging browsers
   * don't re-run the underlying repository loads per page. Page 0 always reloads, which
   * is how a browser re-querying after [invalidationParentIds] notifications gets fresh
   * data.
   */
  suspend fun getChildren(
    parentId: String,
    page: Int,
    pageSize: Int,
  ): List<MediaItem> {
    val children = childrenCacheMutex.withLock {
      val cached = childrenCache
      if (page > 0 && cached?.first == parentId) {
        cached.second
      } else {
        loadChildren(parentId).also { childrenCache = parentId to it }
      }
    }
    return children.paginate(page, pageSize)
  }

  private val childrenCacheMutex = Mutex()
  private var childrenCache: Pair<String, List<MediaItem>>? = null

  private suspend fun loadChildren(parentId: String): List<MediaItem> {
    return when (parentId) {
      ROOT_ID -> {
        val mediaType = currentMediaType()
        androidAutoSettings.observeCategoryConfigs().value
          .filter { it.visible && it.category.isAvailableFor(mediaType) }
          .mapNotNull { config ->
            config.category.toTopLevelMediaItem()?.asBrowsableMediaItem(
              context = application,
              isGridLayout = config.isGridLayout,
            )
          }
      }

      HOME_ID -> loadHome()
      SHOWS_ID -> loadShows()
      SERIES_ID -> loadSeries()
      AUTHORS_ID -> loadAuthors()
      PLAYLISTS_ID -> loadPlaylists()
      COLLECTIONS_ID -> loadCollections()
      DOWNLOADS_ID -> loadDownloads()

      else -> when {
        parentId.startsWith(SERIES_PREFIX) -> getSeriesItems(parentId.removePrefix(SERIES_PREFIX))
        parentId.startsWith(PLAYLISTS_PREFIX) -> getPlaylistItems(parentId.removePrefix(PLAYLISTS_PREFIX))
        parentId.startsWith(COLLECTIONS_PREFIX) -> getCollectionItems(parentId.removePrefix(COLLECTIONS_PREFIX))
        parentId.startsWith(AUTHORS_PREFIX) -> getAuthorItems(parentId.removePrefix(AUTHORS_PREFIX))
        parentId.startsWith(PODCAST_PREFIX) -> getPodcastEpisodes(parentId.removePrefix(PODCAST_PREFIX))

        else -> emptyList()
      }
    }
  }

  /**
   * The parent ids the media session should invalidate when the browse tree's inputs
   * change (active library switch, Android Auto category settings edits).
   */
  val invalidationParentIds: List<String>
    get() = listOf(ROOT_ID) + TopLevelMediaItem.All.map { it.mediaId }

  /**
   * Resolve the active library's media type, falling back to [MediaType.Book] (today's
   * behavior) when nothing is cached yet — a cold cache store stream may never emit.
   */
  private suspend fun currentMediaType(): MediaType {
    return withTimeoutOrNull(MediaTypeTimeout) {
      libraryRepository.observeCurrentLibrary(refresh = false).firstOrNull()?.mediaType
    } ?: MediaType.Book
  }

  suspend fun resolveMediaItem(mediaId: String): List<MediaItem> {
    return try {
      val browseId = BrowseMediaId.decode(mediaId)
      val item = libraryItemRepository.getLibraryItem(browseId.libraryItemId)
      val episode = item.findEpisode(browseId.episodeId)
      val mediaItems = if (episode != null) {
        MediaItemBuilder.buildPodcastEpisode(item, episode)
      } else {
        MediaItemBuilder.build(item)
      }
      mediaItems.map { it.asPlatformMediaItem(application) }
    } catch (e: Throwable) {
      bark(LogPriority.ERROR, throwable = e) {
        "Unable to find item for ${mediaId.loggableId}"
      }
      emptyList()
    }
  }

  @kotlin.OptIn(ExperimentalCoroutinesApi::class)
  private suspend fun loadHome(): List<MediaItem> {
    val homeFeed = homeRepository.observeHomeFeed()
      .filterNot { it is FeedResponse.Loading }
      .mapLatest {
        val shelves = it.dataOrNull ?: emptyList()
        shelves.associateWith { shelf ->
          homeRepository.observeShelf(shelf.id, shelf.type)
            .firstOrNull()
            ?: emptyList()
        }
      }
      .firstOrNull()
      ?: return emptyList()

    return homeFeed
      .flatMap { (shelf, items) ->
        items.map { item ->
          when (item) {
            is LibraryItem -> item.asBrowsableMediaItem(titleHint = shelf.label)
            is Series -> item.asBrowsableMediaItem(titleHint = shelf.label)
            is Author -> item.asBrowsableMediaItem(titleHint = shelf.label)

            is ShelfEntity.EpisodeShelfEntry -> item.libraryItem.asBrowsableMediaItem(
              titleHint = shelf.label,
              episode = item.recentEpisode,
            )
          }
        }
      }
  }

  private suspend fun loadShows(): List<MediaItem> {
    return libraryRepository.observeCurrentLibraryItems()
      .firstNonEmptyOrEmpty()
      .filter { it.media is Media.Podcast }
      .map { it.asBrowsableMediaItem() }
  }

  private suspend fun loadSeries(): List<MediaItem> {
    val series = seriesRepository.observeAllSeries()
      .firstNonEmptyOrEmpty()

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

  private suspend fun loadAuthors(): List<MediaItem> {
    val authors = authorRepository.observeAuthors().firstNonEmptyOrEmpty()

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

  private suspend fun loadPlaylists(): List<MediaItem> {
    val playlists = playlistsRepository.observeAllPlaylists().firstNonEmptyOrEmpty()

    return playlists.map { playlist ->
      playlist.asBrowsableMediaItem()
    }
  }

  private suspend fun getPlaylistItems(playlistId: PlaylistId): List<MediaItem> {
    val items = playlistsRepository.observePlaylistItems(playlistId)
      .firstOrNull()
      ?: return emptyList()

    return items.map { item ->
      item.libraryItem.asBrowsableMediaItem(episode = item.episode)
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

  private suspend fun getPodcastEpisodes(libraryItemId: LibraryItemId): List<MediaItem> {
    return try {
      val item = libraryItemRepository.getLibraryItem(libraryItemId)
      (item.media as? Media.Podcast)
        ?.episodes
        .orEmpty()
        .sortedByDescending { it.publishedAtMillis ?: it.addedAtMillis }
        .map { episode -> item.asBrowsableMediaItem(episode = episode) }
    } catch (e: Throwable) {
      bark(LogPriority.ERROR, throwable = e) {
        "Unable to load episodes for ${libraryItemId.loggableId}"
      }
      emptyList()
    }
  }

  private suspend fun loadDownloads(): List<MediaItem> {
    val downloadItems = withTimeoutOrNull(LoadTimeout) {
      offlineDownloadManager.observeAll()
        .map { downloads ->
          downloads.associateWith { download ->
            libraryItemRepository.getLibraryItem(download.libraryItemId)
          }
        }
        .firstOrNull { it.isNotEmpty() }
    }.orEmpty()

    return downloadItems.map { (download, libraryItem) ->
      libraryItem.asBrowsableMediaItem(
        download = download,
        episode = download.episodeId?.let { episodeId ->
          (libraryItem.media as? Media.Podcast)?.episodes?.find { it.id == episodeId }
        },
      )
    }
  }

  /**
   * Await the first non-empty emission, bounded so repositories whose flows never emit a
   * non-empty value (e.g. book-only concepts on a podcast library, or an empty playlist
   * list that Store suppresses) can't hang the browse request forever.
   */
  private suspend fun <T> Flow<List<T>>.firstNonEmptyOrEmpty(
    timeout: Duration = LoadTimeout,
  ): List<T> = withTimeoutOrNull(timeout) { firstOrNull { it.isNotEmpty() } }.orEmpty()

  suspend fun getItem(mediaId: String): MediaItem? {
    // Don't attempt to fetch our folder media items.
    if (
      mediaId != ROOT_ID ||
      mediaId.startsWith(SERIES_PREFIX) ||
      mediaId.startsWith(PLAYLISTS_PREFIX) ||
      mediaId.startsWith(COLLECTIONS_PREFIX) ||
      mediaId.startsWith(AUTHORS_PREFIX) ||
      mediaId.startsWith(PODCAST_PREFIX) ||
      TopLevelMediaItem.All.any { it.mediaId == mediaId }
    ) {
      return null
    }

    try {
      val browseId = BrowseMediaId.decode(mediaId)
      val item = libraryItemRepository.getLibraryItem(browseId.libraryItemId)
      return item.asBrowsableMediaItem(episode = item.findEpisode(browseId.episodeId))
    } catch (e: Throwable) {
      bark(LogPriority.ERROR, throwable = e) {
        "Error getting item for ${mediaId.loggableId}"
      }
    }
    return null
  }

  /**
   * Run a fresh search and cache the full result list. Called when the browser submits a
   * query — the returned size feeds `notifySearchResultChanged`, after which the browser
   * pages through the same results via [getSearchResults].
   */
  suspend fun search(query: String): List<MediaItem> {
    val result = searchRepository.searchCurrentLibrary(query)
      .firstOrNull { it !is SearchResult.Loading }
    bark { "Search result: $result" }
    val items = if (result is SearchResult.Success) {
      result.books.map { it.asBrowsableMediaItem(titleHint = "Books") } +
        result.series.map { it.asBrowsableMediaItem(titleHint = "Series") } +
        result.authors.map { it.asBrowsableMediaItem(titleHint = "Authors") }
    } else {
      emptyList()
    }
    searchCacheMutex.withLock { searchCache = query to items }
    return items
  }

  /**
   * Return one page of the results for [query], serving from the cache populated by
   * [search] so paging doesn't re-hit the search repository per page.
   */
  suspend fun getSearchResults(query: String, page: Int, pageSize: Int): List<MediaItem> {
    val cached = searchCacheMutex.withLock {
      searchCache?.takeIf { it.first == query }?.second
    }
    return (cached ?: search(query)).paginate(page, pageSize)
  }

  private val searchCacheMutex = Mutex()
  private var searchCache: Pair<String, List<MediaItem>>? = null

  @OptIn(UnstableApi::class)
  private fun LibraryItem.asBrowsableMediaItem(
    titleHint: String? = null,
    download: OfflineDownload? = null,
    episode: PodcastEpisode? = null,
  ): MediaItem {
    val podcast = media as? Media.Podcast

    // Podcast items without a specific episode (library lists, downloads of a whole show)
    // present as browsable folders whose children are the podcast's episodes. The episode
    // listing re-fetches the full item, so this works even when this item's media is
    // minified and doesn't carry the episode list itself.
    val isPodcastFolder = podcast != null && episode == null

    return MediaItem.Builder()
      .setMediaId(
        when {
          episode != null -> BrowseMediaId(id, episode.id).encoded()
          isPodcastFolder -> "$PODCAST_PREFIX$id"
          else -> id
        },
      )
      .setMediaMetadata(
        MediaMetadata.Builder()
          .apply {
            when {
              episode != null -> {
                setTitle(episode.title)
                setSubtitle(media.metadata.title)
                setAlbumTitle(media.metadata.title)
                setArtist(media.metadata.author ?: media.metadata.title)
                setDescription(episode.description)
                setDurationMs(episode.durationInMillis)
                setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
              }

              podcast != null -> {
                setTitle(media.metadata.title)
                setArtist(media.metadata.author)
                setDescription(media.metadata.description)
                setDurationMs(media.durationInMillis)
                setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST)
                setTotalTrackCount(podcast.numEpisodes)
              }

              else -> {
                setTitle(media.metadata.title)
                setArtist(media.metadata.authorName)
                setDescription(media.metadata.description)
                setDurationMs(media.durationInMillis)
                setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
                setTotalTrackCount(media.numChapters)
              }
            }
          }
          .setArtworkUri(coverContentUriForItem(application, id))
          .setGenre(media.metadata.genres.firstOrNull())
          .setExtras(
            Bundle().apply {
              if (titleHint != null) {
                putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, titleHint)
              }

              if (download != null) {
                putLong(
                  MediaConstants.EXTRAS_KEY_DOWNLOAD_STATUS,
                  when (download.state) {
                    OfflineDownload.State.Stopped,
                    OfflineDownload.State.Failed,
                    OfflineDownload.State.None,
                    -> MediaConstants.EXTRAS_VALUE_STATUS_NOT_DOWNLOADED

                    OfflineDownload.State.Queued,
                    OfflineDownload.State.Downloading,
                    -> MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADING

                    OfflineDownload.State.Completed -> MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED
                  },
                )

                putFloat(
                  MediaConstants.EXTRAS_KEY_DOWNLOAD_PROGRESS,
                  download.progress.percent.coerceIn(0f..1f),
                )
              }
            },
          )
          .setIsBrowsable(isPodcastFolder)
          .setIsPlayable(!isPodcastFolder)
          .build(),
      )
      .build()
  }

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
            ?.id
            ?.let { coverContentUriForItem(application, it) },
        )
        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .fluentIf(titleHint != null) {
          setExtras(
            Bundle().apply {
              putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, titleHint)
            },
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
        .setArtworkUri(books.firstOrNull()?.id?.let { coverContentUriForItem(application, it) })
        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build(),
    )
    .build()

  private fun Playlist.asBrowsableMediaItem() = MediaItem.Builder()
    .setMediaId("$PLAYLISTS_PREFIX$id")
    .setMediaMetadata(
      MediaMetadata.Builder()
        .setTitle(name)
        .setDescription(description)
        .setArtworkUri(
          items.firstOrNull()?.libraryItem?.id?.let { coverContentUriForItem(application, it) },
        )
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
        .setArtworkUri(imagePath?.let { coverContentUriForAuthor(application, id) })
        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .fluentIf(titleHint != null) {
          setExtras(
            Bundle().apply {
              putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, titleHint)
            },
          )
        }
        .build(),
    )
    .build()
}

private fun LibraryItem.findEpisode(episodeId: PodcastEpisodeId?): PodcastEpisode? {
  if (episodeId == null) return null
  return (media as? Media.Podcast)?.episodes?.find { it.id == episodeId }
}

private fun AndroidAutoCategory.toTopLevelMediaItem(): TopLevelMediaItem? = when (this) {
  AndroidAutoCategory.Home -> TopLevelMediaItem.Home
  AndroidAutoCategory.Shows -> TopLevelMediaItem.Shows
  AndroidAutoCategory.Series -> TopLevelMediaItem.Series
  AndroidAutoCategory.Authors -> TopLevelMediaItem.Authors
  AndroidAutoCategory.Playlists -> TopLevelMediaItem.Playlists
  AndroidAutoCategory.Collections -> TopLevelMediaItem.Collections
  AndroidAutoCategory.Downloads -> TopLevelMediaItem.Downloads
}

enum class TopLevelMediaItem(
  val mediaId: String,
  @get:StringRes val title: Int,
  val isGridLayout: Boolean = false,
  val folderMediaType: Int = MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS,
) {
  Home(HOME_ID, R.string.folder_home_title, true),
  Shows(SHOWS_ID, R.string.folder_shows_title, true, MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS),
  Series(SERIES_ID, R.string.folder_series_title),
  Authors(AUTHORS_ID, R.string.folder_authors_title),
  Playlists(PLAYLISTS_ID, R.string.folder_playlists_title),
  Collections(COLLECTIONS_ID, R.string.folder_collections_title),
  Downloads(DOWNLOADS_ID, R.string.folder_downloads_title),
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
        .setMediaType(folderMediaType)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .fluentIf(isGridLayout) {
          setExtras(
            Bundle().apply {
              putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
              )
            },
          )
        }
        .build(),
    )
    .build()

  companion object {
    val All = entries.toList()
  }
}

private val MediaTypeTimeout = 3.seconds
private val LoadTimeout = 5.seconds

private const val ROOT_ID = "root-campfire"

private const val HOME_ID = "home-campfire"
private const val SHOWS_ID = "shows-campfire"
private const val SERIES_ID = "series-campfire"
private const val SERIES_PREFIX = "series_"
private const val AUTHORS_ID = "authors-campfire"
private const val AUTHORS_PREFIX = "authors_"
private const val PLAYLISTS_ID = "playlists-campfire"
private const val PLAYLISTS_PREFIX = "playlists_"
private const val COLLECTIONS_ID = "collections-campfire"
private const val COLLECTIONS_PREFIX = "collections_"
private const val DOWNLOADS_ID = "downloads-campfire"
private const val PODCAST_PREFIX = "podcast_"
