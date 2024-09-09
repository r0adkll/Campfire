package app.campfire.author.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.AuthorCoverImage
import app.campfire.core.model.Author

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
      author = author
    )

    Spacer(Modifier.height(16.dp))

    author.description?.let { description ->
      AuthorDescription(description)
      Spacer(Modifier.height(16.dp))
    }
  }
}
