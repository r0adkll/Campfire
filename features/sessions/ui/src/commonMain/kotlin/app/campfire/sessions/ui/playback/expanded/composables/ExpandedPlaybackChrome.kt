// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.ui.cast.CastButton
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.KeyboardArrowDown
import app.campfire.common.compose.icons.rounded.QueueMusic
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_close
import campfire.features.sessions.ui.generated.resources.action_queue
import org.jetbrains.compose.resources.stringResource

/**
 * The app bar across the top of every expanded player: a queue toggle as the title once there
 * is a queue, [navigationIcon] at the start, and either the clear-queue confirmation (while the
 * queue is showing) or [trailingActions] at the end.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExpandedPlaybackTopBar(
  hasQueue: Boolean,
  showQueue: Boolean,
  onShowQueueChange: (Boolean) -> Unit,
  onClearQueue: () -> Unit,
  navigationIcon: @Composable () -> Unit,
  containerColor: Color,
  windowInsets: WindowInsets,
  modifier: Modifier = Modifier,
  contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
  queueButtonSize: Dp = ButtonDefaults.MinHeight,
  trailingActions: @Composable RowScope.() -> Unit = { CastButton() },
) {
  TopAppBar(
    title = {
      AnimatedVisibility(
        visible = hasQueue,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        QueueButton(
          checked = showQueue,
          onCheckedChange = onShowQueueChange,
          buttonSize = queueButtonSize,
        )
      }
    },
    navigationIcon = navigationIcon,
    actions = {
      AnimatedContent(
        targetState = hasQueue && showQueue,
      ) { isQueueVisible ->
        Row(
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = if (isQueueVisible) 8.dp else 0.dp),
        ) {
          if (isQueueVisible) {
            ClearQueueButton(onConfirmClick = onClearQueue)
          } else {
            trailingActions()
          }
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = containerColor,
      navigationIconContentColor = contentColor,
      actionIconContentColor = contentColor,
    ),
    windowInsets = windowInsets,
    modifier = modifier,
  )
}

/**
 * The top bar turned on its side for the wide, short player: [navigationIcon] at the top, the
 * queue toggle (icon-only, and its clear-queue confirmation, while the queue is showing) beneath,
 * and [trailingActions] pinned at the bottom. Such a region has width to spare and no height, so
 * the chrome takes a strip off the leading edge rather than off the top.
 *
 * The clear-queue confirmation expands sideways; its slot keeps the rail's width and lets the
 * confirmation overflow across the content rather than pushing it.
 */
@Composable
internal fun ExpandedPlaybackRail(
  hasQueue: Boolean,
  showQueue: Boolean,
  onShowQueueChange: (Boolean) -> Unit,
  onClearQueue: () -> Unit,
  navigationIcon: @Composable () -> Unit,
  contentColor: Color,
  modifier: Modifier = Modifier,
  trailingActions: @Composable () -> Unit = { CastButton() },
) {
  CompositionLocalProvider(LocalContentColor provides contentColor) {
    Column(
      modifier = modifier
        .fillMaxHeight()
        .width(RailWidth)
        .padding(vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      navigationIcon()

      if (hasQueue) {
        val queueLabel = stringResource(Res.string.action_queue)
        IconButtonTooltip(text = queueLabel) {
          IconToggleButton(
            checked = showQueue,
            onCheckedChange = onShowQueueChange,
          ) {
            Icon(CampfireIcons.Rounded.QueueMusic, contentDescription = queueLabel)
          }
        }
      }

      if (hasQueue && showQueue) {
        ClearQueueButton(
          onConfirmClick = onClearQueue,
          modifier = Modifier
            .requiredWidth(RailWidth)
            .wrapContentWidth(align = Alignment.Start, unbounded = true),
        )
      }

      Spacer(Modifier.weight(1f))

      trailingActions()
    }
  }
}

/** Wide enough for a standard icon button and its touch padding. */
private val RailWidth = 56.dp

/** The chevron that collapses the expanded player back into the floating bar. */
@Composable
internal fun PlayerCloseButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val closeLabel = stringResource(Res.string.action_close)
  IconButtonTooltip(text = closeLabel, modifier = modifier) {
    IconButton(onClick = onClick) {
      Icon(CampfireIcons.Rounded.KeyboardArrowDown, contentDescription = closeLabel)
    }
  }
}
