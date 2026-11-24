// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.use
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the [LibraryItemCard] sample grids from `LibraryItemCardPreviews.kt` — every state of
 * every item in each context, and every context side by side — in light and dark, to PNGs under
 * `build/renders/library-item-card` for reviewing the card's visual consistency.
 */
@OptIn(ExperimentalComposeUiApi::class)
class LibraryItemCardSamplesRenderTest {

  private val renders = File("build/renders/library-item-card").apply { mkdirs() }

  @Test
  fun `every state of every item on a home shelf`() = renderBothThemes("states-home-shelf") {
    LibraryItemCardStateGrid(CardSampleContext.HomeShelf)
  }

  @Test
  fun `every state of every item in the library grid`() = renderBothThemes("states-library-grid") {
    LibraryItemCardStateGrid(CardSampleContext.LibraryGrid)
  }

  @Test
  fun `every state of every item in the dense library grid`() = renderBothThemes("states-library-grid-dense") {
    LibraryItemCardStateGrid(CardSampleContext.LibraryGridDense)
  }

  @Test
  fun `every item in every context`() = renderBothThemes("contexts") {
    LibraryItemCardContextGrid()
  }

  @Test
  fun `every item in every context while in progress and downloaded`() =
    renderBothThemes("contexts-in-progress-downloaded") {
      LibraryItemCardContextGrid(state = CardSampleState.InProgressDownloaded)
    }

  @Test
  fun `every configuration composes on its own`() {
    AllCardSamples.forEach { sample ->
      val size = measure(darkTheme = false) { LibraryItemCardSample(sample) }
      assertTrue(size.width > 0 && size.height > 0, "${sample.name} drew nothing")
    }
  }

  private fun renderBothThemes(name: String, content: @Composable () -> Unit) {
    render("$name-light", darkTheme = false, content)
    render("$name-dark", darkTheme = true, content)
  }

  private fun render(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
    val size = measure(darkTheme, content)
    scene(size, darkTheme, content).use { scene ->
      // The first frame starts the cover requests, the second draws what the handler returned.
      scene.render()
      val image = scene.render(nanoTime = FrameNanos)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      val file = File(renders, "$name.png")
      file.writeBytes(bytes)
      println("rendered ${file.absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  /** The pixel size [content] wraps to, laid out in a scene too big to constrain it. */
  private fun measure(darkTheme: Boolean, content: @Composable () -> Unit): IntSize {
    var size = IntSize.Zero
    scene(IntSize(MaxScenePx, MaxScenePx), darkTheme) {
      Box(Modifier.wrapContentSize().onSizeChanged { size = it }) { content() }
    }.use { it.render() }
    return size
  }

  private fun scene(size: IntSize, darkTheme: Boolean, content: @Composable () -> Unit) = ImageComposeScene(
    width = size.width,
    height = size.height,
    density = Density(SceneDensity),
    coroutineContext = Dispatchers.Unconfined,
  ) {
    // The cover stand-ins only resolve in inspection mode, as in an IDE preview
    CompositionLocalProvider(LocalInspectionMode provides true) {
      CardSampleHost(darkTheme = darkTheme) {
        content()
      }
    }
  }

  private companion object {
    const val SceneDensity = 2f
    const val MaxScenePx = 8_000
    const val FrameNanos = 1_000_000_000L
  }
}
