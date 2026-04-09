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
import app.campfire.widgets.theme.LocalContentColorProvider
import app.campfire.widgets.theme.withAlpha

@Composable
internal fun WidgetScaffold(
  sizeClass: WidgetSizeClass,
  onClick: Action,
  modifier: GlanceModifier = GlanceModifier,
  defaultBackground: ImageProvider? = null,
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
    if (defaultBackground != null) {
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
      when (sizeClass.height) {
        WidgetHeightClass.Single -> playbackContent()

        WidgetHeightClass.ExtraTall,
        WidgetHeightClass.Tall,
        WidgetHeightClass.Expanded,
        WidgetHeightClass.Compact,
        WidgetHeightClass.LargeCompact,
        -> if (sizeClass.width == WidgetWidthClass.Expanded) {
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
