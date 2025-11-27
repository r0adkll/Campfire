package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

  private val isFirst = libraryItem.media.chapters.indexOf(chapter) == 0
  private val isLast = libraryItem.media.chapters.run {
    indexOf(chapter) == lastIndex
  }

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    val progress = mediaProgress?.let { mediaProgress ->
      (mediaProgress.currentTime.seconds - chapter.start.seconds) / chapter.duration
    }?.toFloat() ?: 0f

    val topCornerSize = if (isFirst) CornerSize(20.dp) else CornerSize(4.dp)
    val bottomCornerSize = if (isLast) CornerSize(20.dp) else CornerSize(4.dp)
    Column(
      modifier = modifier
        .background(ChapterContainerColor),
    ) {
      Surface(
        modifier = Modifier
          .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(0.dp),
        shape = RoundedCornerShape(
          topStart = topCornerSize,
          topEnd = topCornerSize,
          bottomStart = bottomCornerSize,
          bottomEnd = bottomCornerSize,
        ),
      ) {
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

      if (!isLast) {
        Spacer(Modifier.height(2.dp))
      }
    }
  }
}
