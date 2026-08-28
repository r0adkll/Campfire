package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.outline.Autoplay
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.KeyboardArrowDown
import app.campfire.common.compose.icons.rounded.MotionPlay
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.Sensors
import app.campfire.core.model.PlayMethod
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_currently_playing
import campfire.features.libraries.ui.generated.resources.action_direct_play
import campfire.features.libraries.ui.generated.resources.action_ebook_not_supported
import campfire.features.libraries.ui.generated.resources.action_play
import campfire.features.libraries.ui.generated.resources.action_resume_listening
import campfire.features.libraries.ui.generated.resources.action_resume_streaming
import campfire.features.libraries.ui.generated.resources.action_stream
import campfire.features.libraries.ui.generated.resources.menu_item_download
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayAndDownloadButtons(
  offlineDownload: OfflineDownload?,
  onPlayClick: (PlayMethod?) -> Unit,
  onDownloadClick: () -> Unit,
  hasProgress: Boolean,
  isCurrentSession: Boolean,
  isEbookOnly: Boolean,
  canStreamHls: Boolean,
  willStreamHls: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val hasOfflineDownload = offlineDownload?.state != null &&
      offlineDownload.state != OfflineDownload.State.None

    val size = ButtonDefaults.MediumContainerHeight
    val splitButtonRadius = 4.dp
    val pressedSplitButtonRadius = 6.dp
    val pressedCornerRadius = if (hasOfflineDownload) pressedSplitButtonRadius else 12.dp
    val endCornerRadius by animateDpAsState(
      targetValue = if (hasOfflineDownload || isEbookOnly) size / 2 else splitButtonRadius,
    )

    Button(
      enabled = !isCurrentSession && !isEbookOnly,
      onClick = { onPlayClick(null) },
      modifier = Modifier
        .heightIn(size)
        .weight(1f)
        .testTag("button_play"),
      shapes = ButtonShapes(
        shape = RoundedCornerShape(
          topStart = CornerSize(50),
          bottomStart = CornerSize(50),
          topEnd = CornerSize(endCornerRadius),
          bottomEnd = CornerSize(endCornerRadius),
        ),
        pressedShape = RoundedCornerShape(
          topStart = CornerSize(12.dp), // Corner Medium
          bottomStart = CornerSize(12.dp), // Corner Medium
          topEnd = CornerSize(pressedCornerRadius),
          bottomEnd = CornerSize(pressedCornerRadius),
        ),
      ),
      contentPadding = ButtonDefaults.MediumContentPadding,
    ) {
      Icon(
        when {
          isEbookOnly -> Icons.AutoMirrored.Rounded.MenuBook
          isCurrentSession -> CampfireIcons.Rounded.MotionPlay
          hasProgress -> Icons.Outlined.Autoplay
          willStreamHls -> CampfireIcons.Rounded.Sensors
          else -> CampfireIcons.Rounded.PlayArrow
        },
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
      Text(
        text = when {
          isEbookOnly -> stringResource(Res.string.action_ebook_not_supported)
          isCurrentSession -> stringResource(Res.string.action_currently_playing)
          hasProgress -> stringResource(Res.string.action_resume_listening)
          willStreamHls -> stringResource(Res.string.action_stream)
          else -> stringResource(Res.string.action_play)
        },
        style = ButtonDefaults.textStyleFor(size),
      )
    }

    Spacer(Modifier.width(2.dp))

    AnimatedVisibility(
      visible = (!hasOfflineDownload && !isEbookOnly) || canStreamHls,
      enter = expandHorizontally(),
      exit = shrinkHorizontally(),
    ) {
      if (canStreamHls && !isCurrentSession) {
        var expanded by remember { mutableStateOf(false) }
        val trailingButtonRadius by animateDpAsState(
          if (expanded) 20.dp else splitButtonRadius,
        )

        val trailingButtonContainerColor by animateColorAsState(
          if (expanded) {
            MaterialTheme.colorScheme.primaryContainer
          } else MaterialTheme.colorScheme.primary,
        )

        val trailingButtonContentColor by animateColorAsState(
          if (expanded) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else MaterialTheme.colorScheme.onPrimary,
        )

        val borderWidth by animateDpAsState(
          if (expanded) 2.dp else 0.dp,
        )

        TrailingButton(
          size = size,
          splitButtonRadius = trailingButtonRadius,
          pressedSplitButtonRadius = pressedSplitButtonRadius,
          onClick = { expanded = !expanded },
          colors = ButtonDefaults.buttonColors(
            containerColor = trailingButtonContainerColor,
            contentColor = trailingButtonContentColor,
          ),
          border = BorderStroke(
            width = borderWidth,
            color = MaterialTheme.colorScheme.primary,
          ),
        ) {
          val rotation by animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            label = "Trailing Icon Rotation",
          )
          Icon(
            CampfireIcons.Rounded.KeyboardArrowDown,
            contentDescription = "More options",
            modifier = Modifier
              .size(ButtonDefaults.iconSizeFor(size))
              .graphicsLayer {
                rotationZ = rotation
              },
          )
        }

        PlayOptionsMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false },
          willStreamHls = willStreamHls,
          hasProgress = hasProgress,
          hasOfflineDownload = hasOfflineDownload,
          isEbookOnly = isEbookOnly,
          onPlayClick = onPlayClick,
          onDownloadClick = onDownloadClick,
        )
      } else {
        TrailingButton(
          size = size,
          splitButtonRadius = splitButtonRadius,
          pressedSplitButtonRadius = pressedSplitButtonRadius,
          onClick = onDownloadClick,
        ) {
          Icon(
            CampfireIcons.Rounded.Download,
            contentDescription = "Download",
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrailingButton(
  size: Dp,
  splitButtonRadius: Dp,
  pressedSplitButtonRadius: Dp,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  colors: ButtonColors = ButtonDefaults.buttonColors(),
  border: BorderStroke? = null,
  icon: @Composable () -> Unit,
) {
  Button(
    onClick = onClick,
    shapes = ButtonShapes(
      shape = RoundedCornerShape(
        topStart = CornerSize(splitButtonRadius),
        bottomStart = CornerSize(splitButtonRadius),
        topEnd = CornerSize(50),
        bottomEnd = CornerSize(50),
      ),
      pressedShape = RoundedCornerShape(
        topStart = CornerSize(pressedSplitButtonRadius),
        bottomStart = CornerSize(pressedSplitButtonRadius),
        topEnd = CornerSize(12.dp),
        bottomEnd = CornerSize(12.dp),
      ),
    ),
    colors = colors,
    border = border,
    contentPadding = PaddingValues(
      start = 20.dp,
      end = 24.dp,
      top = 16.dp,
      bottom = 16.dp,
    ),
    modifier = modifier
      .heightIn(size)
      .testTag("button_download"),
  ) {
    // This is a DUMB hack to make it height match the left side button
    Text(
      text = "",
      style = ButtonDefaults.textStyleFor(size),
    )
    icon()
  }
}

@Composable
private fun PlayOptionsMenu(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  willStreamHls: Boolean,
  hasProgress: Boolean,
  hasOfflineDownload: Boolean,
  isEbookOnly: Boolean,
  onPlayClick: (PlayMethod) -> Unit,
  onDownloadClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    modifier = modifier.sizeIn(minWidth = 180.dp),
    shape = MaterialTheme.shapes.medium,
  ) {
    if (willStreamHls) {
      DropdownMenuItem(
        text = { Text(stringResource(Res.string.action_direct_play)) },
        onClick = { onPlayClick(PlayMethod.DirectPlay) },
        leadingIcon = { Icon(CampfireIcons.Rounded.PlayArrow, contentDescription = null) },
      )
    } else {
      DropdownMenuItem(
        text = {
          Text(
            text = if (hasProgress) {
              stringResource(Res.string.action_resume_streaming)
            } else {
              stringResource(Res.string.action_stream)
            },
          )
        },
        onClick = { onPlayClick(PlayMethod.Transcode) },
        leadingIcon = { Icon(CampfireIcons.Rounded.Sensors, contentDescription = null) },
      )
    }

    if (!hasOfflineDownload && !isEbookOnly) {
      DropdownMenuItem(
        text = { Text(stringResource(Res.string.menu_item_download)) },
        onClick = onDownloadClick,
        leadingIcon = { Icon(CampfireIcons.Rounded.Download, contentDescription = null) },
      )
    }
  }
}
