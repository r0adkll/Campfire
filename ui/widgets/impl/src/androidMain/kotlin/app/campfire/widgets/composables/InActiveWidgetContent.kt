package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import app.campfire.audioplayer.AudioPlayer
import app.campfire.widgets.R
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun InActiveWidgetContent(
  title: String,
  subtitle: String,
  onClick: Action,
  widgetSizeClass: WidgetSizeClass,
  modifier: GlanceModifier = GlanceModifier,
) {
  WidgetScaffold(
    sizeClass = widgetSizeClass,
    defaultBackground = if (widgetSizeClass.height != WidgetHeightClass.Single) {
      ImageProvider(R.drawable.default_background_expanded)
    } else {
      ImageProvider(R.drawable.default_background)
    },
    onClick = onClick,
    modifier = modifier,
    playbackContent = {
      if (widgetSizeClass.width == WidgetWidthClass.Expanded) {
        SinglePlaybackContent(
          title = title,
          subtitle = subtitle,
          artworkUrl = null,
          playbackState = AudioPlayer.State.Disabled,
          currentTime = 0.seconds,
          currentDuration = 0.seconds,
          playbackSpeed = 1f,
          sizeClass = widgetSizeClass,
          showPlaybackActions = false,
        )
      }
    },
    content = {
    },
  )
}
