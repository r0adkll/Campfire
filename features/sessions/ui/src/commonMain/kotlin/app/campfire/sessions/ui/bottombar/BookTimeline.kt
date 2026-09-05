// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.KeyboardDoubleArrowRight
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Bookmark
import app.campfire.core.model.Chapter
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.timeline_content_description
import campfire.features.sessions.ui.generated.resources.timeline_time_placeholder
import kotlin.math.roundToInt
import kotlin.time.Duration
import org.jetbrains.compose.resources.stringResource

/**
 * The whole book on one thin track. Chapter starts are ticks above the track and bookmarks are
 * dots below it; hovering anywhere shows the time under the pointer (snapping to a nearby
 * marker and naming it), clicking seeks there, and dragging scrubs. Elapsed and remaining
 * (speed-adjusted) book time flank the track.
 *
 * With no duration yet, or when disabled, the track renders flat with placeholder times and no
 * thumb, so the bar keeps its shape before a session is primed or when nothing is playing.
 */
@Composable
internal fun BookTimeline(
  position: Duration,
  duration: Duration,
  chapters: List<Chapter>,
  bookmarks: List<Bookmark>,
  playbackSpeed: Float,
  enabled: Boolean,
  onSeek: (Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactive = enabled && duration > Duration.ZERO
  val markers = remember(chapters, bookmarks) {
    TimelineMath.chapterMarkers(chapters) + TimelineMath.bookmarkMarkers(bookmarks)
  }

  var scrubFraction by remember { mutableStateOf<Float?>(null) }
  var hover by remember { mutableStateOf<Hover?>(null) }
  var trackWidthPx by remember { mutableStateOf(0f) }

  val displayed = scrubFraction?.let { TimelineMath.timeAt(it, duration) } ?: position
  val placeholder = stringResource(Res.string.timeline_time_placeholder)

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = if (interactive) displayed.readoutFormat().trim() else placeholder,
      textAlign = TextAlign.End,
      style = MaterialTheme.typography.labelSmall,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(TimeLabelWidth),
    )

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val playedColor = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val bookmarkColor = MaterialTheme.colorScheme.tertiary
    val hoverColor = MaterialTheme.colorScheme.onSurface
    val snapThresholdPx = with(LocalDensity.current) { SnapThreshold.toPx() }

    Box(
      modifier = Modifier
        .weight(1f)
        .height(TrackAreaHeight)
        .padding(horizontal = 8.dp)
        .semantics {
          progressBarRangeInfo = ProgressBarRangeInfo(TimelineMath.fraction(displayed, duration), 0f..1f)
        }
        .fluentIf(interactive) {
          pointerHoverIcon(PointerIcon.Hand)
            // Hover tracking through the common pointer API (onPointerEvent is desktop-only)
            .pointerInput(duration, markers) {
              awaitPointerEventScope {
                while (true) {
                  val event = awaitPointerEvent()
                  when (event.type) {
                    PointerEventType.Move,
                    PointerEventType.Enter,
                    -> if (event.changes.none { it.pressed }) {
                      val x = event.changes.first().position.x
                      hover = Hover(x, TimelineMath.snap(x, trackWidthPx, duration, markers, snapThresholdPx))
                    }
                    PointerEventType.Exit -> hover = null
                  }
                }
              }
            }
            .pointerInput(duration, markers) {
              awaitEachGesture {
                val down = awaitFirstDown()
                val width = size.width.toFloat()
                var lastX = down.position.x
                var dragged = false
                drag(down.id) { change ->
                  dragged = true
                  lastX = change.position.x
                  scrubFraction = TimelineMath.fractionAt(lastX, width)
                  hover = Hover(lastX, null)
                  change.consume()
                }
                val target = if (dragged) {
                  TimelineMath.timeAt(TimelineMath.fractionAt(lastX, width), duration)
                } else {
                  TimelineMath.snap(lastX, width, duration, markers, snapThresholdPx)?.time
                    ?: TimelineMath.timeAt(TimelineMath.fractionAt(lastX, width), duration)
                }
                scrubFraction = null
                onSeek(target)
              }
            }
        },
    ) {
      val description = stringResource(Res.string.timeline_content_description)
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(TrackAreaHeight)
          .semantics { contentDescription = description },
      ) {
        trackWidthPx = size.width
        val centerY = size.height / 2f
        val trackHeight = TrackHeight.toPx()
        val radius = CornerRadius(trackHeight / 2f)

        drawRoundRect(
          color = trackColor,
          topLeft = Offset(0f, centerY - trackHeight / 2f),
          size = Size(size.width, trackHeight),
          cornerRadius = radius,
        )

        if (!interactive) return@Canvas

        val playedX = TimelineMath.xOf(displayed, duration, size.width)
        drawRoundRect(
          color = playedColor,
          topLeft = Offset(0f, centerY - trackHeight / 2f),
          size = Size(playedX, trackHeight),
          cornerRadius = radius,
        )

        val hovered = hover?.marker
        markers.forEach { marker ->
          val x = TimelineMath.xOf(marker.time, duration, size.width)
          val isHovered = marker == hovered
          when (marker) {
            is TimelineMarker.ChapterMarker -> {
              val tickHeight = (if (isHovered) HoveredTickHeight else TickHeight).toPx()
              val tickWidth = TickWidth.toPx()
              drawRoundRect(
                color = if (isHovered) hoverColor else tickColor,
                topLeft = Offset(x - tickWidth / 2f, centerY - trackHeight / 2f - TickGap.toPx() - tickHeight),
                size = Size(tickWidth, tickHeight),
                cornerRadius = CornerRadius(tickWidth / 2f),
              )
            }
            is TimelineMarker.BookmarkMarker -> {
              drawCircle(
                color = if (isHovered) hoverColor else bookmarkColor,
                radius = (if (isHovered) HoveredBookmarkRadius else BookmarkRadius).toPx(),
                center = Offset(x, centerY + trackHeight / 2f + BookmarkGap.toPx() + BookmarkRadius.toPx()),
              )
            }
          }
        }

        drawCircle(
          color = playedColor,
          radius = ThumbRadius.toPx(),
          center = Offset(playedX, centerY),
        )
      }

      hover?.let { current ->
        if (interactive) {
          TimelineTooltip(
            hover = current,
            duration = duration,
            trackWidthPx = trackWidthPx,
          )
        }
      }
    }

    val isAccelerated = playbackSpeed != 1f
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.width(TimeLabelWidth),
    ) {
      AnimatedVisibility(visible = isAccelerated && interactive) {
        Icon(
          CampfireIcons.Rounded.KeyboardDoubleArrowRight,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
          tint = MaterialTheme.colorScheme.secondary,
        )
      }
      val remaining = ((duration - displayed) / playbackSpeed.toDouble()).coerceAtLeast(Duration.ZERO)
      Text(
        text = if (interactive) remaining.readoutFormat().trim() else placeholder,
        textAlign = TextAlign.Start,
        style = MaterialTheme.typography.labelSmall.fluentIf(isAccelerated && interactive) {
          copy(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 12.sp,
          )
        },
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

private data class Hover(val xPx: Float, val marker: TimelineMarker?)

/** Time (and marker name) under the pointer, floating above the track at the pointer's x. */
@Composable
private fun TimelineTooltip(
  hover: Hover,
  duration: Duration,
  trackWidthPx: Float,
) {
  val time = hover.marker?.time ?: TimelineMath.timeAt(TimelineMath.fractionAt(hover.xPx, trackWidthPx), duration)
  Popup(
    popupPositionProvider = remember(hover.xPx) { AboveAnchorAt(hover.xPx.roundToInt()) },
  ) {
    Surface(
      color = MaterialTheme.colorScheme.inverseSurface,
      contentColor = MaterialTheme.colorScheme.inverseOnSurface,
      shape = MaterialTheme.shapes.small,
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        hover.marker?.let { marker ->
          Text(
            text = marker.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
          )
        }
        Text(
          text = time.readoutFormat(),
          style = MaterialTheme.typography.labelSmall,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

/** Centers the popup horizontally on [anchorX] within the anchor and sits it just above the anchor. */
private class AboveAnchorAt(private val anchorX: Int) : PopupPositionProvider {
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize,
  ): IntOffset {
    val x = (anchorBounds.left + anchorX - popupContentSize.width / 2)
      .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
    val y = (anchorBounds.top - popupContentSize.height - TooltipGapPx).coerceAtLeast(0)
    return IntOffset(x, y)
  }
}

private val TimeLabelWidth = 96.dp
private val TrackAreaHeight = 28.dp
private val TrackHeight = 3.dp
private val ThumbRadius = 5.dp
private val TickWidth = 2.dp
private val TickHeight = 7.dp
private val HoveredTickHeight = 10.dp
private val TickGap = 3.dp
private val BookmarkRadius = 3.dp
private val HoveredBookmarkRadius = 4.5.dp
private val BookmarkGap = 3.dp
private val SnapThreshold = 6.dp
private const val TooltipGapPx = 6
