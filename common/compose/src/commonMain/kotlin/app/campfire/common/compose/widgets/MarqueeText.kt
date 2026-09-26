// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * A single line of text that scrolls a few times when it does not fit, fading out at both ends so
 * it reads as running under the surface rather than being cut off.
 *
 * Text that fits is laid out exactly as a plain [Text] would be, [autoSize] included — the marquee
 * only takes over once the text still overflows at its smallest size, and keeps that size while it
 * scrolls. It scrolls [MarqueeIterations] times and then rests at its start; changing the text or
 * the available width re-measures, so a new title scrolls again and a title that now fits stops.
 *
 * With [scrollEnabled] false, overflowing text is ellipsized instead.
 */
@Composable
fun MarqueeText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  style: TextStyle = LocalTextStyle.current,
  fontWeight: FontWeight? = null,
  fontFamily: FontFamily? = null,
  textAlign: TextAlign? = null,
  autoSize: TextAutoSize? = null,
  scrollEnabled: Boolean = true,
  fadeEdgeWidth: Dp = MarqueeFadeEdgeWidth,
) {
  BoxWithConstraints(modifier) {
    // The size the text settled on before it gave up and overflowed, or null while it fits.
    var overflowFontSize by remember(text, maxWidth) { mutableStateOf<TextUnit?>(null) }

    val fittedFontSize = overflowFontSize
    when {
      fittedFontSize == null -> Text(
        text = text,
        color = color,
        style = style,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        autoSize = autoSize,
        maxLines = 1,
        // Not Ellipsis: autoSize judges an ellipsized fit by isLineEllipsized, which Skiko never
        // reports, so the text would not shrink on desktop or iOS. Clipped text never shows anyway,
        // as overflowing text switches to one of the branches below.
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
          if (result.hasVisualOverflow) {
            overflowFontSize = result.layoutInput.style.fontSize
          }
        },
      )

      scrollEnabled -> ScrollingText(
        text = text,
        color = color,
        style = style.copy(fontSize = fittedFontSize),
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        containerWidthPx = constraints.maxWidth,
        fadeEdgeWidth = fadeEdgeWidth,
      )

      else -> Text(
        text = text,
        color = color,
        style = style.copy(fontSize = fittedFontSize),
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ScrollingText(
  text: String,
  color: Color,
  style: TextStyle,
  fontWeight: FontWeight?,
  fontFamily: FontFamily?,
  textAlign: TextAlign?,
  containerWidthPx: Int,
  fadeEdgeWidth: Dp,
  modifier: Modifier = Modifier,
) {
  var contentWidthPx by remember { mutableIntStateOf(0) }
  val velocityPxPerSecond = with(LocalDensity.current) { MarqueeVelocity.toPx() }
  val passMillis = marqueePassMillis(
    distancePx = contentWidthPx + marqueeSpacingPx(containerWidthPx),
    velocityPxPerSecond = velocityPxPerSecond,
  )

  // basicMarquee keeps its offset to itself, so follow the same timeline on the same frame clock to
  // know when the text is moving. The start edge only fades while it is: at rest the text starts
  // flush with it, and fading it would dim the first letters.
  var moving by remember { mutableStateOf(false) }
  LaunchedEffect(passMillis) {
    moving = false
    if (contentWidthPx == 0) return@LaunchedEffect
    val start = withFrameMillis { it }
    while (true) {
      val elapsed = withFrameMillis { it } - start
      moving = isMarqueeMoving(elapsed, passMillis)
      if (elapsed >= marqueeTotalMillis(passMillis)) break
    }
  }
  val startFade by animateFloatAsState(
    targetValue = if (moving) 1f else 0f,
    animationSpec = tween(StartFadeMillis),
  )

  Text(
    text = text,
    color = color,
    style = style,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    textAlign = textAlign,
    maxLines = 1,
    softWrap = false,
    onTextLayout = { contentWidthPx = it.size.width },
    modifier = modifier
      .fadingEdges(fadeEdgeWidth, startFade = { startFade })
      .basicMarquee(
        iterations = MarqueeIterations,
        initialDelayMillis = MarqueeDelayMillis,
        repeatDelayMillis = MarqueeDelayMillis,
        spacing = MarqueeSpacing { _, containerWidth -> marqueeSpacingPx(containerWidth) },
        velocity = MarqueeVelocity,
      ),
  )
}

/**
 * Fades the content out over [width] at its start and end edges, keeping the fade within the
 * bounds this modifier is given. [startFade] scales the start edge's fade from none (0) to full
 * (1), and is read at draw time so animating it does not recompose.
 */
fun Modifier.fadingEdges(
  width: Dp,
  startFade: () -> Float = { 1f },
): Modifier = this
  .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
  .drawWithContent {
    drawContent()
    if (size.width <= 0f) return@drawWithContent
    val edge = (width.toPx() / size.width).coerceIn(0f, 0.5f)
    drawRect(
      brush = Brush.horizontalGradient(
        0f to Color.Black.copy(alpha = 1f - startFade().coerceIn(0f, 1f)),
        edge to Color.Black,
        1f - edge to Color.Black,
        1f to Color.Transparent,
      ),
      blendMode = BlendMode.DstIn,
    )
  }

/** How long one pass takes to scroll [distancePx], matching basicMarquee's velocity-based tween. */
internal fun marqueePassMillis(distancePx: Int, velocityPxPerSecond: Float): Long =
  ceil(distancePx / (velocityPxPerSecond / 1000f)).toLong()

/**
 * Whether the text is moving [elapsedMillis] into the marquee: it waits [MarqueeDelayMillis] before
 * each of its [MarqueeIterations] passes, and rests once they are done.
 */
internal fun isMarqueeMoving(elapsedMillis: Long, passMillis: Long): Boolean {
  if (elapsedMillis >= marqueeTotalMillis(passMillis)) return false
  val intoCycle = elapsedMillis % (MarqueeDelayMillis + passMillis)
  return intoCycle >= MarqueeDelayMillis
}

private fun marqueeTotalMillis(passMillis: Long): Long = (MarqueeDelayMillis + passMillis) * MarqueeIterations

private fun marqueeSpacingPx(containerWidthPx: Int): Int = containerWidthPx / 3

internal const val MarqueeIterations = 3
internal const val MarqueeDelayMillis = 1_200
private const val StartFadeMillis = 150
private val MarqueeVelocity = 30.dp
private val MarqueeFadeEdgeWidth = 16.dp
