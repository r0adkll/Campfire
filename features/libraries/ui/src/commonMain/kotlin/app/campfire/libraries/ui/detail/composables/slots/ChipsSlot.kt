package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.ChipSection
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource

class ChipsSlot(
  private val title: ChipsTitle,
  private val chips: List<String>,
) : ContentSlot {

  override val id: String = title.hashCode().toString()
  override val contentType = ContentSlot.ContentType.Chips

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    ChipSection(
      title = pluralStringResource(title.pluralRes, title.quantity),
      items = chips,
      modifier = modifier,
    )
  }
}

@Immutable
data class ChipsTitle(
  val pluralRes: PluralStringResource,
  val quantity: Int,
)
