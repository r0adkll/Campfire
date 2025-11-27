@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.isNullOrNone
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.MarkFinished
import app.campfire.common.compose.icons.outline.Autoplay
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Tent
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import app.campfire.libraries.ui.detail.composables.slots.ExpressiveControlSlot
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_delete_offline
import campfire.features.libraries.ui.generated.resources.action_play
import campfire.features.libraries.ui.generated.resources.action_resume_listening
import campfire.features.libraries.ui.generated.resources.action_stop_downloading
import campfire.features.libraries.ui.generated.resources.menu_item_discard_progress
import campfire.features.libraries.ui.generated.resources.menu_item_discard_progress_short
import campfire.features.libraries.ui.generated.resources.menu_item_mark_finished
import campfire.features.libraries.ui.generated.resources.menu_item_mark_finished_short
import campfire.features.libraries.ui.generated.resources.menu_item_mark_not_finished
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

@Composable
internal fun ExpressiveControlBar(
  mediaProgress: MediaProgress?,
  offlineDownload: OfflineDownload?,
  onPlayClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onMarkFinished: () -> Unit,
  onMarkNotFinished: () -> Unit,
  onDiscardProgress: () -> Unit,
  onStopDownloadClick: () -> Unit,
  onDeleteDownloadClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      val hasProgress = mediaProgress != null &&
        mediaProgress.progress > 0f &&
        !mediaProgress.isFinished

      PlayAndDownloadButtons(
        offlineDownload = offlineDownload,
        hasProgress = hasProgress,
        onPlayClick = onPlayClick,
        onDownloadClick = onDownloadClick,
      )

      if (!offlineDownload.isNullOrNone()) {
        Spacer(Modifier.size(8.dp))

        OfflineStatus(
          offlineDownload = offlineDownload,
          onDeleteClick = onDeleteDownloadClick,
          onStopClick = onStopDownloadClick,
        )
      }

      if (mediaProgress != null) {
        Spacer(Modifier.size(2.dp))
      }

      ProgressModifierButtons(
        hasProgress = hasProgress,
        onDiscardProgress = onDiscardProgress,
        mediaProgress = mediaProgress,
        onMarkFinished = onMarkFinished,
        onMarkNotFinished = onMarkNotFinished,
      )
    }
  }
}

@Composable
private fun OfflineStatus(
  offlineDownload: OfflineDownload,
  onDeleteClick: () -> Unit,
  onStopClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    modifier = modifier,
  ) {
    OfflineTitleBar(
      completed = offlineDownload.isCompleted,
      title = {
        Text(
          when (offlineDownload.state) {
            OfflineDownload.State.None,
            OfflineDownload.State.Downloading,
            -> "Downloading"

            OfflineDownload.State.Queued -> "Queued"
            OfflineDownload.State.Stopped -> "Stopped"
            OfflineDownload.State.Failed -> "Failed"
            OfflineDownload.State.Completed -> "Available for offline"
          },
        )
      },
      subtitle = if (offlineDownload.isCompleted) {
        {
          Text(offlineDownload.progress.bytes.asReadableBytes())
        }
      } else {
        null
      },
      trailing = {
        AnimatedContent(
          targetState = offlineDownload.isActive,
        ) { isActive ->
          if (isActive) {
            FilledIconButton(
              onClick = onStopClick,
              shapes = IconButtonDefaults.shapes(),
              modifier = Modifier.size(IconButtonDefaults.extraSmallContainerSize()),
            ) {
              Icon(
                Icons.Rounded.Stop,
                contentDescription = stringResource(Res.string.action_stop_downloading),
                modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
              )
            }
          } else {
            val size = ButtonDefaults.ExtraSmallContainerHeight
            val color = MaterialTheme.colorScheme.error
            Button(
              onClick = onDeleteClick,
              shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.squareShape,
              ),
              colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = MaterialTheme.colorScheme.contentColorFor(color),
              ),
              modifier = Modifier
                .heightIn(size),
              contentPadding = ButtonDefaults.contentPaddingFor(size),
            ) {
              Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(Res.string.action_stop_downloading),
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
              )
              Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
              Text(
                text = stringResource(Res.string.action_delete_offline),
                style = ButtonDefaults.textStyleFor(size),
              )
            }
          }
        }
      },
      modifier = Modifier.testTag("offline_status_title"),
    )

    if (!offlineDownload.isCompleted) {
      OfflineProgressBar(
        progress = offlineDownload.progress.percent,
        bytesDownloaded = offlineDownload.progress.bytes,
        contentLength = offlineDownload.contentLength,
        isIndeterminate = offlineDownload.progress.indeterminate,
        onStopClick = onStopClick,
      )
    }
  }
}

@Composable
internal fun OfflineTitleBar(
  completed: Boolean,
  title: @Composable () -> Unit,
  subtitle: (@Composable () -> Unit)?,
  trailing: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      if (completed) Icons.Rounded.DownloadDone else Icons.Rounded.Downloading,
      contentDescription = null,
      modifier = Modifier
        .padding(16.dp),
    )

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.Start,
    ) {
      ProvideTextStyle(
        if (subtitle != null) {
          MaterialTheme.typography.titleSmall
        } else {
          MaterialTheme.typography.titleMedium
        },
      ) {
        title()
      }

      if (subtitle != null) {
        ProvideTextStyle(
          MaterialTheme.typography.labelMedium,
        ) {
          CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = 0.65f),
          ) {
            subtitle()
          }
        }
      }
    }

    Spacer(Modifier.width(8.dp))

    trailing()

    Spacer(Modifier.width(16.dp))
  }
}

@Composable
private fun OfflineProgressBar(
  progress: Float,
  bytesDownloaded: Long,
  contentLength: Long,
  isIndeterminate: Boolean,
  onStopClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .padding(
        start = 16.dp,
        end = 16.dp,
        bottom = 8.dp,
      ),
  ) {
    if (isIndeterminate) {
      LinearWavyProgressIndicator(
        trackColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("indeterminate_progress_bar"),
      )
    } else {
      LinearWavyProgressIndicator(
        progress = { progress },
        trackColor = MaterialTheme.colorScheme.surface,
        color = LocalContentColor.current,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("determinate_progress_bar"),
      )
    }

    Spacer(Modifier.height(4.dp))

    AnimatedVisibility(
      visible = !isIndeterminate,
    ) {
      Row(
        Modifier.fillMaxWidth(),
      ) {
        Text(
          text = bytesDownloaded.asReadableBytes(),
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = contentLength.asReadableBytes(),
          textAlign = TextAlign.End,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun ProgressModifierButtons(
  hasProgress: Boolean,
  onDiscardProgress: () -> Unit,
  mediaProgress: MediaProgress?,
  onMarkFinished: () -> Unit,
  onMarkNotFinished: () -> Unit,
) {
  val isSupportingContent = LocalContentLayout.current == ContentLayout.Supporting

  val size = ButtonDefaults.ExtraSmallContainerHeight
  val iconSize = ButtonDefaults.iconSizeFor(size)
  val iconSpacing = ButtonDefaults.iconSpacingFor(size)
  val textStyle = ButtonDefaults.textStyleFor(size)
  ButtonDefaults.shapesFor(size)

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val shapes = ButtonDefaults.shapes(
      shape = ButtonDefaults.squareShape,
      pressedShape = ButtonDefaults.shape,
    )

    if (hasProgress) {
      Button(
        onClick = onDiscardProgress,
        shapes = shapes,
        contentPadding = ButtonDefaults.ExtraSmallContentPadding,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.secondary,
          contentColor = MaterialTheme.colorScheme.onSecondary,
        ),
        modifier = Modifier
          .weight(1f)
          .heightIn(size)
          .testTag("button_discard_progress"),
      ) {
        Icon(
          Icons.AutoMirrored.Rounded.Backspace,
          contentDescription = null,
          modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.size(iconSpacing))
        Text(
          text = if (isSupportingContent) {
            stringResource(Res.string.menu_item_discard_progress_short)
          } else {
            stringResource(Res.string.menu_item_discard_progress)
          },
          style = textStyle,
        )
      }
    }

    // Show the mark as (not) finished buttons based on the state
    if (mediaProgress?.isFinished != true) {
      Button(
        onClick = onMarkFinished,
        shapes = shapes,
        contentPadding = ButtonDefaults.ExtraSmallContentPadding,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.secondary,
          contentColor = MaterialTheme.colorScheme.onSecondary,
        ),
        modifier = Modifier
          .weight(1f)
          .heightIn(size)
          .testTag("button_mark_finished"),
      ) {
        Icon(
          Icons.Rounded.MarkFinished,
          contentDescription = null,
          modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.size(iconSpacing))
        Text(
          text = if (isSupportingContent) {
            stringResource(Res.string.menu_item_mark_finished_short)
          } else {
            stringResource(Res.string.menu_item_mark_finished)
          },
          style = textStyle,
        )
      }
    } else {
      FilledTonalButton(
        onClick = onMarkNotFinished,
        shapes = shapes,
        contentPadding = ButtonDefaults.ExtraSmallContentPadding,
        modifier = Modifier
          .weight(1f)
          .heightIn(size)
          .testTag("button_mark_not_finished"),
      ) {
        Icon(
          Icons.Filled.MarkFinished,
          contentDescription = null,
          modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.size(iconSpacing))
        Text(
          text = stringResource(Res.string.menu_item_mark_not_finished),
          style = textStyle,
        )
      }
    }
  }
}

@Composable
private fun PlayAndDownloadButtons(
  offlineDownload: OfflineDownload?,
  onPlayClick: () -> Unit,
  hasProgress: Boolean,
  onDownloadClick: () -> Unit,
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
      targetValue = if (hasOfflineDownload) size / 2 else splitButtonRadius,
    )

    Button(
      onClick = onPlayClick,
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
        if (hasProgress) Icons.Outlined.Autoplay else Icons.Rounded.PlayArrow,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
      Text(
        text = if (hasProgress) {
          stringResource(Res.string.action_resume_listening)
        } else {
          stringResource(Res.string.action_play)
        },
        style = ButtonDefaults.textStyleFor(size),
      )
    }

    Spacer(Modifier.width(2.dp))

    AnimatedVisibility(
      visible = !hasOfflineDownload,
      enter = expandHorizontally(),
      exit = shrinkHorizontally(),
    ) {
      Button(
        onClick = onDownloadClick,
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
        contentPadding = PaddingValues(
          start = 20.dp,
          end = 24.dp,
          top = 16.dp,
          bottom = 16.dp,
        ),
        modifier = Modifier
          .heightIn(size)
          .testTag("button_download"),
      ) {
        // This is a DUMB hack to make it height match the left side button
        Text(
          text = "",
          style = ButtonDefaults.textStyleFor(size),
        )
        Icon(
          CampfireIcons.Rounded.Download,
          contentDescription = "Download",
          modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
        )
      }
    }
  }
}

class ControlSlotProvider : PreviewParameterProvider<ExpressiveControlSlot> {
  override val values: Sequence<ExpressiveControlSlot> = sequenceOf(
    ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = null,
      mediaProgress = null,
      showConfirmDownloadDialogSetting = false,
    ),
    ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = null,
      mediaProgress = mediaProgress(),
      showConfirmDownloadDialogSetting = false,
    ),
    ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = OfflineDownload(
        libraryItemId = "",
        state = OfflineDownload.State.None,
        contentLength = 5L * 1024L * 1024L,
        progress = OfflineDownload.Progress(0L, 0f),
      ),
      mediaProgress = mediaProgress(),
      showConfirmDownloadDialogSetting = false,
    ),
    ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = OfflineDownload(
        libraryItemId = "",
        state = OfflineDownload.State.Downloading,
        contentLength = 5L * 1024L * 1024L,
        progress = OfflineDownload.Progress(
          bytes = 2L * 1024L * 1024L,
          percent = (2f / 5f),
        ),
      ),
      mediaProgress = mediaProgress(),
      showConfirmDownloadDialogSetting = false,
    ),
    ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = OfflineDownload(
        libraryItemId = "",
        state = OfflineDownload.State.Downloading,
        contentLength = 5L * 1024L * 1024L,
        progress = OfflineDownload.Progress(
          bytes = 2L * 1024L * 1024L,
          percent = (2f / 5f),
          indeterminate = true,
        ),
      ),
      mediaProgress = mediaProgress(),
      showConfirmDownloadDialogSetting = false,
    ),
    ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = OfflineDownload(
        libraryItemId = "",
        state = OfflineDownload.State.Completed,
        contentLength = 564L * 1024L * 1024L,
        progress = OfflineDownload.Progress(
          bytes = 2L * 1024L * 1024L,
          percent = (2f / 5f),
        ),
      ),
      mediaProgress = mediaProgress(),
      showConfirmDownloadDialogSetting = false,
    ),
  )
}

@Preview
@Composable
fun ExpressiveControlSlotPreview(
  @PreviewParameter(ControlSlotProvider::class) slot: ExpressiveControlSlot,
) {
  CampfireTheme(
    useDarkColors = false,
    tent = Tent.Red,
  ) {
    CompositionLocalProvider(
      LocalContentLayout provides ContentLayout.Root,
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              vertical = 24.dp,
            ),
        ) {
          slot.Content(Modifier) {}
        }
      }
    }
  }
}

@Preview
@Composable
fun DarkExpressiveControlSlotPreview(
  @PreviewParameter(ControlSlotProvider::class) slot: ExpressiveControlSlot,
) {
  CampfireTheme(
    useDarkColors = true,
  ) {
    CompositionLocalProvider(
      LocalContentLayout provides ContentLayout.Root,
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              vertical = 24.dp,
            ),
        ) {
          slot.Content(Modifier) {}
        }
      }
    }
  }
}
