// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.image.CoverUrls
import coil3.ColorImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.request.ImageResult
import java.io.File
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * Renders [CoverImage] mid-request — the frame an item detail shows while the shared-element
 * transition is still running — with the network stalled so the larger rendition never arrives.
 *
 * With the rendition a grid card already loaded sitting in the memory cache, the cover has to be on
 * screen for that whole frame; with a cold cache it falls back to the loading treatment. PNGs land in
 * `build/renders` for eyeballing.
 */
@OptIn(ExperimentalComposeUiApi::class)
class CoverImagePlaceholderRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @AfterTest
  fun tearDown() {
    SingletonImageLoader.reset()
  }

  @Test
  fun `the rendition a card already loaded carries the cover while a larger one is fetched`() {
    install(cachedRenditionWidth = 400)
    val center = render("cover-warm").centerColor()
    assertTrue(center.isCachedCover(), "expected the cached rendition, was ${center.toHexString()}")
  }

  @Test
  fun `a cold cache still falls back to the loading cover`() {
    install(cachedRenditionWidth = null)
    val center = render("cover-cold").centerColor()
    assertTrue(!center.isCachedCover(), "expected the loading cover, was ${center.toHexString()}")
  }

  /**
   * An [ImageLoader] whose requests never resolve, so the painter stays in its loading state and the
   * render captures the placeholder rather than the finished image.
   */
  private fun install(cachedRenditionWidth: Int?) {
    val memoryCache = MemoryCache.Builder().maxSizeBytes(MaxCacheBytes).build()
    if (cachedRenditionWidth != null) {
      memoryCache[MemoryCache.Key(CoverUrls.sized(Cover, cachedRenditionWidth))] =
        MemoryCache.Value(ColorImage(CachedCoverArgb, width = 400, height = 400))
    }
    SingletonImageLoader.reset()
    SingletonImageLoader.setUnsafe(
      ImageLoader.Builder(PlatformContext.INSTANCE)
        .memoryCache { memoryCache }
        .components { add(StalledInterceptor) }
        .build(),
    )
  }

  private fun render(name: String): Image = scene().use { scene ->
    // Two frames: the first starts the request (and picks up the placeholder), the second draws it.
    scene.render()
    scene.render(nanoTime = FrameNanos).also { save(name, it) }
  }

  private fun scene() = ImageComposeScene(
    width = (Width * SceneDensity).toInt(),
    height = (Height * SceneDensity).toInt(),
    density = Density(SceneDensity),
    coroutineContext = Dispatchers.Unconfined,
  ) {
    Content()
  }

  @Composable
  private fun Content() {
    CampfireTheme(useDarkColors = false) {
      Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center,
      ) {
        CoverImage(
          imageUrl = Cover,
          contentDescription = null,
          size = 300.dp,
        )
      }
    }
  }

  private fun save(name: String, image: Image) {
    val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
    val file = File(renders, "$name.png")
    file.writeBytes(bytes)
    println("rendered ${file.absolutePath}")
    assertTrue(bytes.size > 1_000)
  }

  private fun Image.centerColor(): Int {
    val bitmap = Bitmap().also {
      it.allocPixels(imageInfo)
      assertTrue(readPixels(it))
    }
    return bitmap.getColor(width / 2, height / 2)
  }

  private fun Int.isCachedCover(): Boolean {
    val r = (this shr 16) and 0xff
    val g = (this shr 8) and 0xff
    val b = this and 0xff
    return abs(r - 0xE0) < 8 && abs(g - 0x7A) < 8 && abs(b - 0x3F) < 8
  }

  private fun Int.toHexString(): String = "#${(this and 0xFFFFFF).toString(16).padStart(6, '0')}"

  private object StalledInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult = awaitCancellation()
  }

  private companion object {
    const val Cover = "https://abs.example.com/api/items/li_1/cover?ts=5"
    const val Width = 400
    const val Height = 400
    const val SceneDensity = 2f
    const val FrameNanos = 1_000_000_000L
    const val MaxCacheBytes = 8L * 1024L * 1024L
    const val CachedCoverArgb = 0xFFE07A3F.toInt()
  }
}
