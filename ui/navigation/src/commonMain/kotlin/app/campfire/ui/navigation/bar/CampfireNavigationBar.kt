package app.campfire.ui.navigation.bar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import app.campfire.common.compose.extensions.thenIfNotNull
import app.campfire.core.reflect.instanceOf
import app.campfire.ui.navigation.HomeNavigationItem
import app.campfire.ui.navigation.HomeNavigationItemIcon
import app.campfire.ui.navigation.buildNavigationItems
import com.slack.circuit.runtime.screen.Screen
import kotlin.math.roundToInt

@Composable
fun CampfireNavigationBar(
  selectedNavigation: Screen,
  onNavigationSelected: (Screen) -> Unit,
  modifier: Modifier = Modifier,
  state: CampfireNavigationBarState = rememberCampfireNavigationBarState(),
  navigationItems: List<HomeNavigationItem> = buildNavigationItems(),
) {
  NavigationBar(
    modifier = modifier
      .thenIfNotNull(state.scrollBehavior) { behavior ->
        layout { measurable, constraints ->
          val placeable = measurable.measure(constraints)
          val scrollOffset = (-behavior.scrollOffset)
            .coerceIn(0f, state.scrollOffsetLimit)
            .roundToInt()

          layout(placeable.width, placeable.height - scrollOffset) {
            placeable.placeWithLayer(0, 0)
          }
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

@Stable
class CampfireNavigationBarState(
  initialOffsetLimit: Float,
) {
  internal var scrollBehavior by mutableStateOf<SearchBarScrollBehavior?>(null)
  internal var scrollOffsetLimit by mutableFloatStateOf(initialOffsetLimit)

  fun playbackBarOffset(bottomSystemInset: Float): Float {
    return scrollBehavior?.let { behavior ->
      val scrollOffset = (-behavior.scrollOffset)
        .coerceIn(0f, scrollOffsetLimit)

      val remainingHeight = scrollOffsetLimit - scrollOffset
      if (remainingHeight > bottomSystemInset) {
        0f
      } else {
        bottomSystemInset - remainingHeight
      }
    } ?: 0f
  }

  fun installScrollBehavior(behavior: SearchBarScrollBehavior) {
    scrollBehavior = behavior
  }

  fun clearScrollBehavior(behavior: SearchBarScrollBehavior) {
    if (scrollBehavior === behavior) {
      scrollBehavior = null
    }
  }
}

@Composable
fun rememberCampfireNavigationBarState(): CampfireNavigationBarState {
  return remember {
    CampfireNavigationBarState(
      initialOffsetLimit = 0f,
    )
  }
}

val LocalNavigationBarState = compositionLocalOf<CampfireNavigationBarState?> { null }

@Composable
fun AttachScrollBehaviorToLocalNavigationBar(scrollBehavior: SearchBarScrollBehavior) {
  val navigationBarState = LocalNavigationBarState.current

  if (navigationBarState != null) {
    DisposableEffect(Unit) {
      navigationBarState.installScrollBehavior(scrollBehavior)

      onDispose {
        navigationBarState.clearScrollBehavior(scrollBehavior)
      }
    }
  }
}
