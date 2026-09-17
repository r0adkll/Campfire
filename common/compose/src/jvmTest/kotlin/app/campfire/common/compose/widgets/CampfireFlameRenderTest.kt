// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface

/**
 * Renders [CampfireFlame] off-screen over time and across a pull, writing strips (and the burning
 * frames, for stitching into a GIF) to `build/renders/flame` for eyeballing. The assertions guard
 * that the fire moves while burning, grows with the pull, and paints nothing at rest.
 */
@OptIn(ExperimentalComposeUiApi::class)
class CampfireFlameRenderTest {

  private val renders = File("build/renders/flame").apply { mkdirs() }

  @Test
  fun `burning fire changes shape from frame to frame`() {
    val frames = render(progress = 1f, isBurning = true, frameCount = BurningFrames, container = true)
    frames.forEachIndexed { index, frame -> save("burning-${index.toString().padStart(3, '0')}", frame) }
    save("burning-strip", strip(frames.filterIndexed { index, _ -> index % 5 == 0 }))

    val distinct = frames.map { it.pixels().contentHashCode() }.toSet()
    assertTrue(distinct.size > BurningFrames * 3 / 4, "expected a moving fire, got ${distinct.size} distinct frames")
  }

  @Test
  fun `pulling grows the fire from its base`() {
    val progresses = listOf(0.1f, 0.3f, 0.5f, 0.75f, 1f)
    val frames = progresses.map { progress ->
      render(progress = progress, isBurning = false, frameCount = 1, container = false).last()
    }
    save("pull-strip", strip(frames))

    val painted = frames.map { it.paintedPixels() }
    assertEquals(painted.sorted(), painted, "expected the fire to grow with the pull, painted $painted")
  }

  @Test
  fun `fire at rest paints nothing`() {
    val frame = render(progress = 0f, isBurning = false, frameCount = 1, container = false).last()
    assertEquals(0, frame.paintedPixels())
  }

  private fun render(progress: Float, isBurning: Boolean, frameCount: Int, container: Boolean): List<Image> =
    ImageComposeScene(
      width = SizePx,
      height = SizePx,
      density = Density(SceneDensity),
      coroutineContext = Dispatchers.Unconfined,
    ) {
      Indicator(progress, isBurning, container)
    }.use { scene ->
      scene.render(nanoTime = 0L)
      (1..frameCount).map { frame -> scene.render(nanoTime = frame * FrameNanos) }
    }

  @Composable
  private fun Indicator(progress: Float, isBurning: Boolean, container: Boolean) {
    CampfireTheme(useDarkColors = true) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .then(
            if (container) {
              Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            } else {
              Modifier
            },
          ),
      ) {
        CampfireFlame(
          progress = { progress },
          isBurning = isBurning,
          modifier = Modifier.fillMaxSize().padding(10.dp),
        )
      }
    }
  }

  private fun strip(frames: List<Image>): Image {
    val surface = Surface.makeRasterN32Premul(SizePx * frames.size, SizePx)
    frames.forEachIndexed { index, frame -> surface.canvas.drawImage(frame, (index * SizePx).toFloat(), 0f) }
    return surface.makeImageSnapshot()
  }

  private fun save(name: String, image: Image) {
    val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
    File(renders, "$name.png").writeBytes(bytes)
  }

  private fun Image.pixels(): ByteArray {
    val bitmap = Bitmap().also {
      it.allocPixels(imageInfo)
      assertTrue(readPixels(it))
    }
    return bitmap.readPixels()!!
  }

  private fun Image.paintedPixels(): Int {
    val bitmap = Bitmap().also {
      it.allocPixels(imageInfo)
      assertTrue(readPixels(it))
    }
    var count = 0
    for (x in 0 until width) {
      for (y in 0 until height) {
        if ((bitmap.getColor(x, y) ushr 24) != 0) count++
      }
    }
    return count
  }

  private companion object {
    const val SceneDensity = 3f
    const val SizePx = (72 * SceneDensity).toInt()
    const val BurningFrames = 60
    const val FrameNanos = 1_000_000_000L / 30
  }
}
