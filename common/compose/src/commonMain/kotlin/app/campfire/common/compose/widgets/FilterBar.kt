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
import androidx.compose.material3.ProvideTextStyle
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
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortDisplayMode

private val FilterBarHeight = 56.dp

@Composable
fun FilterBar(
  count: @Composable () -> Unit,
  itemDisplayState: ItemDisplayState,
  sortMode: ContentSortMode,
  sortDirection: SortDirection,
  onSortClick: () -> Unit,
  modifier: Modifier = Modifier,
  onDisplayStateClick: (() -> Unit)? = null,
  isFiltered: Boolean = false,
  onFilterClick: (() -> Unit)? = null,
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
          .clickable(
            enabled = onDisplayStateClick != null,
            onClick = {
              onDisplayStateClick?.invoke()
            },
          ),
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
        ProvideTextStyle(
          MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
          ),
        ) {
          count()
        }
      }

      Spacer(Modifier.weight(1f))

      SortIconButton(
        sortMode = sortMode,
        sortDirection = sortDirection,
        onClick = onSortClick,
        modifier = Modifier.offset(x = 12.dp),
      )

      if (onFilterClick != null) {
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
}

@Composable
private fun SortIconButton(
  sortMode: SortDisplayMode,
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
    Icons.Rounded.SortNumericDesc,
    Icons.Rounded.SortNumericAsc,
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
    fun forMode(displayMode: SortDisplayMode): SortIcon = when (displayMode.mode) {
      SortDisplayMode.Mode.Alphabetical -> Alphabetical
      SortDisplayMode.Mode.Numerical -> Numeric
      SortDisplayMode.Mode.Normal -> Normal
    }
  }
}
