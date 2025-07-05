package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.campfire.common.compose.extensions.clockFormat
import app.campfire.core.extensions.seconds
import app.campfire.core.model.Chapter
import app.campfire.widgets.R

@Composable
internal fun ChapterListItem(
  chapter: Chapter,
  showTimeInBook: Boolean,
  onClick: Action,
  modifier: GlanceModifier = GlanceModifier,
  current: Boolean = false,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = 16.dp,
        vertical = 8.dp,
      )
      .clickable(onClick),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (current) {
      Image(
        provider = ImageProvider(R.drawable.ic_media_equalizer),
        contentDescription = null,
        modifier = GlanceModifier.size(24.dp),
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondary),
      )
      Spacer(GlanceModifier.width(8.dp))
    }

    Text(
      text = chapter.title,
      style = TextStyle(
        color = GlanceTheme.colors.onSecondary,
        fontSize = 16.sp,
        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
      ),
      modifier = GlanceModifier.defaultWeight(),
      maxLines = 2,
    )

    Spacer(GlanceModifier.width(16.dp))

    Text(
      text = if (showTimeInBook) {
        chapter.start.seconds.clockFormat()
      } else {
        chapter.duration.clockFormat()
      },
      style = TextStyle(
        color = GlanceTheme.colors.onSecondary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
      ),
    )
  }
}
