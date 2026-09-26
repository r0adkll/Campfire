// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the fade the marquee sits under: a white bar drawn over black should run from black at
 * each end to fully white across the middle. PNGs land in `build/renders` for eyeballing, alongside
 * a scrolling title as it looks in a player.
 */
@OptIn(ExperimentalComposeUiApi::class)
class FadingEdgesRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `content fades out towards both edges`() {
    val bitmap = render("fading-edges", width = 200, height = 20) {
      Box(
        Modifier
          .fillMaxSize()
          .fadingEdges(16.dp)
          .background(Color.White),
      )
    }

    val row = 10
    assertThat(bitmap.red(0, row)).isLessThan(16)
    assertThat(bitmap.red(8, row)).isGreaterThan(64)
    assertThat(bitmap.red(8, row)).isLessThan(192)
    assertThat(bitmap.red(100, row)).isEqualTo(255)
    assertThat(bitmap.red(191, row)).isGreaterThan(64)
    assertThat(bitmap.red(199, row)).isLessThan(16)
  }

  @Test
  fun `the start edge stays sharp while its fade is off`() {
    val bitmap = render("fading-edges-end-only", width = 200, height = 20) {
      Box(
        Modifier
          .fillMaxSize()
          .fadingEdges(16.dp, startFade = { 0f })
          .background(Color.White),
      )
    }

    val row = 10
    assertThat(bitmap.red(0, row)).isEqualTo(255)
    assertThat(bitmap.red(199, row)).isLessThan(16)
  }

  @Test
  fun `a scrolling title renders under the fade`() {
    // At rest before its first pass, mid-pass, and resting after its last.
    listOf("rest" to 500L, "moving" to 3_000L, "stopped" to 120_000L).forEach { (phase, atMillis) ->
      render("marquee-title-$phase", width = 240, height = 48, untilMillis = atMillis) {
        MarqueeText(
          text = "Chapter One Hundred and Twelve: In Which Everything Goes Wrong",
          color = Color.White,
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 16.dp)
            .width(208.dp),
        )
      }
    }
  }

  private fun render(
    name: String,
    width: Int,
    height: Int,
    untilMillis: Long = 0L,
    content: @Composable BoxScope.() -> Unit,
  ): Bitmap {
    val image = ImageComposeScene(width, height, Density(1f)) {
      Box(Modifier.fillMaxSize().background(Color.Black)) { content() }
    }.use { scene ->
      // Frame by frame, so animations and effects step along the same clock the app gives them.
      var image = scene.render(0L)
      for (millis in FrameMillis..untilMillis step FrameMillis) image = scene.render(millis * 1_000_000L)
      image
    }
    image.encodeToData(EncodedImageFormat.PNG)?.bytes?.let { File(renders, "$name.png").writeBytes(it) }
    return Bitmap.makeFromImage(image)
  }

  private fun Bitmap.red(x: Int, y: Int): Int = (getColor(x, y) shr 16) and 0xFF

  private companion object {
    const val FrameMillis = 16L
  }
}
