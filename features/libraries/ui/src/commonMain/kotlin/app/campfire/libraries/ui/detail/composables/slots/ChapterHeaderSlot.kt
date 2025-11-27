package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
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
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.BookRibbon
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.header_chapters
import org.jetbrains.compose.resources.stringResource

internal val ChapterContainerColor
  @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

class ChapterHeaderSlot(
  @get:VisibleForTesting
  val showTimeInBook: Boolean,
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
      MetadataHeader(
        title = stringResource(Res.string.header_chapters),
        textStyle = MaterialTheme.typography.titleLarge,
        textColor = MaterialTheme.colorScheme.contentColorFor(ChapterContainerColor),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .padding(
            horizontal = 24.dp,
            vertical = 8.dp,
          ),
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
    }
  }
}
