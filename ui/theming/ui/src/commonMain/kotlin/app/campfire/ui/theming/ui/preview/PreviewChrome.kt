// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.Author
import app.campfire.common.compose.icons.filled.Home
import app.campfire.common.compose.icons.filled.Library
import app.campfire.common.compose.icons.filled.Playlists
import app.campfire.common.compose.icons.filled.Series
import app.campfire.common.compose.icons.outline.Author
import app.campfire.common.compose.icons.outline.Download
import app.campfire.common.compose.icons.outline.FormatListBulleted
import app.campfire.common.compose.icons.outline.Home
import app.campfire.common.compose.icons.outline.Library
import app.campfire.common.compose.icons.outline.Playlists
import app.campfire.common.compose.icons.outline.Podcasts
import app.campfire.common.compose.icons.outline.Series
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.Replay10
import app.campfire.common.compose.icons.rounded.Search
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_preview_chapter
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_authors
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_home
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_latest
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_library
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_playlists
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_queue
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_series
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_shows
import campfire.ui.theming.ui.generated.resources.theme_preview_search
import campfire.ui.theming.ui.generated.resources.theme_preview_time_left
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The search bar along the top of Home and Library: the theme's icon, which opens the drawer,
 * beside the search field. A copy of `CampfireSearchAppBar`, which needs the search component.
 */
@Composable
internal fun PreviewAppBar(
  icon: AppTheme.Icon,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      imageVector = icon.icon(),
      contentDescription = null,
      modifier = Modifier
        .padding(horizontal = 4.dp)
        .size(40.dp)
        .padding(4.dp)
        .clip(RoundedCornerShape(8.dp)),
    )
    Spacer(Modifier.width(4.dp))
    Row(
      modifier = Modifier
        .weight(1f)
        .height(56.dp)
        .themeTarget(ContrastCheck.SupportingText, targets, cornerRadius = 28.dp)
        .background(colors.surfaceContainerHigh, CircleShape)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(CampfireIcons.Rounded.Search, contentDescription = null, tint = colors.onSurface)
      Text(
        text = stringResource(Res.string.theme_preview_search),
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/** Which tab the preview's navigation bar has selected. */
internal enum class PreviewTab { Home, Library }

/** The navigation bar under Home and Library, with the tabs of a book or podcast library. */
@Composable
internal fun PreviewNavigationBar(
  selected: PreviewTab,
  isPodcastLibrary: Boolean,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val items = if (isPodcastLibrary) PodcastTabs else BookTabs
  NavigationBar(
    modifier = modifier.themeTarget(ContrastCheck.NavigationBar, targets),
    windowInsets = WindowInsets(0),
  ) {
    items.forEach { item ->
      val isSelected = item.tab == selected
      NavigationBarItem(
        selected = isSelected,
        onClick = {},
        icon = {
          Icon(
            imageVector = if (isSelected) item.selectedIcon else item.icon,
            contentDescription = null,
            modifier = if (isSelected) {
              Modifier.themeTarget(ContrastCheck.SelectedItems, targets, cornerRadius = 16.dp)
            } else {
              Modifier
            },
          )
        },
        label = {
          Text(
            text = stringResource(item.label),
            maxLines = 1,
            modifier = if (isSelected) Modifier.themeTarget(ContrastCheck.SelectedTabName, targets) else Modifier,
          )
        },
      )
    }
  }
}

private class PreviewNavItem(
  val label: StringResource,
  val icon: ImageVector,
  val selectedIcon: ImageVector = icon,
  val tab: PreviewTab? = null,
)

private val BookTabs = listOf(
  PreviewNavItem(
    Res.string.theme_preview_nav_home,
    CampfireIcons.Outline.Home,
    CampfireIcons.Filled.Home,
    PreviewTab.Home,
  ),
  PreviewNavItem(
    Res.string.theme_preview_nav_library,
    CampfireIcons.Outline.Library,
    CampfireIcons.Filled.Library,
    PreviewTab.Library,
  ),
  PreviewNavItem(Res.string.theme_preview_nav_series, CampfireIcons.Outline.Series, CampfireIcons.Filled.Series),
  PreviewNavItem(Res.string.theme_preview_nav_authors, CampfireIcons.Outline.Author, CampfireIcons.Filled.Author),
  PreviewNavItem(
    Res.string.theme_preview_nav_playlists,
    CampfireIcons.Outline.Playlists,
    CampfireIcons.Filled.Playlists,
  ),
)

private val PodcastTabs = listOf(
  PreviewNavItem(
    Res.string.theme_preview_nav_home,
    CampfireIcons.Outline.Home,
    CampfireIcons.Filled.Home,
    PreviewTab.Home,
  ),
  PreviewNavItem(Res.string.theme_preview_nav_latest, CampfireIcons.Outline.FormatListBulleted),
  PreviewNavItem(Res.string.theme_preview_nav_shows, CampfireIcons.Outline.Podcasts, tab = PreviewTab.Library),
  PreviewNavItem(Res.string.theme_preview_nav_queue, CampfireIcons.Outline.Download),
  PreviewNavItem(
    Res.string.theme_preview_nav_playlists,
    CampfireIcons.Outline.Playlists,
    CampfireIcons.Filled.Playlists,
  ),
)

/**
 * The playback bar floating over Home and Library, in the theme's colors as it is when cover
 * theming for the player is off. A copy of `CollapsedPlaybackBar`, which needs a session.
 */
@Composable
internal fun PreviewMiniPlayer(
  nowPlaying: PreviewShelfItem,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val item = nowPlaying.item
  // The app titles the bar with the chapter playing, as the player does
  val title = nowPlaying.episode?.title ?: stringResource(Res.string.theme_preview_chapter, PreviewChapter)
  val progress = item.userMediaProgress
  val timeLeft = progress?.duration
    ?.let { duration -> (duration - progress.currentTime).toDouble().coerceAtLeast(0.0).seconds }
    ?.readoutFormat()?.trim()
    ?: "--"

  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shape = RoundedCornerShape(12.dp),
    shadowElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .themeTarget(ContrastCheck.SelectedItems, targets, cornerRadius = 12.dp),
  ) {
    Box {
      Row(verticalAlignment = Alignment.CenterVertically) {
        CoverImage(
          imageUrl = item.media.coverImageUrl,
          contentDescription = null,
          size = 56.dp,
          shape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = PaytoneOneFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = stringResource(Res.string.theme_preview_time_left, timeLeft),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.alpha(0.75f),
          )
        }
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
          Icon(CampfireIcons.Rounded.Replay10, contentDescription = null)
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
          Icon(CampfireIcons.Rounded.Pause, contentDescription = null)
        }
        Spacer(Modifier.width(16.dp))
      }
      LinearProgressIndicator(
        progress = { progress?.actualProgress ?: 0f },
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(horizontal = 12.dp)
          .height(2.dp)
          .fillMaxWidth(),
      )
    }
  }
}
