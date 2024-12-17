package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.common.compose.widgets.Tag
import app.campfire.common.compose.widgets.TagGroup
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.genres_title
import org.jetbrains.compose.resources.pluralStringResource

@Composable
internal fun GenreChips(
  genres: List<String>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .padding(horizontal = 16.dp),
  ) {
    MetadataHeader(pluralStringResource(Res.plurals.genres_title, genres.size))
    TagGroup(
      tags = genres.map { Tag(it) },
    )
  }
}
