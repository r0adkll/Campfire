// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.BookRibbon
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.header_audio_tracks
import campfire.features.libraries.ui.generated.resources.header_chapters
import campfire.features.libraries.ui.generated.resources.header_chapters_invalid_message
import org.jetbrains.compose.resources.stringResource

internal val ChapterContainerColor
  @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

class ChapterHeaderSlot(
  @get:VisibleForTesting
  val showTimeInBook: Boolean,
  val validation: LibraryItemValidation = LibraryItemValidation.Success,
  val isAudioTracks: Boolean = false,
) : ContentSlot {

  override val id: String = "chapter_header"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.extraLarge.copy(
        bottomStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
      ),
      color = ChapterContainerColor,
    ) {
      Column {
        Spacer(Modifier.height(8.dp))

        // Header
        MetadataHeader(
          title = if (isAudioTracks) {
            stringResource(Res.string.header_audio_tracks)
          } else {
            stringResource(Res.string.header_chapters)
          },
          textStyle = MaterialTheme.typography.titleLarge,
          textColor = MaterialTheme.colorScheme.contentColorFor(ChapterContainerColor),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .padding(
              horizontal = 24.dp,
            ),
          leadingContent = if (validation is LibraryItemValidation.Error.InvalidChapters) {
            {
              Icon(
                CampfireIcons.Rounded.Warning,
                contentDescription = "Invalid Chapters",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          } else {
            null
          },
          trailingContent = {
            Switch(
              checked = showTimeInBook,
              onCheckedChange = {
                eventSink(LibraryItemUiEvent.TimeInBookChange(it))
              },
              colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                checkedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
              ),
              thumbContent = {
                Icon(
                  if (showTimeInBook) CampfireIcons.Rounded.BookRibbon else Icons.Rounded.Timer,
                  contentDescription = null,
                  modifier = Modifier.size(SwitchDefaults.IconSize),
                )
              },
            )
          },
        )

        // Validation
        if (validation is LibraryItemValidation.Error.InvalidChapters) {
          Spacer(Modifier.height(4.dp))
          Text(
            text = stringResource(Res.string.header_chapters_invalid_message),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(
              horizontal = 24.dp,
            ),
          )
          Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
      }
    }
  }
}

class ChapterHeaderSlotProvider : PreviewParameterProvider<ChapterHeaderSlot> {
  override val values: Sequence<ChapterHeaderSlot> = sequenceOf(
    ChapterHeaderSlot(showTimeInBook = true),
    ChapterHeaderSlot(showTimeInBook = false),
    ChapterHeaderSlot(
      showTimeInBook = true,
      isAudioTracks = true,
    ),
    ChapterHeaderSlot(
      showTimeInBook = true,
      validation = LibraryItemValidation.Error.InvalidChapters(setOf(1)),
    ),
  )
}

@Preview
@Composable
fun ChapterHeaderSlotPreview(
  @PreviewParameter(ChapterHeaderSlotProvider::class) slot: ChapterHeaderSlot,
) {
  CampfireTheme {
    slot.Content(Modifier) {}
  }
}
