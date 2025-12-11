package app.campfire.common.compose.icons.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun ThemeIconPreview() {
  val icons = listOf(
    CampfireIcons.Theme.Tent,
    CampfireIcons.Theme.Rucksack,
    CampfireIcons.Theme.Forest,
    CampfireIcons.Theme.WaterBottle,
    CampfireIcons.Theme.LifeFloat,
    CampfireIcons.Theme.Mountain,
  )

  LazyVerticalGrid(
    columns = GridCells.Fixed(3),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.padding(16.dp),
  ) {
    items(icons) { ico ->
      Image(
        ico,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
      )
    }
  }
}
