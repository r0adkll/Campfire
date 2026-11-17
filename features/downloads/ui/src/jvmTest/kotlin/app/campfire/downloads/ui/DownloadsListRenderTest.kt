// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import app.campfire.downloads.ui.composables.DownloadRow
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders Downloads rows off-screen, finished and not, and writes PNGs to `build/renders` for
 * eyeballing; the assertions only guard that each list paints.
 */
class DownloadsListRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  private val audiobook = libraryItem(id = "audiobook").let { item ->
    item.copy(media = (item.media as Media.Book).copy(tracks = listOf(m4bTrack)))
  }
  private val ebook = libraryItem(id = "ebook", numTracks = 0, ebookFormat = "epub")
  private val episode = PodcastEpisode(
    id = "episode",
    libraryItemId = "podcast",
    podcastId = "podcast_media",
    title = "What we lose when we lose attention",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 3_840_000L,
    sizeInBytes = 58_000_000L,
  )

  private val downloaded = listOf(
    DownloadEntry.Book(audiobook, download("audiobook")),
    DownloadEntry.Ebook(ebook, download("ebook", contentLength = 18_637L)),
    DownloadEntry.Episode(libraryItem(id = "podcast"), episode, download("podcast")),
  )

  private val unfinished = listOf(
    DownloadEntry.Book(
      audiobook,
      download("audiobook").copy(
        state = Download.State.Downloading,
        progress = 0.45f,
        bytes = 210_000_000L,
        totalBytes = 468_000_000L,
      ),
    ),
    DownloadEntry.Book(audiobook, download("audiobook").copy(state = Download.State.Queued)),
    DownloadEntry.Book(audiobook, download("audiobook").copy(state = Download.State.Stopped, progress = 0.2f)),
    DownloadEntry.Book(audiobook, download("audiobook").copy(state = Download.State.Failed)),
    DownloadEntry.Unavailable(download("gone"), sizeInBytes = 312_000_000L),
  )

  @Test
  fun `finished downloads show as their items do in other lists`() {
    render("downloads-downloaded", height = 330) { Rows(downloaded) }
  }

  @Test
  fun `finished downloads in the dark`() {
    render("downloads-downloaded-dark", height = 330, dark = true) { Rows(downloaded) }
  }

  @Test
  fun `downloads that aren't finished say where they stand`() {
    render("downloads-unfinished", height = 530) { Rows(unfinished) }
  }

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Test
  fun `the whole list, in its sections`() {
    val show = libraryItem(id = "podcast")
    val episodes = (1..5).map { number ->
      DownloadEntry.Episode(
        show,
        episode.copy(id = "episode_$number", title = "Episode $number of the show"),
        download("podcast").copy(content = DownloadContent.Episode("podcast", "episode_$number")),
      )
    }
    // Its own item, as a row's key is its item's
    val going = DownloadEntry.Book(libraryItem(id = "going"), unfinished.first().download)
    val ebookEntry = DownloadEntry.Ebook(
      ebook,
      download("ebook", contentLength = 18_637L).copy(content = DownloadContent.Ebook("ebook")),
    )
    val entries = listOf(going, downloaded.first(), ebookEntry) + episodes + unfinished.takeLast(1)
    val content = DownloadsContent(entries, entries.summary(), filter = null, sections = entries.asSections())
    render("downloads-sections", height = 1150) { List(content) }
    render("downloads-sections-wide", width = 900, height = 760) { List(content) }
  }

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Composable
  private fun List(content: DownloadsContent) {
    PreviewSharedElementTransitionLayout {
      DownloadList(
        content = content,
        onFilterChange = {},
        onShowClick = {},
        onDeleteShow = { _, _ -> },
        onClick = {},
        onPlayClick = {},
        onRetryClick = {},
        onDelete = {},
      )
    }
  }

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Composable
  private fun Rows(entries: List<DownloadEntry>) {
    PreviewSharedElementTransitionLayout {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp),
      ) {
        entries.forEach { entry ->
          DownloadRow(
            entry = entry,
            onClick = {},
            onPlayClick = {},
            onRetryClick = {},
            onDelete = {},
          )
        }
      }
    }
  }

  private fun render(
    name: String,
    height: Int,
    width: Int = WIDTH,
    dark: Boolean = false,
    content: @Composable () -> Unit,
  ) {
    val scene = ImageComposeScene(width = width * 2, height = height * 2, density = Density(2f)) {
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
