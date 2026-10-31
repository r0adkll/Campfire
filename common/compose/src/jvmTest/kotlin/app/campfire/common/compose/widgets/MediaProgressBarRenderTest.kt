// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.extensions.asHct
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.test.mediaProgress
import app.campfire.home.ui.libraryItem
import coil3.ColorImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.r0adkll.swatchbuckler.color.contrast.Contrast
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpecs
import com.r0adkll.swatchbuckler.color.dynamiccolor.DynamicScheme
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.r0adkll.swatchbuckler.color.utils.ColorUtils
import com.r0adkll.swatchbuckler.compose.util.asColorScheme
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * Draws [MediaProgressBar] over a cover-colored backdrop in every scheme style, light and dark,
 * and reads back the pixels: the track must be opaque and stay at least 3:1 from the fill. Card
 * renders land in `build/renders` for eyeballing.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSharedTransitionApi::class)
class MediaProgressBarRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @AfterTest
  fun tearDown() {
    SingletonImageLoader.reset()
  }

  @Test
  fun `the track is opaque and contrasts with the fill in every scheme style`() {
    for (variant in Variant.entries) {
      for (seed in Seeds) {
        for (isDark in listOf(false, true)) {
          val scheme = scheme(seed, variant, isDark)
          val where = "$variant ${seed.toHex()} ${if (isDark) "dark" else "light"}"

          val bitmap = renderBar(scheme)
          val fill = bitmap.getColor(BarWidthPx / 4, BarHeightPx / 2)
          val track = bitmap.getColor(BarWidthPx * 3 / 4, BarHeightPx / 2)

          assertEquals(scheme.primary.toArgb(), fill, "fill, $where")
          assertEquals(scheme.surfaceContainerHighest.toArgb(), track, "track over the cover, $where")
          val ratio = contrast(fill, track)
          assertTrue(ratio >= NonTextContrast, "fill on track is %.2f:1, $where".format(ratio))
        }
      }
    }
  }

  @Test
  fun `cards show progress at the bottom of the cover`() {
    installCoverLoader()
    for (isDark in listOf(false, true)) {
      val mode = if (isDark) "dark" else "light"
      renderCards("progress-cards-expressive-$mode", scheme(Seeds.first(), Variant.EXPRESSIVE, isDark))
    }
  }

  private fun renderBar(scheme: ColorScheme): Bitmap = ImageComposeScene(
    width = BarWidthPx,
    height = BarHeightPx,
    density = Density(1f),
    coroutineContext = Dispatchers.Unconfined,
  ) {
    MaterialTheme(colorScheme = scheme) {
      Box(Modifier.fillMaxSize().background(CoverColor)) {
        MediaProgressBar(
          mediaProgress = mediaProgress(libraryItemId = "li_1", progress = 0.5f),
          trackHeight = BarHeightPx.dp,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }.use { scene -> scene.render().toBitmap() }

  private fun renderCards(name: String, scheme: ColorScheme) {
    val image = ImageComposeScene(
      width = (CardsWidth * SceneDensity).toInt(),
      height = (CardsHeight * SceneDensity).toInt(),
      density = Density(SceneDensity),
      coroutineContext = Dispatchers.Unconfined,
    ) {
      PreviewSharedElementTransitionLayout {
        MaterialTheme(colorScheme = scheme) {
          CompositionLocalProvider(LocalContentLayout provides ContentLayout.Root) {
            Row(
              modifier = Modifier.fillMaxSize().background(scheme.surface).padding(16.dp),
              verticalAlignment = Alignment.Top,
            ) {
              listOf(0.2f, 0.55f, 0.85f).forEachIndexed { index, progress ->
                LibraryItemCard(
                  item = libraryItem(id = "li_$index"),
                  progress = mediaProgress(libraryItemId = "li_$index", progress = progress),
                  marqueeEnabled = false,
                  modifier = Modifier.width(150.dp).padding(end = 12.dp),
                )
              }
            }
          }
        }
      }
    }.use { scene ->
      scene.render()
      scene.render(nanoTime = FrameNanos)
    }
    val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
    val file = File(renders, "$name.png")
    file.writeBytes(bytes)
    println("rendered ${file.absolutePath}")
    assertTrue(bytes.size > 1_000)
  }

  private fun installCoverLoader() {
    SingletonImageLoader.reset()
    SingletonImageLoader.setUnsafe(
      ImageLoader.Builder(PlatformContext.INSTANCE)
        .components { add(CoverInterceptor) }
        .build(),
    )
  }

  private object CoverInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult = SuccessResult(
      image = ColorImage(CoverColor.toArgb(), width = 400, height = 400),
      request = chain.request,
      dataSource = DataSource.MEMORY,
    )
  }

  private fun scheme(seed: Color, variant: Variant, isDark: Boolean): ColorScheme {
    val source = seed.asHct()
    val specVersion = if (variant in Spec2025Styles) {
      ColorSpec.SpecVersion.SPEC_2025
    } else {
      ColorSpec.SpecVersion.SPEC_2021
    }
    val spec = ColorSpecs.get(specVersion)
    val platform = DynamicScheme.Platform.PHONE
    return DynamicScheme(
      sourceColorHct = source,
      variant = variant,
      isDark = isDark,
      contrastLevel = 0.0,
      platform = platform,
      specVersion = specVersion,
      primaryPalette = spec.getPrimaryPalette(variant, source, isDark, platform, 0.0),
      secondaryPalette = spec.getSecondaryPalette(variant, source, isDark, platform, 0.0),
      tertiaryPalette = spec.getTertiaryPalette(variant, source, isDark, platform, 0.0),
      neutralPalette = spec.getNeutralPalette(variant, source, isDark, platform, 0.0),
      neutralVariantPalette = spec.getNeutralVariantPalette(variant, source, isDark, platform, 0.0),
      errorPalette = spec.getErrorPalette(variant, source, isDark, platform, 0.0),
    ).asColorScheme()
  }

  private fun Image.toBitmap(): Bitmap = Bitmap().also {
    it.allocPixels(imageInfo)
    assertTrue(readPixels(it))
  }

  private fun contrast(a: Int, b: Int): Double =
    Contrast.ratioOfTones(ColorUtils.lstarFromArgb(a), ColorUtils.lstarFromArgb(b))

  private fun Color.toHex(): String = "#${(toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')}"

  private companion object {
    val Seeds = listOf(Color(0xFF904B3C), Color(0xFF16A34A), Color(0xFF2563EB), Color(0xFFFFD23F))
    val Spec2025Styles = setOf(Variant.EXPRESSIVE, Variant.VIBRANT, Variant.TONAL_SPOT, Variant.NEUTRAL)

    /** Close to most generated primaries, so a see-through track would read as the cover. */
    val CoverColor = Color(0xFF8A5A44)

    const val NonTextContrast = 3.0
    const val BarWidthPx = 200
    const val BarHeightPx = 12
    const val CardsWidth = 520
    const val CardsHeight = 280
    const val SceneDensity = 2f
    const val FrameNanos = 1_000_000_000L
  }
}
