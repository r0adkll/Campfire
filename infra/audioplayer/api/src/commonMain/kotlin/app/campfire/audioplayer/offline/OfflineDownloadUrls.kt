// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.offline

private val LibraryFilePath = Regex("/api/items/[^/?#]+/file/[^/?#]+$")

/**
 * The URL to download a track from, given its streaming [contentUrl]. ABS serves the same file
 * from `/api/items/:id/file/:ino/download`, which — unlike the streaming route — refuses users
 * without download permission, and is the route the official apps download through. Any other
 * URL shape is downloaded as-is.
 */
fun offlineDownloadUrl(contentUrl: String): String {
  val path = contentUrl.substringBefore('?').substringBefore('#')
  return if (LibraryFilePath.containsMatchIn(path)) "$path/download" else contentUrl
}
