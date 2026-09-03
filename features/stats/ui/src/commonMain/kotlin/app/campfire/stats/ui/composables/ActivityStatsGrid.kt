// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ChevronRight
import app.campfire.core.model.MediaType
import app.campfire.stats.ui.StatsUiModel.Activity
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.activity_best_day
import campfire.features.stats.ui.generated.resources.activity_best_streak
import campfire.features.stats.ui.generated.resources.activity_books_finished
import campfire.features.stats.ui.generated.resources.activity_books_finished_year
import campfire.features.stats.ui.generated.resources.activity_current_streak
import campfire.features.stats.ui.generated.resources.activity_daily_average
import campfire.features.stats.ui.generated.resources.activity_episodes_finished
import campfire.features.stats.ui.generated.resources.activity_episodes_finished_year
import campfire.features.stats.ui.generated.resources.activity_streak_days
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActivityStatsGrid(
  model: Activity,
  onFinishedThisYearClick: (MediaType) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tiles = buildList {
    add(
      TileSpec(
        icon = Icons.Rounded.LocalFireDepartment,
        accent = StreakAccent,
        value = pluralStringResource(Res.plurals.activity_streak_days, model.currentStreak, model.currentStreak),
        label = stringResource(Res.string.activity_current_streak),
      ),
    )
    add(
      TileSpec(
        icon = Icons.Rounded.EmojiEvents,
        accent = BestStreakAccent,
        value = pluralStringResource(Res.plurals.activity_streak_days, model.bestStreak, model.bestStreak),
        label = stringResource(Res.string.activity_best_streak),
      ),
    )
    add(
      TileSpec(
        icon = Icons.Rounded.AvTimer,
        accent = DailyAverageAccent,
        value = model.dailyAverage.thresholdReadoutFormat(),
        label = stringResource(Res.string.activity_daily_average),
      ),
    )
    add(
      TileSpec(
        icon = Icons.Rounded.Star,
        accent = BestDayAccent,
        value = model.bestDay.thresholdReadoutFormat(),
        label = stringResource(Res.string.activity_best_day),
      ),
    )
    add(
      TileSpec(
        icon = Icons.Rounded.AutoStories,
        accent = BooksAccent,
        value = model.booksFinished.toString(),
        label = stringResource(Res.string.activity_books_finished),
      ),
    )
    add(
      TileSpec(
        icon = Icons.Rounded.Event,
        accent = BooksThisYearAccent,
        value = model.booksFinishedThisYear.toString(),
        label = stringResource(Res.string.activity_books_finished_year),
        onClick = { onFinishedThisYearClick(MediaType.Book) },
      ),
    )
    if (model.hasPodcastActivity) {
      add(
        TileSpec(
          icon = Icons.Rounded.Podcasts,
          accent = EpisodesAccent,
          value = model.episodesFinished.toString(),
          label = stringResource(Res.string.activity_episodes_finished),
        ),
      )
      add(
        TileSpec(
          icon = Icons.Rounded.EventAvailable,
          accent = EpisodesThisYearAccent,
          value = model.episodesFinishedThisYear.toString(),
          label = stringResource(Res.string.activity_episodes_finished_year),
          onClick = { onFinishedThisYearClick(MediaType.Podcast) },
        ),
      )
    }
  }

  FlowRow(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = StatsDefaults.HorizontalPadding,
        vertical = StatsDefaults.VerticalPadding,
      ),
    horizontalArrangement = Arrangement.spacedBy(TileSpacing),
    verticalArrangement = Arrangement.spacedBy(TileSpacing),
    maxItemsInEachRow = 2,
  ) {
    val lastRow = (tiles.lastIndex) / 2
    tiles.forEachIndexed { index, tile ->
      ActivityTile(
        icon = tile.icon,
        accent = tile.accent,
        value = tile.value,
        label = tile.label,
        shape = tileShape(
          isTopRow = index / 2 == 0,
          isBottomRow = index / 2 == lastRow,
          isStart = index % 2 == 0,
        ),
        onClick = tile.onClick,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

private data class TileSpec(
  val icon: ImageVector,
  val accent: Color,
  val value: String,
  val label: String,
  val onClick: (() -> Unit)? = null,
)

private fun tileShape(
  isTopRow: Boolean,
  isBottomRow: Boolean,
  isStart: Boolean,
): Shape = RoundedCornerShape(
  topStart = if (isTopRow && isStart) LargeTileRadius else SmallTileRadius,
  topEnd = if (isTopRow && !isStart) LargeTileRadius else SmallTileRadius,
  bottomStart = if (isBottomRow && isStart) LargeTileRadius else SmallTileRadius,
  bottomEnd = if (isBottomRow && !isStart) LargeTileRadius else SmallTileRadius,
)

@Composable
private fun ActivityTile(
  icon: ImageVector,
  accent: Color,
  value: String,
  label: String,
  shape: Shape,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  if (onClick != null) {
    ElevatedCard(
      onClick = onClick,
      shape = shape,
      modifier = modifier,
    ) {
      ActivityTileContent(
        icon = icon,
        accent = accent,
        value = value,
        label = label,
        clickable = true,
      )
    }
  } else {
    ElevatedCard(
      shape = shape,
      modifier = modifier,
    ) {
      ActivityTileContent(icon, accent, value, label)
    }
  }
}

@Composable
private fun ActivityTileContent(
  icon: ImageVector,
  accent: Color,
  value: String,
  label: String,
  modifier: Modifier = Modifier,
  clickable: Boolean = false,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .wrapContentHeight(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .background(
            color = accent.containerColor,
            shape = CircleShape,
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(18.dp),
        )
      }

      Spacer(Modifier.width(12.dp))

      Column(
        Modifier.weight(1f),
      ) {
        Text(
          text = value,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(Modifier.width(4.dp))
    }

    if (clickable) {
      Icon(
        CampfireIcons.Rounded.ChevronRight,
        contentDescription = "Open",
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(horizontal = 4.dp),
      )
    }
  }
}

private val Color.containerColor: Color
  get() = this.copy(alpha = 0.16f)

private val TileSpacing = 4.dp
private val LargeTileRadius = 12.dp
private val SmallTileRadius = 4.dp

// Mid-tone accents that read against both light and dark card surfaces
private val StreakAccent = Color(0xFFFF7043)
private val BestStreakAccent = Color(0xFFFFB300)
private val DailyAverageAccent = Color(0xFF42A5F5)
private val BestDayAccent = Color(0xFFAB47BC)
private val BooksAccent = Color(0xFF66BB6A)
private val BooksThisYearAccent = Color(0xFF26A69A)
private val EpisodesAccent = Color(0xFF5C6BC0)
private val EpisodesThisYearAccent = Color(0xFFEC407A)
