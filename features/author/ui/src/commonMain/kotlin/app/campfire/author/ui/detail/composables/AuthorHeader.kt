package app.campfire.author.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.AuthorCoverImage
import app.campfire.core.model.Author
import campfire.features.author.ui.generated.resources.Res
import campfire.features.author.ui.generated.resources.author_description_header
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AuthorHeader(
  author: Author,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth(),
  ) {
    AuthorCoverImage(
      author = author,
      modifier = Modifier.align(Alignment.CenterHorizontally),
    )

    Spacer(Modifier.height(16.dp))

    AuthorDetailHeader(
      title = stringResource(Res.string.author_description_header),
    )

    author.description?.let { description ->
      AuthorDescription(description)
    }
  }
}
