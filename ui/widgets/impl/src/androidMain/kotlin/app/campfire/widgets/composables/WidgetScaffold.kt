package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
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
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import app.campfire.core.extensions.fluentIf
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
  expandedPlaybackBackgroundColor: ColorProvider? = GlanceTheme.colors.secondaryContainer,
  expandedPlaybackContentColor: ColorProvider = GlanceTheme.colors.onSecondaryContainer,
  playbackContent: @Composable RowScope.() -> Unit,
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
        WidgetHeightClass.Single -> SingleRowWidget(
          widthSizeClass = sizeClass.widthSizeClass,
          content = playbackContent,
        )

        WidgetHeightClass.Expanded,
        WidgetHeightClass.Compact,
        -> if (sizeClass.widthSizeClass == WidgetWidthClass.Expanded) {
          TwoRowWidget(
            widthSizeClass = sizeClass.widthSizeClass,
            playbackContent = playbackContent,
            backgroundColor = expandedPlaybackBackgroundColor,
            contentColor = expandedPlaybackContentColor,
            content = content,
          )
        } else {
          SingleRowWidget(
            widthSizeClass = sizeClass.widthSizeClass,
            content = playbackContent,
          )
        }
      }
    }
  }
}

@Composable
private fun SingleRowWidget(
  widthSizeClass: WidgetWidthClass,
  modifier: GlanceModifier = GlanceModifier,
  content: @Composable RowScope.() -> Unit,
) {
  Row(
    modifier = modifier
      .fillMaxSize()
      .padding(
        horizontal = if (widthSizeClass == WidgetWidthClass.Expanded) {
          24.dp
        } else {
          8.dp
        },
      ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalAlignment = if (widthSizeClass == WidgetWidthClass.Expanded) {
      Alignment.Start
    } else {
      Alignment.CenterHorizontally
    },
    content = content,
  )
}

@Composable
private fun TwoRowWidget(
  widthSizeClass: WidgetWidthClass,
  modifier: GlanceModifier = GlanceModifier,
  backgroundColor: ColorProvider? = GlanceTheme.colors.secondaryContainer,
  contentColor: ColorProvider = GlanceTheme.colors.onSecondaryContainer,
  playbackContent: @Composable RowScope.() -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier.fillMaxSize(),
  ) {
    Row(
      modifier = modifier
        .height(110.dp)
        .fillMaxWidth()
        .fluentIf(backgroundColor != null) {
          background(backgroundColor!!)
        }
        .padding(
          horizontal = if (widthSizeClass == WidgetWidthClass.Expanded) {
            24.dp
          } else {
            8.dp
          },
        ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalAlignment = if (widthSizeClass == WidgetWidthClass.Expanded) {
        Alignment.Start
      } else {
        Alignment.CenterHorizontally
      },
      content = {
        CompositionLocalProvider(
          LocalContentColorProvider provides contentColor,
        ) {
          playbackContent()
        }
      },
    )

    content()
  }
}
