// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.Home
import app.campfire.common.compose.icons.rounded.ArrowBack
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.LibraryBooks
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.QueryStats
import app.campfire.common.compose.icons.rounded.Search
import app.campfire.common.compose.icons.rounded.SkipNext
import app.campfire.common.compose.icons.rounded.SkipPrevious
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_preview_book_chapters
import campfire.ui.theming.ui.generated.resources.theme_preview_book_duration
import campfire.ui.theming.ui.generated.resources.theme_preview_byline
import campfire.ui.theming.ui.generated.resources.theme_preview_cancel
import campfire.ui.theming.ui.generated.resources.theme_preview_chapter
import campfire.ui.theming.ui.generated.resources.theme_preview_chapter_of
import campfire.ui.theming.ui.generated.resources.theme_preview_chapter_playing
import campfire.ui.theming.ui.generated.resources.theme_preview_continue
import campfire.ui.theming.ui.generated.resources.theme_preview_continue_listening
import campfire.ui.theming.ui.generated.resources.theme_preview_download
import campfire.ui.theming.ui.generated.resources.theme_preview_finished
import campfire.ui.theming.ui.generated.resources.theme_preview_genre_classics
import campfire.ui.theming.ui.generated.resources.theme_preview_genre_romance
import campfire.ui.theming.ui.generated.resources.theme_preview_marked_finished
import campfire.ui.theming.ui.generated.resources.theme_preview_mini_player_progress
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_library
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_search
import campfire.ui.theming.ui.generated.resources.theme_preview_nav_stats
import campfire.ui.theming.ui.generated.resources.theme_preview_new_count
import campfire.ui.theming.ui.generated.resources.theme_preview_recently_added
import campfire.ui.theming.ui.generated.resources.theme_preview_remove
import campfire.ui.theming.ui.generated.resources.theme_preview_remove_download_body
import campfire.ui.theming.ui.generated.resources.theme_preview_remove_download_title
import campfire.ui.theming.ui.generated.resources.theme_preview_scene_home
import campfire.ui.theming.ui.generated.resources.theme_preview_see_all
import campfire.ui.theming.ui.generated.resources.theme_preview_server_unreachable
import campfire.ui.theming.ui.generated.resources.theme_preview_server_url
import campfire.ui.theming.ui.generated.resources.theme_preview_server_url_error
import campfire.ui.theming.ui.generated.resources.theme_preview_sleep_timer
import campfire.ui.theming.ui.generated.resources.theme_preview_stream_offline
import campfire.ui.theming.ui.generated.resources.theme_preview_undo
import campfire.ui.theming.ui.generated.resources.theme_preview_wifi_only
import org.jetbrains.compose.resources.stringResource

/** A Campfire screen the theme preview can draw. */
enum class PreviewScene {
  Home,
  Book,
  Player,
  EdgeCases,
}

/**
 * Copies of Campfire screens drawn entirely from the surrounding [MaterialTheme] color scheme, for
 * previewing a theme. They're copies rather than the real widgets because those need shared
 * element scopes, image loading and library models.
 *
 * Each scene scrolls inside the space it's given, so give this a bounded height.
 */
@Composable
fun ThemePreview(
  scene: PreviewScene,
  modifier: Modifier = Modifier,
) {
  ThemePreviewContent(scene = scene, targets = ThemeTargets.None, modifier = modifier)
}

@Composable
internal fun ThemePreviewContent(
  scene: PreviewScene,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    when (scene) {
      PreviewScene.Home -> HomeScene(targets)
      PreviewScene.Book -> BookScene(targets)
      PreviewScene.Player -> PlayerScene(targets)
      PreviewScene.EdgeCases -> EdgeCasesScene(targets)
    }
  }
}

@Composable
private fun HomeScene(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Column(modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PreviewTopBar(
        title = stringResource(Res.string.theme_preview_scene_home),
        targets = targets,
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .themeTarget(ContrastCheck.Highlights, targets, cornerRadius = 16.dp)
            .background(colors.primaryContainer, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "D",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onPrimaryContainer,
          )
        }
      }

      SectionHeader(
        text = stringResource(Res.string.theme_preview_continue_listening),
        targets = targets,
      ) {
        Text(
          text = stringResource(Res.string.theme_preview_see_all),
          style = MaterialTheme.typography.labelLarge,
          color = colors.primary,
          modifier = Modifier.themeTarget(ContrastCheck.Links, targets),
        )
      }

      Row(
        modifier = Modifier
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        PreviewBookCard(SampleBook.MobyDick, progress = 0.62f, targets = targets)
        PreviewBookCard(SampleBook.PrideAndPrejudice, progress = 0.24f, targets = targets)
        PreviewBookCard(SampleBook.Dracula, progress = 0.81f, targets = targets)
      }

      SectionHeader(
        text = stringResource(Res.string.theme_preview_recently_added),
        targets = targets,
      ) {
        PreviewBadge(stringResource(Res.string.theme_preview_new_count), targets)
      }

      PreviewMiniPlayer(targets, Modifier.padding(horizontal = 8.dp))
      Spacer(Modifier.height(4.dp))
    }

    PreviewNavigationBar(targets)
  }
}

@Composable
private fun BookScene(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val book = SampleBook.PrideAndPrejudice
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    PreviewTopBar(
      title = null,
      targets = targets,
      navigationIcon = CampfireIcons.Rounded.ArrowBack,
    ) {
      Icon(CampfireIcons.Rounded.Download, contentDescription = null)
    }

    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SampleCover(book, Modifier.size(104.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = book.title,
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.themeTarget(ContrastCheck.ScreenText, targets),
        )
        Text(
          text = stringResource(Res.string.theme_preview_byline, book.author, SampleNarrator),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.padding(top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          PreviewBadge(stringResource(Res.string.theme_preview_book_duration), targets)
          PreviewBadge(stringResource(Res.string.theme_preview_book_chapters), targets)
        }
      }
    }

    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PreviewButton(
        text = stringResource(Res.string.theme_preview_continue),
        icon = CampfireIcons.Rounded.PlayArrow,
        containerColor = colors.primary,
        contentColor = colors.onPrimary,
        check = ContrastCheck.PlayButtons,
        targets = targets,
        modifier = Modifier.weight(1f),
      )
      PreviewButton(
        text = stringResource(Res.string.theme_preview_download),
        icon = CampfireIcons.Rounded.Download,
        containerColor = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
        check = ContrastCheck.SelectedItems,
        targets = targets,
      )
    }

    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PreviewChip(stringResource(Res.string.theme_preview_genre_romance), targets)
      PreviewChip(stringResource(Res.string.theme_preview_genre_classics), targets)
    }

    Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp)) {
      SampleChapters.forEach { (number, duration) ->
        PreviewChapterRow(
          number = number,
          duration = duration,
          playing = number == PlayingChapter,
          targets = targets,
        )
      }
    }
  }
}

@Composable
private fun PlayerScene(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val book = SampleBook.TimeMachine
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    SampleCover(book, Modifier.size(144.dp))
    Text(
      text = book.title,
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.themeTarget(ContrastCheck.ScreenText, targets),
    )
    Text(
      text = book.author,
      style = MaterialTheme.typography.bodyMedium,
      color = colors.onSurfaceVariant,
    )
    Text(
      text = stringResource(Res.string.theme_preview_chapter_of),
      style = MaterialTheme.typography.labelLarge,
      color = colors.onSecondaryContainer,
      modifier = Modifier
        .themeTarget(ContrastCheck.SelectedItems, targets, cornerRadius = 8.dp)
        .background(colors.secondaryContainer, RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp),
    )

    PreviewSeekBar(progress = 0.42f, modifier = Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth()) {
      Text("06:12", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
      Spacer(Modifier.weight(1f))
      Text("−08:31", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
      Icon(CampfireIcons.Rounded.SkipPrevious, contentDescription = null, modifier = Modifier.size(32.dp))
      Box(
        modifier = Modifier
          .size(64.dp)
          .themeTarget(ContrastCheck.PlayButtons, targets, cornerRadius = 22.dp)
          .background(colors.primary, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          CampfireIcons.Rounded.Pause,
          contentDescription = null,
          tint = colors.onPrimary,
          modifier = Modifier.size(32.dp),
        )
      }
      Icon(CampfireIcons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(32.dp))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      PreviewPill("1.25×", targets)
      PreviewPill(stringResource(Res.string.theme_preview_sleep_timer), targets)
    }
  }
}

@Composable
private fun EdgeCasesScene(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(
      modifier = Modifier
        .themeTarget(ContrastCheck.CardText, targets, cornerRadius = 28.dp)
        .background(colors.surfaceContainerHigh, RoundedCornerShape(28.dp))
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(Res.string.theme_preview_remove_download_title),
        style = MaterialTheme.typography.titleLarge,
      )
      Text(
        text = stringResource(Res.string.theme_preview_remove_download_body, SampleBook.Frankenstein.title),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.align(Alignment.End),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Text(
          text = stringResource(Res.string.theme_preview_cancel),
          style = MaterialTheme.typography.labelLarge,
          color = colors.primary,
          modifier = Modifier.themeTarget(ContrastCheck.Links, targets),
        )
        Text(
          text = stringResource(Res.string.theme_preview_remove),
          style = MaterialTheme.typography.labelLarge,
          color = colors.error,
          modifier = Modifier.themeTarget(ContrastCheck.ErrorText, targets),
        )
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .themeTarget(ContrastCheck.ErrorBanners, targets, cornerRadius = 12.dp)
        .background(colors.errorContainer, RoundedCornerShape(12.dp))
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        CampfireIcons.Rounded.Warning,
        contentDescription = null,
        tint = colors.onErrorContainer,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = stringResource(Res.string.theme_preview_server_unreachable),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onErrorContainer,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .themeTarget(ContrastCheck.Borders, targets, cornerRadius = 4.dp),
      ) {
        // The border sits under the label, which masks it like an outlined text field's does.
        Box(
          Modifier
            .matchParentSize()
            .border(1.dp, colors.outline, RoundedCornerShape(4.dp)),
        )
        Text(
          text = SampleServerUrl,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(16.dp),
        )
        Text(
          text = stringResource(Res.string.theme_preview_server_url),
          style = MaterialTheme.typography.bodySmall,
          color = colors.onSurfaceVariant,
          modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = 12.dp, y = (-9).dp)
            .background(colors.surface)
            .padding(horizontal = 4.dp),
        )
      }
      Text(
        text = stringResource(Res.string.theme_preview_server_url_error),
        style = MaterialTheme.typography.bodySmall,
        color = colors.error,
        modifier = Modifier
          .padding(start = 16.dp)
          .themeTarget(ContrastCheck.ErrorText, targets),
      )
    }

    PreviewSwitchRow(stringResource(Res.string.theme_preview_wifi_only), checked = true, targets = targets)
    PreviewSwitchRow(stringResource(Res.string.theme_preview_stream_offline), checked = false, targets = targets)

    // Campfire's success colors are fixed rather than themed, so this shows how they sit on the theme.
    Text(
      text = stringResource(Res.string.theme_preview_finished),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = CampfireTheme.colorScheme.onSuccess,
      modifier = Modifier
        .background(CampfireTheme.colorScheme.success, CircleShape)
        .padding(horizontal = 8.dp, vertical = 2.dp),
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .themeTarget(ContrastCheck.Snackbars, targets, cornerRadius = 4.dp)
        .background(colors.inverseSurface, RoundedCornerShape(4.dp))
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(Res.string.theme_preview_marked_finished),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.inverseOnSurface,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = stringResource(Res.string.theme_preview_undo),
        style = MaterialTheme.typography.labelLarge,
        color = colors.inversePrimary,
        modifier = Modifier.themeTarget(ContrastCheck.SnackbarActions, targets),
      )
    }
  }
}

@Composable
private fun PreviewTopBar(
  title: String?,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
  navigationIcon: ImageVector? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    navigationIcon?.let { Icon(it, contentDescription = null) }
    if (title != null) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        modifier = Modifier.themeTarget(ContrastCheck.ScreenText, targets),
      )
    }
    Spacer(Modifier.weight(1f))
    actions()
  }
}

@Composable
private fun SectionHeader(
  text: String,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
  trailing: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.themeTarget(ContrastCheck.ScreenText, targets),
    )
    Spacer(Modifier.weight(1f))
    trailing()
  }
}

@Composable
private fun PreviewBookCard(
  book: SampleBook,
  progress: Float,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .width(112.dp)
      .themeTarget(ContrastCheck.CardText, targets, cornerRadius = 16.dp)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
      .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    SampleCover(book, Modifier.fillMaxWidth())
    Spacer(Modifier.height(4.dp))
    Text(
      text = book.title,
      style = MaterialTheme.typography.labelLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = book.author,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { progress },
      color = MaterialTheme.colorScheme.primary,
      trackColor = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier
        .fillMaxWidth()
        .themeTarget(ContrastCheck.ProgressBars, targets, cornerRadius = 2.dp),
    )
  }
}

@Composable
private fun PreviewBadge(
  text: String,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onTertiaryContainer,
    modifier = modifier
      .themeTarget(ContrastCheck.Badges, targets, cornerRadius = 12.dp)
      .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
      .padding(horizontal = 8.dp, vertical = 2.dp),
  )
}

@Composable
private fun PreviewMiniPlayer(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(16.dp)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .themeTarget(ContrastCheck.MiniPlayer, targets, cornerRadius = 16.dp)
      .clip(shape)
      .background(colors.surfaceContainerHighest),
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      SampleCover(SampleBook.Frankenstein, Modifier.size(40.dp), showTitle = false)
      Column(Modifier.weight(1f)) {
        Text(
          text = SampleBook.Frankenstein.title,
          style = MaterialTheme.typography.labelLarge,
          color = colors.onSurface,
        )
        Text(
          text = stringResource(Res.string.theme_preview_mini_player_progress),
          style = MaterialTheme.typography.bodySmall,
          color = colors.onSurfaceVariant,
        )
      }
      Box(
        modifier = Modifier
          .size(40.dp)
          .themeTarget(ContrastCheck.SelectedItems, targets, cornerRadius = 12.dp)
          .background(colors.secondaryContainer, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(CampfireIcons.Rounded.Pause, contentDescription = null, tint = colors.onSecondaryContainer)
      }
    }
    Box(
      Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth(0.38f)
        .height(3.dp)
        .background(colors.primary),
    )
  }
}

@Composable
private fun PreviewNavigationBar(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .themeTarget(ContrastCheck.NavigationBar, targets)
      .background(MaterialTheme.colorScheme.surfaceContainer)
      .padding(top = 10.dp, bottom = 12.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    PreviewNavItem(
      icon = CampfireIcons.Filled.Home,
      label = stringResource(Res.string.theme_preview_scene_home),
      selected = true,
      targets = targets,
    )
    PreviewNavItem(
      icon = CampfireIcons.Rounded.LibraryBooks,
      label = stringResource(Res.string.theme_preview_nav_library),
      selected = false,
      targets = targets,
    )
    PreviewNavItem(
      icon = CampfireIcons.Rounded.Search,
      label = stringResource(Res.string.theme_preview_nav_search),
      selected = false,
      targets = targets,
    )
    PreviewNavItem(
      icon = CampfireIcons.Rounded.QueryStats,
      label = stringResource(Res.string.theme_preview_nav_stats),
      selected = false,
      targets = targets,
    )
  }
}

@Composable
private fun PreviewNavItem(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier = Modifier
        .size(width = 56.dp, height = 30.dp)
        .then(
          if (selected) {
            Modifier
              .themeTarget(ContrastCheck.SelectedItems, targets, cornerRadius = 15.dp)
              .background(colors.secondaryContainer, CircleShape)
          } else {
            Modifier
          },
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) colors.onSurface else colors.onSurfaceVariant,
    )
  }
}

@Composable
private fun PreviewButton(
  text: String,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  check: ContrastCheck,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .height(40.dp)
      .themeTarget(check, targets, cornerRadius = 20.dp)
      .background(containerColor, CircleShape)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
  ) {
    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun PreviewChip(
  text: String,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier
      .themeTarget(ContrastCheck.Borders, targets, cornerRadius = 8.dp)
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
      .padding(horizontal = 12.dp, vertical = 6.dp),
  )
}

@Composable
private fun PreviewPill(
  text: String,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier
      .themeTarget(ContrastCheck.CardText, targets, cornerRadius = 16.dp)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
      .padding(horizontal = 12.dp, vertical = 6.dp),
  )
}

@Composable
private fun PreviewChapterRow(
  number: Int,
  duration: String,
  playing: Boolean,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = modifier
      .fillMaxWidth()
      .then(
        if (playing) {
          Modifier
            .themeTarget(ContrastCheck.SelectedItems, targets, cornerRadius = 12.dp)
            .background(colors.secondaryContainer, RoundedCornerShape(12.dp))
        } else {
          Modifier
        },
      )
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = if (playing) {
        stringResource(Res.string.theme_preview_chapter_playing, number)
      } else {
        stringResource(Res.string.theme_preview_chapter, number)
      },
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (playing) FontWeight.SemiBold else null,
      color = if (playing) colors.onSecondaryContainer else colors.onSurface,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = duration,
      style = MaterialTheme.typography.bodySmall,
      color = if (playing) colors.onSecondaryContainer else colors.onSurfaceVariant,
    )
  }
  if (!playing) {
    HorizontalDivider(color = colors.outlineVariant)
  }
}

@Composable
private fun PreviewSeekBar(
  progress: Float,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = modifier.height(20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      Modifier
        .weight(progress)
        .height(8.dp)
        .background(colors.primary, CircleShape),
    )
    Box(
      Modifier
        .width(4.dp)
        .fillMaxHeight()
        .background(colors.primary, CircleShape),
    )
    Box(
      Modifier
        .weight(1f - progress)
        .height(8.dp)
        .background(colors.secondaryContainer, CircleShape),
    )
  }
}

@Composable
private fun PreviewSwitchRow(
  label: String,
  checked: Boolean,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.weight(1f),
    )
    Switch(
      checked = checked,
      onCheckedChange = null,
      modifier = Modifier.themeTarget(
        check = if (checked) ContrastCheck.PlayButtons else ContrastCheck.Borders,
        targets = targets,
        cornerRadius = 16.dp,
      ),
    )
  }
}

private const val PlayingChapter = 13

private val SampleChapters = listOf(
  12 to "11:02",
  PlayingChapter to "14:37",
  14 to "09:58",
  15 to "12:20",
)
