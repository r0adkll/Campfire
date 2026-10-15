// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui.web

import androidx.compose.runtime.Immutable
import app.campfire.reader.api.Ebook
import campfire.features.reader.ui.generated.resources.Res

/**
 * What the reader page can load, all from the app itself: the page and foliate-js under
 * [READER_PREFIX] (bundled resources), and the downloaded book at [bookPath]. The web view
 * never loads anything from the network, so it needs no credentials.
 */
@Immutable
class ReaderContent(private val ebook: Ebook) {

  val format: String get() = ebook.format

  val bookPath: String get() = "$BOOK_PREFIX$BOOK_NAME.${ebook.format}"

  /** Resolve an absolute request [path] (e.g. `/reader/index.html`), or null when there's nothing there. */
  suspend fun resolve(path: String): ReaderResource? {
    if (path.split('/').any { it == ".." }) return null
    return when {
      path.startsWith(READER_PREFIX) -> runCatching {
        Res.readBytes("files/reader/${path.removePrefix(READER_PREFIX)}")
      }.getOrNull()?.let { ReaderResource.Bytes(it, mimeTypeOf(path)) }

      path == bookPath -> ReaderResource.File(ebook.filePath, mimeTypeOf(path))
      else -> null
    }
  }

  companion object {
    const val READER_PREFIX = "/reader/"
    const val BOOK_PREFIX = "/book/"
    const val ENTRY_PATH = "${READER_PREFIX}index.html"
    private const val BOOK_NAME = "book"
  }
}

sealed interface ReaderResource {
  val mimeType: String

  class Bytes(val bytes: ByteArray, override val mimeType: String) : ReaderResource

  class File(val path: String, override val mimeType: String) : ReaderResource
}

private fun mimeTypeOf(path: String): String = when (path.substringAfterLast('.').lowercase()) {
  "html" -> "text/html"
  "js" -> "text/javascript"
  "css" -> "text/css"
  "woff2" -> "font/woff2"
  "epub" -> "application/epub+zip"
  "cbz" -> "application/vnd.comicbook+zip"
  "fb2" -> "application/x-fictionbook+xml"
  "mobi", "azw3" -> "application/x-mobipocket-ebook"
  else -> "application/octet-stream"
}
