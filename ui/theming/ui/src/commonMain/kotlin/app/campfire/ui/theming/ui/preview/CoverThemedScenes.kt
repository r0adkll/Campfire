// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ArrowBack
import app.campfire.common.compose.icons.rounded.Bookmarks
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.FatCheck
import app.campfire.common.compose.icons.rounded.Forward10
import app.campfire.common.compose.icons.rounded.KeyboardArrowDown
import app.campfire.common.compose.icons.rounded.LibraryAdd
import app.campfire.common.compose.icons.rounded.List
import app.campfire.common.compose.icons.rounded.Palette
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.PlaylistAdd
import app.campfire.common.compose.icons.rounded.Replay10
import app.campfire.common.compose.icons.rounded.Schedule
import app.campfire.common.compose.icons.rounded.SkipNext
import app.campfire.common.compose.icons.rounded.SkipPrevious
import app.campfire.common.compose.icons.rounded.Speed
import app.campfire.common.compose.icons.rounded.Timer
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.core.model.LibraryItem
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_preview_add_to_playlist
import campfire.ui.theming.ui.generated.resources.theme_preview_by
import campfire.ui.theming.ui.generated.resources.theme_preview_chapter
import campfire.ui.theming.ui.generated.resources.theme_preview_continue_listening_action
import campfire.ui.theming.ui.generated.resources.theme_preview_cover_colors_book_body
import campfire.ui.theming.ui.generated.resources.theme_preview_cover_colors_book_title
import campfire.ui.theming.ui.generated.resources.theme_preview_cover_colors_mini_player
import campfire.ui.theming.ui.generated.resources.theme_preview_cover_colors_player_body
import campfire.ui.theming.ui.generated.resources.theme_preview_cover_colors_player_title
import campfire.ui.theming.ui.generated.resources.theme_preview_cover_colors_settings
import campfire.ui.theming.ui.generated.resources.theme_preview_mark_finished
import campfire.ui.theming.ui.generated.resources.theme_preview_narrated_by
import campfire.ui.theming.ui.generated.resources.theme_preview_percent
import campfire.ui.theming.ui.generated.resources.theme_preview_summary
import campfire.ui.theming.ui.generated.resources.theme_preview_time_left
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.compose.resources.stringResource

/**
 * A book's details as they look when cover theming for them is off: the cover, title and
 * attribution over the play controls, then the summary. A copy of the app's `LibraryItemUi`
 * slots, which need the item's presenter.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BookScene(
  book: LibraryItem,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val metadata = book.media.metadata
  val progress = book.userMediaProgress

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PreviewIcon(CampfireIcons.Rounded.ArrowBack, tint = colors.onSurface)
      Spacer(Modifier.weight(1f))
      PreviewIcon(CampfireIcons.Rounded.LibraryAdd, tint = colors.onSurfaceVariant)
    }

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      CoverImage(
        imageUrl = book.media.coverImageUrl,
        contentDescription = null,
        size = maxWidth * 0.75f,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.padding(vertical = 16.dp),
      )
    }

    Column(
      modifier = Modifier.themeTarget(ContrastCheck.ScreenText, targets),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = metadata.title.orEmpty(),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PaytoneOneFontFamily,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      Spacer(Modifier.height(4.dp))
      Row(
        modifier = Modifier.alpha(0.65f),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(CampfireIcons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          text = book.media.durationInMillis.milliseconds.readoutFormat().trim(),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Spacer(Modifier.height(24.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min)
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Attribution(
          label = stringResource(Res.string.theme_preview_by),
          names = metadata.authorName.orEmpty(),
          modifier = Modifier.weight(1f),
        )
        VerticalDivider(Modifier.height(32.dp))
        Attribution(
          label = stringResource(Res.string.theme_preview_narrated_by),
          names = metadata.narratorName ?: SampleNarrator,
          modifier = Modifier.weight(1f),
        )
      }
    }

    if (progress != null && progress.actualProgress > 0f) {
      Spacer(Modifier.height(32.dp))
      Column(Modifier.padding(horizontal = 20.dp)) {
        LinearWavyProgressIndicator(
          progress = { progress.actualProgress },
          color = colors.primary,
          trackColor = colors.secondaryContainer,
          amplitude = { 0f },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
          val left = progress.duration
            ?.let { (it - progress.currentTime).toDouble().coerceAtLeast(0.0).seconds.readoutFormat().trim() }
            ?: "--"
          Text(
            text = stringResource(Res.string.theme_preview_time_left, left),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = stringResource(Res.string.theme_preview_percent, (progress.actualProgress * 100).roundToInt()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
      Spacer(Modifier.height(8.dp))
    }

    Spacer(Modifier.height(16.dp))
    Surface(
      color = colors.surfaceContainerHighest,
      shape = MaterialTheme.shapes.extraLarge,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          Button(
            onClick = {},
            shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 4.dp, bottomEnd = 4.dp),
            modifier = Modifier
              .weight(1f)
              .height(56.dp)
              .themeTarget(ContrastCheck.Buttons, targets, cornerRadius = 28.dp),
          ) {
            Icon(CampfireIcons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
              text = stringResource(Res.string.theme_preview_continue_listening_action),
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Button(
            onClick = {},
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 28.dp, bottomEnd = 28.dp),
            contentPadding = ButtonDefaults.ContentPadding,
            modifier = Modifier.height(56.dp),
          ) {
            Icon(CampfireIcons.Rounded.Download, contentDescription = null)
          }
        }
        Button(
          onClick = {},
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.secondary,
            contentColor = colors.onSecondary,
          ),
          shape = MaterialTheme.shapes.medium,
          contentPadding = ButtonDefaults.ExtraSmallContentPadding,
          modifier = Modifier.height(32.dp),
        ) {
          Icon(CampfireIcons.Rounded.FatCheck, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(Res.string.theme_preview_mark_finished), style = MaterialTheme.typography.labelLarge)
        }
        Button(
          onClick = {},
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
          ),
          modifier = Modifier
            .height(40.dp)
            .themeTarget(ContrastCheck.SelectedItems, targets, cornerRadius = 20.dp),
        ) {
          Icon(CampfireIcons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(Res.string.theme_preview_add_to_playlist), style = MaterialTheme.typography.labelLarge)
        }
      }
    }

    Spacer(Modifier.height(16.dp))
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .themeTarget(ContrastCheck.ScreenText, targets),
    ) {
      Text(
        text = stringResource(Res.string.theme_preview_summary),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 14.dp),
      )
      Text(
        text = book.media.metadata.description.orEmpty(),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun Attribution(
  label: String,
  names: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text(names, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 2)
  }
}

/**
 * The full-screen player as it looks when cover theming for it is off: the cover, chapter and
 * seek bar over the transport controls and actions, all on the secondary container. A copy of
 * `ExpandedPlaybackBar`, which needs a session.
 */
@Composable
internal fun PlayerScene(
  book: LibraryItem,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val progress = book.userMediaProgress?.actualProgress ?: SamplePlayerProgress

  Surface(
    color = colors.secondaryContainer,
    contentColor = colors.onSecondaryContainer,
    modifier = modifier
      .fillMaxSize()
      .themeTarget(ContrastCheck.SelectedItems, targets),
  ) {
    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PreviewIcon(CampfireIcons.Rounded.KeyboardArrowDown)
      }

      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
      ) {
        CoverImage(
          imageUrl = book.media.coverImageUrl,
          contentDescription = null,
          size = maxWidth.coerceAtMost(280.dp),
          shape = RoundedCornerShape(32.dp),
        )
      }

      Spacer(Modifier.height(16.dp))
      Text(
        text = stringResource(Res.string.theme_preview_chapter, PreviewChapter),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PaytoneOneFontFamily,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
      Text(
        text = book.media.metadata.title.orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 24.dp),
      )

      Spacer(Modifier.height(16.dp))
      PreviewSeekBar(
        progress = progress,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 32.dp),
      ) {
        Text("1:12:08", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text("-23:41", style = MaterialTheme.typography.labelSmall)
      }

      Spacer(Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val transportColors = IconButtonDefaults.filledIconButtonColors(
          containerColor = colors.primary.copy(alpha = 0.25f),
          contentColor = colors.onSurface,
        )
        FilledIconButton(
          onClick = {},
          colors = transportColors,
          shape = MaterialTheme.shapes.extraLarge,
          modifier = Modifier.size(96.dp),
        ) {
          Icon(CampfireIcons.Rounded.Replay10, contentDescription = null, modifier = Modifier.size(32.dp))
        }
        FilledIconButton(
          onClick = {},
          shape = MaterialTheme.shapes.extraLarge,
          modifier = Modifier
            .size(width = 128.dp, height = 96.dp)
            .themeTarget(ContrastCheck.Buttons, targets, cornerRadius = 28.dp),
        ) {
          Icon(CampfireIcons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(40.dp))
        }
        FilledIconButton(
          onClick = {},
          colors = transportColors,
          shape = MaterialTheme.shapes.extraLarge,
          modifier = Modifier.size(96.dp),
        ) {
          Icon(CampfireIcons.Rounded.Forward10, contentDescription = null, modifier = Modifier.size(32.dp))
        }
      }
      Spacer(Modifier.height(2.dp))
      Row(
        modifier = Modifier.width(324.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        listOf(CampfireIcons.Rounded.SkipPrevious, CampfireIcons.Rounded.SkipNext).forEach { icon ->
          FilledIconButton(
            onClick = {},
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = colors.primary.copy(alpha = 0.25f),
              contentColor = colors.onSurface,
            ),
            modifier = Modifier
              .weight(1f)
              .height(40.dp),
          ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
          }
        }
      }

      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(72.dp)
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PreviewIcon(CampfireIcons.Rounded.Bookmarks)
        PreviewIcon(CampfireIcons.Rounded.Speed)
        PreviewIcon(CampfireIcons.Rounded.Timer)
        PreviewIcon(CampfireIcons.Rounded.List)
      }
    }
  }
}

/** The player's seek bar at rest: a thick primary track up to a bar-shaped thumb. */
@Composable
private fun PreviewSeekBar(
  progress: Float,
  modifier: Modifier = Modifier,
) {
  val active = MaterialTheme.colorScheme.primary
  val inactive = MaterialTheme.colorScheme.surfaceContainer
  Canvas(
    modifier
      .fillMaxWidth()
      .height(44.dp),
  ) {
    val track = 16.dp.toPx()
    val gap = 6.dp.toPx()
    val thumbWidth = 4.dp.toPx()
    val radius = CornerRadius(track / 2)
    val top = (size.height - track) / 2
    val thumbX = size.width * progress.coerceIn(0f, 1f)

    drawRoundRect(
      color = active,
      topLeft = Offset(0f, top),
      size = Size((thumbX - gap).coerceAtLeast(0f), track),
      cornerRadius = radius,
    )
    drawRoundRect(
      color = inactive,
      topLeft = Offset(thumbX + gap, top),
      size = Size((size.width - thumbX - gap).coerceAtLeast(0f), track),
      cornerRadius = radius,
    )
    drawRoundRect(
      color = active,
      topLeft = Offset(thumbX - thumbWidth / 2, 0f),
      size = Size(thumbWidth, size.height),
      cornerRadius = CornerRadius(thumbWidth / 2),
    )
  }
}

@Composable
private fun PreviewIcon(
  icon: ImageVector,
  modifier: Modifier = Modifier,
  tint: Color = LocalContentColor.current,
) {
  Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(icon, contentDescription = null, tint = tint)
  }
}

/** Where the preview stands in for a part of the app that takes its colors from book covers. */
internal enum class CoverColorsNoteKind { MiniPlayer, BookDetails, Player }

/**
 * Says that a part of the app uses cover colors, so this theme doesn't reach it, and links to
 * the setting that changes that. It draws in neutral outline colors so it doesn't read as part
 * of the screen it stands in for.
 */
@Composable
internal fun CoverColorsNote(
  kind: CoverColorsNoteKind,
  onSettingsClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  if (kind == CoverColorsNoteKind.MiniPlayer) {
    Row(
      modifier = modifier
        .fillMaxWidth()
        .height(56.dp)
        // Opaque, as the mini player it stands in for floats over the shelves
        .background(colors.surface, RoundedCornerShape(12.dp))
        .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
        .padding(start = 16.dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        CampfireIcons.Rounded.Palette,
        contentDescription = null,
        tint = colors.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Spacer(Modifier.width(12.dp))
      Text(
        text = stringResource(Res.string.theme_preview_cover_colors_mini_player),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        maxLines = 2,
        modifier = Modifier.weight(1f),
      )
      if (onSettingsClick != null) {
        TextButton(onClick = onSettingsClick) {
          Text(stringResource(Res.string.theme_preview_cover_colors_settings))
        }
      }
    }
    return
  }

  val isBook = kind == CoverColorsNoteKind.BookDetails
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
      CampfireIcons.Rounded.Palette,
      contentDescription = null,
      tint = colors.onSurfaceVariant,
      modifier = Modifier.size(40.dp),
    )
    Text(
      text = stringResource(
        if (isBook) BookTitle else PlayerTitle,
      ),
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(
        if (isBook) BookBody else PlayerBody,
      ),
      style = MaterialTheme.typography.bodyMedium,
      color = colors.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (onSettingsClick != null) {
      TextButton(onClick = onSettingsClick) {
        Text(stringResource(Res.string.theme_preview_cover_colors_settings))
      }
    }
  }
}

private const val SamplePlayerProgress = 0.42f

/** The chapter the preview's mini player and player are on. */
internal const val PreviewChapter = 5

private val BookTitle = Res.string.theme_preview_cover_colors_book_title
private val BookBody = Res.string.theme_preview_cover_colors_book_body
private val PlayerTitle = Res.string.theme_preview_cover_colors_player_title
private val PlayerBody = Res.string.theme_preview_cover_colors_player_body
