package app.campfire.ui.navigation.bar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.util.withDensity
import app.campfire.core.reflect.instanceOf
import app.campfire.ui.navigation.HomeNavigationItemIcon
import app.campfire.ui.navigation.NavigationComponent
import com.slack.circuit.runtime.screen.Screen
import kotlin.math.roundToInt

private val PlaybackBarBottomPadding = 8.dp

@Composable
fun CampfireNavigationBar(
  selectedNavigation: Screen,
  onNavigationSelected: (Screen) -> Unit,
  modifier: Modifier = Modifier,
  state: CampfireNavigationBarState = rememberCampfireNavigationBarState(),
  navigationComponent: NavigationComponent = rememberComponent(),
) {
  val bottomBarOffset = withDensity {
    WindowInsets.navigationBars.asPaddingValues()
      .calculateBottomPadding().toPx() +
      PlaybackBarBottomPadding.toPx()
  }

  LaunchedEffect(bottomBarOffset) {
    state.bottomBarOffset = bottomBarOffset
  }

  val presenter = remember(navigationComponent) {
    navigationComponent.navigationPresenterFactory()
  }
  val navigationItems = presenter.present()

  NavigationBar(
    modifier = modifier
      .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val scrollOffset = (-state.scrollOffset)
          .coerceIn(0f, state.scrollOffsetLimit)
          .roundToInt()

        layout(placeable.width, placeable.height - scrollOffset) {
          placeable.placeWithLayer(0, 0)
        }
      }
      .onSizeChanged { size ->
        state.scrollOffsetLimit = size.height.toFloat()
      },
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
