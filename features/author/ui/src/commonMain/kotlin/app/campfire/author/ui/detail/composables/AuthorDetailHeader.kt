package app.campfire.author.ui.detail.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader

@Composable
internal fun AuthorDetailHeader(
  title: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.height(48.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    MetadataHeader(title)
  }
}
