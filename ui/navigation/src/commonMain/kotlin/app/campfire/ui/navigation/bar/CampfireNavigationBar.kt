package app.campfire.ui.navigation.bar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.reflect.instanceOf
import app.campfire.ui.navigation.HomeNavigationItem
import app.campfire.ui.navigation.HomeNavigationItemIcon
import app.campfire.ui.navigation.buildNavigationItems
import com.slack.circuit.runtime.screen.Screen

@Composable
fun CampfireNavigationBar(
  selectedNavigation: Screen,
  onNavigationSelected: (Screen) -> Unit,
  modifier: Modifier = Modifier,
  navigationItems: List<HomeNavigationItem> = buildNavigationItems(),
) {
  NavigationBar(
    modifier = modifier,
    windowInsets = WindowInsets.navigationBars,
  ) {
    for (item in navigationItems) {
      NavigationBarItem(
        icon = {
          HomeNavigationItemIcon(
            item = item,
            selected = item.screen.instanceOf(selectedNavigation::class),
          )
        },
        label = { Text(text = item.label) },
        selected = selectedNavigation == item.screen,
        onClick = { onNavigationSelected(item.screen) },
      )
    }
  }
}
