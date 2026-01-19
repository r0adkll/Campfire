package app.campfire.common.compose.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.rounded.Lists
import app.campfire.common.compose.icons.rounded.SortAlphaAsc
import app.campfire.common.compose.icons.rounded.SortAlphaDesc
import app.campfire.common.compose.icons.rounded.SortAsc
import app.campfire.common.compose.icons.rounded.SortDesc
import app.campfire.common.compose.icons.rounded.SortNumericAsc
import app.campfire.common.compose.icons.rounded.SortNumericDesc
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.filter_bar_book_count
import org.jetbrains.compose.resources.pluralStringResource

private val FilterBarHeight = 56.dp

@Composable
fun FilterBar(
  itemCount: Int,
  itemDisplayState: ItemDisplayState,
  onDisplayStateClick: () -> Unit,
  isFiltered: Boolean,
  onFilterClick: () -> Unit,
  sortMode: SortMode,
  sortDirection: SortDirection,
  onSortClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CompositionLocalProvider(
    LocalContentColor provides MaterialTheme.colorScheme.onSurface,
  ) {
    Row(
      modifier = modifier
        .height(FilterBarHeight),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier
          .fillMaxHeight()
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onDisplayStateClick),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          when (itemDisplayState) {
            ItemDisplayState.List -> Icons.Rounded.Lists
            ItemDisplayState.Grid -> Icons.Rounded.Grid3x3
            ItemDisplayState.GridDense -> Icons.Rounded.Grid4x4
          },
          contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = pluralStringResource(Res.plurals.filter_bar_book_count, itemCount, itemCount),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }

      Spacer(Modifier.weight(1f))

      SortIconButton(
        sortMode = sortMode,
        sortDirection = sortDirection,
        onClick = onSortClick,
        modifier = Modifier.offset(x = 12.dp),
      )

      IconButton(
        onClick = onFilterClick,
        modifier = Modifier.offset(x = 12.dp),
      ) {
        Icon(
          if (isFiltered) Icons.Rounded.FilterAlt else Icons.Rounded.FilterAltOff,
          contentDescription = null,
          tint = if (isFiltered) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )
      }
    }
  }
}

@Composable
private fun SortIconButton(
  sortMode: SortMode,
  sortDirection: SortDirection,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sortIcon = SortIcon.forMode(sortMode)
  val icon = sortIcon.forDirection(sortDirection)

  IconButton(
    onClick = onClick,
    modifier = modifier,
  ) {
    Icon(
      icon,
      contentDescription = null,
    )
  }
}

enum class SortIcon(
  private val asc: ImageVector,
  private val desc: ImageVector,
) {
  Normal(
    Icons.Rounded.SortAsc,
    Icons.Rounded.SortDesc,
  ),
  Numeric(
    Icons.Rounded.SortNumericAsc,
    Icons.Rounded.SortNumericDesc,
  ),
  Alphabetical(
    Icons.Rounded.SortAlphaAsc,
    Icons.Rounded.SortAlphaDesc,
  ),
  ;

  fun forDirection(direction: SortDirection): ImageVector = when (direction) {
    SortDirection.Ascending -> asc
    SortDirection.Descending -> desc
  }

  companion object {
    fun forMode(mode: SortMode): SortIcon = when (mode) {
      SortMode.Title -> Alphabetical
      SortMode.AuthorFL -> Alphabetical
      SortMode.AuthorLF -> Alphabetical
      SortMode.PublishYear -> Numeric
      SortMode.AddedAt -> Numeric
      SortMode.Size -> Numeric
      SortMode.Duration -> Numeric
    }
  }
}
