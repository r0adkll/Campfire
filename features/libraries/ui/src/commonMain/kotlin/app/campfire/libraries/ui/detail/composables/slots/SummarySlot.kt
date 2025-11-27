package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.ItemDescription
import org.jetbrains.compose.ui.tooling.preview.Preview

class SummarySlot(
  private val description: String,
  private val publisher: String? = null,
  private val publishedYear: String? = null,
) : ContentSlot {

  override val id: String = "summary"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Column(
      modifier = modifier,
    ) {
      MetadataHeader(
        title = "Summary",
        textStyle = MaterialTheme.typography.titleLarge,
        textColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
          .heightIn(min = 56.dp)
          .padding(
            horizontal = 16.dp,
          ),
      )
      Spacer(Modifier.height(8.dp))
      ItemDescription(
        description = description,
        publisher = publisher,
        publishedYear = publishedYear,
      )
    }
  }
}

@Preview
@Composable
fun SummarySlotPreview() {
  CampfireTheme {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      Box(
        Modifier
          .heightIn(min = 300.dp)
          .padding(
            vertical = 8.dp,
          ),
      ) {
        SummarySlot(
          "Word ".repeat(100),
        ).Content(Modifier) {}
      }
    }
  }
}
