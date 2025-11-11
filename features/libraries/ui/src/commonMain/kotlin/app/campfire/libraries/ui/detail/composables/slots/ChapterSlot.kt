package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.extensions.seconds
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.DurationListItem

class ChapterSlot(
  private val libraryItem: LibraryItem,
  private val chapter: Chapter,
  private val showTimeInBook: Boolean,
  private val mediaProgress: MediaProgress?,
) : ContentSlot {

  override val id: String = "chapter_${chapter.id}"
  override val contentType = ContentSlot.ContentType.Chapter

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    val progress = mediaProgress?.let { mediaProgress ->
      (mediaProgress.currentTime.seconds - chapter.start.seconds) / chapter.duration
    }?.toFloat() ?: 0f

    DurationListItem(
      title = chapter.title,
      duration = if (showTimeInBook) {
        chapter.start.seconds
      } else {
        chapter.duration
      },
      progress = progress,
      modifier = Modifier
        .clickable {
          eventSink(LibraryItemUiEvent.ChapterClick(libraryItem, chapter))
        },
    )
  }
}
