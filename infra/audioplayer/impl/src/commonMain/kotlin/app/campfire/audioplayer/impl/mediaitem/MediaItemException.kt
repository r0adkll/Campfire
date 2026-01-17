package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.model.LibraryItem

internal class MediaItemException(
  message: String,
  item: LibraryItem,
) : Exception(
  "$message: Item[numChapters=${item.media.chapters.size}, numTracks=${item.media.tracks.size}]",
)
