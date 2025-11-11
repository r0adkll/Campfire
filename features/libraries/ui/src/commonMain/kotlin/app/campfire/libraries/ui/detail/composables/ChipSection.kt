package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.common.compose.widgets.Tag
import app.campfire.common.compose.widgets.TagGroup

@Composable
internal fun ChipSection(
  title: String,
  items: List<String>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .padding(horizontal = 16.dp),
  ) {
    MetadataHeader(title)
    TagGroup(
      tags = items.map { Tag(it) },
    )
  }
}
