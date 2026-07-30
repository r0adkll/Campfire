// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.LibraryId
import app.campfire.core.model.Playlist
import app.campfire.core.model.UserId
import app.campfire.data.Playlists as DbPlaylist
import app.campfire.network.models.PlaylistExpanded as NetworkPlaylist
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun NetworkPlaylist.asDomainModel(urlHydrator: UrlHydrator): Playlist {
  return Playlist(
    id = id,
    name = name,
    description = description,
    lastUpdatedAt = Instant.fromEpochMilliseconds(lastUpdate).toLocalDateTime(TimeZone.currentSystemDefault()),
    createdAt = Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(TimeZone.currentSystemDefault()),
    items = items.mapIndexed { index, item ->
      val libraryItem = item.libraryItem.asDomainModel(urlHydrator)
      // The server places the resolved episode at the playlist-item level — the nested
      // libraryItem.media is the *minified* podcast and never carries episodes here.
      val episode = item.episode?.asDomainModel(urlHydrator)
      Playlist.Item.Expanded(
        index = index,
        libraryItemId = item.libraryItemId,
        episodeId = item.episodeId,
        libraryItem = libraryItem,
        episode = episode,
      )
    },
  )
}

fun NetworkPlaylist.asDbModel(
  userId: UserId,
  libraryId: LibraryId,
  creationId: String? = null,
): DbPlaylist {
  return DbPlaylist(
    id = id,
    creationId = creationId,
    name = name,
    description = description,
    lastUpdated = lastUpdate,
    createdAt = createdAt,
    userId = userId,
    libraryId = libraryId,
  )
}

fun Playlist.asDbModel(
  userId: UserId,
  libraryId: LibraryId,
): DbPlaylist {
  return DbPlaylist(
    id = id,
    creationId = null,
    name = name,
    description = description,
    lastUpdated = lastUpdatedAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
    createdAt = createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
    libraryId = libraryId,
    userId = userId,
  )
}

fun DbPlaylist.asDomainModel(
  items: List<Playlist.Item.Expanded>,
): Playlist {
  return Playlist(
    id = id,
    name = name,
    description = description,
    lastUpdatedAt = Instant.fromEpochMilliseconds(lastUpdated).toLocalDateTime(TimeZone.currentSystemDefault()),
    createdAt = Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(TimeZone.currentSystemDefault()),
    items = items,
  )
}
