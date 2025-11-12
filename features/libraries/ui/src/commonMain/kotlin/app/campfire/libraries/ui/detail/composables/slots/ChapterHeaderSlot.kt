package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

class ChapterHeaderSlot(
  @VisibleForTesting
  val showTimeInBook: Boolean,
) : ContentSlot {

  override val id: String = "chapter_header"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Column(
      modifier = modifier,
    ) {
      HorizontalDivider(Modifier.fillMaxWidth())
      MetadataHeader(
        title = stringResource(Res.string.header_chapters),
        modifier = Modifier
          .height(56.dp)
          .padding(
            horizontal = 16.dp,
          ),
        trailingContent = {
          Switch(
            checked = showTimeInBook,
            onCheckedChange = {
              eventSink(LibraryItemUiEvent.TimeInBookChange(it))
            },
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
