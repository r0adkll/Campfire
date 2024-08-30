package app.campfire.common.compose.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.rounded.Lists
import app.campfire.core.settings.ItemDisplayState

private val FilterBarHeight = 56.dp

@Composable
fun FilterBar(
  itemCount: Int,
  itemDisplayState: ItemDisplayState,
  onDisplayStateClick: () -> Unit,
  isFiltered: Boolean,
  onFilterClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CompositionLocalProvider(
    LocalContentColor provides MaterialTheme.colorScheme.onSurface
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
            ItemDisplayState.List -> Icons.Rounded.GridView
            ItemDisplayState.Grid -> Icons.Rounded.Lists
          },
          contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "$itemCount",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }

      Spacer(Modifier.weight(1f))

      IconButton(
        onClick = onFilterClick,
        modifier = Modifier.offset(x = 12.dp)
      ) {
        Icon(
          if (isFiltered) Icons.Rounded.FilterAltOff else Icons.Rounded.FilterAlt,
          contentDescription = null,
        )
      }
    }
  }
}
