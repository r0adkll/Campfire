package app.campfire.common.compose.widgets

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode

@Preview
@Composable
fun FilterBarPreview() {
  CampfireTheme {
    Surface {
      FilterBar(
        count = { Text("82 Books") },
        itemDisplayState = ItemDisplayState.List,
        onDisplayStateClick = {},
        isFiltered = false,
        onFilterClick = {},
        sortMode = SortMode.AuthorFL,
        sortDirection = SortDirection.Ascending,
        onSortClick = {},
      )
    }
  }
}
