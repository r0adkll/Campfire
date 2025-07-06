package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import app.campfire.widgets.R
import app.campfire.widgets.theme.LocalContentColorProvider
import app.campfire.widgets.theme.withAlpha

@Composable
internal fun WidgetScaffold(
  sizeClass: WidgetSizeClass,
  artworkUrl: String?,
  onClick: Action,
  modifier: GlanceModifier = GlanceModifier,
  defaultBackground: ImageProvider = ImageProvider(R.drawable.default_background),
  playbackContent: @Composable () -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .clickable(onClick)
      .appWidgetBackground()
      .background(GlanceTheme.colors.background),
    contentAlignment = Alignment.BottomStart,
  ) {
    if (artworkUrl != null) {
      GlanceImage(
        url = artworkUrl,
        modifier = GlanceModifier
          .fillMaxSize(),
      )
    } else {
      Image(
        provider = defaultBackground,
        contentScale = ContentScale.Crop,
        contentDescription = null,
        colorFilter = ColorFilter.tint(
          GlanceTheme.colors.secondary.withAlpha(0.5f),
        ),
        modifier = GlanceModifier.fillMaxSize(),
      )
    }

    val localContentColor = GlanceTheme.colors.onSecondary
    CompositionLocalProvider(
      LocalContentColorProvider provides localContentColor,
    ) {
      when (sizeClass.heightSizeClass) {
        WidgetHeightClass.Single -> playbackContent()

        WidgetHeightClass.Expanded,
        WidgetHeightClass.Compact,
        -> if (sizeClass.widthSizeClass == WidgetWidthClass.Expanded) {
          TwoRowWidget(
            playbackContent = playbackContent,
            content = content,
          )
        } else {
          playbackContent()
        }
      }
    }
  }
}

@Composable
private fun TwoRowWidget(
  modifier: GlanceModifier = GlanceModifier,
  playbackContent: @Composable () -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier.fillMaxSize(),
  ) {
    playbackContent()
    content()
  }
}
