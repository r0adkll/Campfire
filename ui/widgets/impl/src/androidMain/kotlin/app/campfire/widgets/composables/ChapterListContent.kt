package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.fillMaxWidth
import app.campfire.core.model.LibraryItem
import app.campfire.widgets.callbacks.PlayChapterActionCallback

@Composable
internal fun ColumnScope.ChapterListContent(
  item: LibraryItem,
  currentPlayingChapterId: Int,
  showTimeInBook: Boolean,
  modifier: GlanceModifier = GlanceModifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .defaultWeight(),
  ) {
    items(
      items = item.media.chapters,
      itemId = { it.title.hashCode().toLong() },
    ) { chapter ->
      ChapterListItem(
        chapter = chapter,
        current = chapter.id == currentPlayingChapterId,
        showTimeInBook = showTimeInBook,
        onClick = actionRunCallback(
          PlayChapterActionCallback::class.java,
          actionParametersOf(
            PlayChapterActionCallback.KEY_ITEM_ID to item.id,
            PlayChapterActionCallback.KEY_CHAPTER_ID to chapter.id,
          ),
        ),
      )
    }
  }
}
