// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.Media
import app.campfire.core.model.preview.libraryItem
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders Downloads rows off-screen, alone and mixed with other kinds, and writes PNGs to
 * `build/renders` for eyeballing; the assertions only guard that each list paints.
 */
class DownloadsListRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  private val audiobook = libraryItem(id = "audiobook").let { item ->
    item.copy(media = (item.media as Media.Book).copy(tracks = listOf(m4bTrack)))
  }
  private val ebook = libraryItem(id = "ebook", numTracks = 0, ebookFormat = "epub")

  private val entries = listOf(
    DownloadEntry.Book(audiobook, download("audiobook")),
    DownloadEntry.Ebook(ebook, download("ebook", contentLength = 18_637L)),
  )

  @Test
  fun `mixed downloads say what each one is`() {
    render("downloads-mixed") { Rows(entries) }
  }

  @Test
  fun `audiobooks alone keep to their size`() {
    render("downloads-audiobooks", dark = true) { Rows(entries.take(1)) }
  }

  @Composable
  private fun Rows(entries: List<DownloadEntry>) {
    Column {
      entries.forEach { entry ->
        ItemDownloadListItem(
          entry = entry,
          showMediaType = entries.hasMixedMediaTypes,
          onClick = {},
          onDeleteClick = {},
        )
      }
    }
  }

  private fun render(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
    val scene = ImageComposeScene(width = WIDTH * 2, height = HEIGHT * 2, density = Density(2f)) {
      CampfireTheme(useDarkColors = dark) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
          content()
        }
      }
    }
    scene.use {
      val image = it.render()
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  private fun download(itemId: String, contentLength: Long = -1L) = Download(
    content = DownloadContent.Audiobook(itemId),
    state = Download.State.Downloaded,
    totalBytes = contentLength,
  )

  private companion object {
    const val WIDTH = 400
    const val HEIGHT = 180

    val m4bTrack = AudioTrack(
      index = 0,
      startOffset = 0f,
      duration = 60f,
      title = "Track",
      contentUrl = "",
      mimeType = "audio/mp4",
      codec = "aac",
      metadata = FileMetadata(
        filename = "book.m4b",
        ext = ".m4b",
        path = "",
        relPath = "",
        size = 0L,
        mtimeMs = 0L,
        ctimeMs = 0L,
        birthtimeMs = 0L,
      ),
      metaTags = null,
    )
  }
}
