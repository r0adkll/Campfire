package app.campfire.playlists.store

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.Playlist
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import org.mobilenativefoundation.store.store5.Fetcher

class PlaylistsFetcherFactory(
  private val api: AudioBookShelfApi,
  private val urlHydrator: UrlHydrator,
) {

  fun create(): Fetcher<PlaylistsStore.Operation, List<Playlist>> {
    return Fetcher.ofResult { operation ->
      require(operation is PlaylistsStore.Operation.All || operation is PlaylistsStore.Operation.Single)
      when (operation) {
        is PlaylistsStore.Operation.All -> {
          api.getPlaylists(operation.libraryId)
            .map { playlists ->
              playlists.map { playlist ->
                playlist.asDomainModel(urlHydrator)
              }
            }
            .asFetcherResult()
        }
        is PlaylistsStore.Operation.Single -> api.getPlaylist(operation.playlistId)
          .map { listOf(it.asDomainModel(urlHydrator)) }
          .asFetcherResult()
        else -> throw IllegalArgumentException("Unknown operation: $operation")
      }
    }
  }
}
