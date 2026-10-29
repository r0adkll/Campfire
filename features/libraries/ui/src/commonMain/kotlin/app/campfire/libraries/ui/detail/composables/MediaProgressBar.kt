// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.extensions.timeAgo
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Check
import app.campfire.common.compose.icons.rounded.Edit
import app.campfire.common.compose.icons.rounded.KeyboardDoubleArrowRight
import app.campfire.common.compose.util.withDensity
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.asSeconds
import app.campfire.core.extensions.fluentIf
import app.campfire.core.extensions.readableFormat
import app.campfire.core.extensions.tenthsPercent
import app.campfire.core.model.MediaProgress
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.last_read_just_now
import campfire.features.libraries.ui.generated.resources.last_read_time_ago
import campfire.features.libraries.ui.generated.resources.listening_dates_edit_action
import campfire.features.libraries.ui.generated.resources.remaining_duration_finished
import campfire.features.libraries.ui.generated.resources.remaining_duration_finished_undated
import campfire.features.libraries.ui.generated.resources.remaining_duration_format
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MediaProgressBar(
  isPlaying: Boolean,
  playbackSpeed: Float,
  totalDuration: Duration,
  progress: MediaProgress,
  modifier: Modifier = Modifier,
  /** The chapter an ebook is being read in, shown before when it was last read. */
  chapter: String? = null,
  /** Says which progress this is, where an item shows more than one. */
  leadingIcon: ImageVector? = null,
  /**
   * Opens the finished progress's listening dates. The bar and the line under it are one target,
   * padded out to a comfortable size, so the content sits [EditableInset] in from the bar's edges.
   */
  onEditDates: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .then(
        if (onEditDates != null) {
          Modifier
            .clip(EditableShape)
            .clickable(
              onClickLabel = stringResource(Res.string.listening_dates_edit_action),
              role = Role.Button,
              onClick = onEditDates,
            )
            .padding(horizontal = EditableInset, vertical = 11.dp)
            .testTag("progress_edit_dates")
        } else {
          Modifier
        },
      ),
  ) {
    LinearWavyProgressIndicator(
      progress = { progress.actualProgress },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("progress_indicator"),
      trackColor = MaterialTheme.colorScheme.secondaryContainer,
      color = if (progress.isFinished) {
        MaterialTheme.colorScheme.inversePrimary
      } else {
        MaterialTheme.colorScheme.primary
      },
      stroke = Stroke(
        width = withDensity { 6.dp.toPx() },
        cap = StrokeCap.Round,
      ),
      trackStroke = Stroke(
        width = withDensity { 6.dp.toPx() },
        cap = StrokeCap.Round,
      ),
      amplitude = if (isPlaying) {
        WavyProgressIndicatorDefaults.indicatorAmplitude
      } else {
        { 0f }
      },
    )

    Spacer(Modifier.height(4.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (progress.isFinished) {
        Icon(
          CampfireIcons.Rounded.Check,
          contentDescription = null,
          tint = Color.Green,
          modifier = Modifier
            .size(14.dp)
            .testTag("icon_finished_check"),
        )
        Spacer(Modifier.width(4.dp))
      } else if (leadingIcon != null) {
        Icon(
          leadingIcon,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
      }

      val duration = progress.duration?.takeIf { it > 0f } ?: totalDuration.asSeconds()
      val remainingText = when {
        progress.isFinished ->
          progress.finishedAt
            ?.let { stringResource(Res.string.remaining_duration_finished, it.asDate().readableFormat) }
            // Progress an older server finished can be missing when
            ?: stringResource(Res.string.remaining_duration_finished_undated)
        else -> {
          val remainingDurationMillis = (duration - (duration * progress.actualProgress)).div(playbackSpeed) * 1000f
          when {
            // An ebook has no time left to show, so it shows when it was last picked up
            duration <= 0f -> listOfNotNull(chapter, lastReadLabel(progress.lastUpdate).ifEmpty { null })
              .joinToString(" · ")
            !remainingDurationMillis.isNaN() -> {
              val remainingDuration = remainingDurationMillis.roundToLong().milliseconds.readoutFormat()
              stringResource(Res.string.remaining_duration_format, remainingDuration)
            }
            else -> "--"
          }
        }
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val isAccelerated = playbackSpeed != 1f && !progress.isFinished
        AnimatedVisibility(
          visible = isAccelerated,
        ) {
          Icon(
            CampfireIcons.Rounded.KeyboardDoubleArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
              .testTag("accelerated_icon"),
          )
        }
        Text(
          text = remainingText,
          style = MaterialTheme.typography.labelSmall.fluentIf(isAccelerated) {
            copy(
              fontWeight = FontWeight.Bold,
              fontStyle = FontStyle.Italic,
              fontSize = 12.sp,
            )
          },
          fontWeight = FontWeight.SemiBold,
        )
        if (onEditDates != null) {
          Spacer(Modifier.width(4.dp))
          Icon(
            CampfireIcons.Rounded.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
          )
        }
      }

      Spacer(Modifier.weight(1f))

      // How far through is all an ebook has to show, so it reads to the tenth
      val percent = if (duration <= 0f) {
        progress.actualProgress.tenthsPercent
      } else {
        progress.actualProgress.times(100f)
          .takeIf { !it.isNaN() }
          ?.roundToInt()
          ?.toString()
          ?: "--"
      }
      Text(
        text = "$percent%",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

/** How far the editable bar's touch target reaches past the bar on either side. */
internal val EditableInset = 12.dp

private val EditableShape = RoundedCornerShape(16.dp)

@Composable
private fun lastReadLabel(lastUpdate: Long): String = when {
  lastUpdate <= 0L -> ""
  Clock.System.now().toEpochMilliseconds() - lastUpdate < 1.minutes.inWholeMilliseconds ->
    stringResource(Res.string.last_read_just_now)
  else -> stringResource(Res.string.last_read_time_ago, lastUpdate.timeAgo)
}
