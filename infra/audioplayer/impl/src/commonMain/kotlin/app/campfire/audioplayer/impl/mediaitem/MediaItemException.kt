// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.model.LibraryItem

internal class MediaItemException(
  message: String,
  item: LibraryItem,
) : Exception(
  "$message: Item[numChapters=${item.media.chapters.size}, numTracks=${item.media.tracks.size}]",
)
