package app.campfire.data.mapping

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.Playlist
import app.campfire.network.models.PlaylistExpanded as NetworkPlaylist
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun NetworkPlaylist.asDomainModel(urlHydrator: UrlHydrator): Playlist {
  return Playlist(
    id = id,
    name = name,
    description = description,
    lastUpdatedAt = Instant.fromEpochMilliseconds(lastUpdated).toLocalDateTime(TimeZone.currentSystemDefault()),
    createdAt = Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(TimeZone.currentSystemDefault()),
    items = items.mapIndexed { index, item ->
      Playlist.Item.Expanded(
        index = index,
        libraryItemId = item.libraryItemId,
        episodeId = item.episodeId,
        libraryItem = item.libraryItem.asDomainModel(urlHydrator),
      )
    }
  )
}
