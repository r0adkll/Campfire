package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.ui.detail.LibraryItemUiEvent

class PublishedSlot(
  private val publisher: String,
  private val publishedYear: String?,
) : ContentSlot {

  override val id: String = "published"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Column(
      modifier = modifier,
    ) {
      MetadataHeader(
        title = "Published by",
        modifier = Modifier.padding(
          horizontal = 16.dp,
        ),
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
            append(publisher)
          }
          if (publishedYear != null) {
            append(" in ")
            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
              append(publishedYear)
            }
          }
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
          .fillMaxWidth()
          .padding(
            horizontal = 16.dp,
          ),
      )
    }
  }
}
