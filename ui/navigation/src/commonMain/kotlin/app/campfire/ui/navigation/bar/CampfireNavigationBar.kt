package app.campfire.ui.navigation.bar

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import app.campfire.core.logging.bark
import app.campfire.core.reflect.instanceOf
import app.campfire.ui.navigation.HomeNavigationItem
import app.campfire.ui.navigation.HomeNavigationItemIcon
import app.campfire.ui.navigation.buildNavigationItems
import com.slack.circuit.runtime.screen.Screen
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

@Stable
class CampfireNavigationBarState(
  initialOffset: Float,
  initialOffsetLimit: Float,
  private val scope: CoroutineScope,
) {
  private var scrollBehavior by mutableStateOf<SearchBarScrollBehavior?>(null)
  var scrollOffsetLimit by mutableFloatStateOf(initialOffsetLimit)

  private var _scrollOffset by mutableFloatStateOf(initialOffset)

  @get:FrequentlyChangingValue
  val scrollOffset by derivedStateOf {
    if (!shouldHide && !isTransitioning) {
      scrollBehavior?.scrollOffset ?: _scrollOffset
    } else {
      _scrollOffset
    }
  }

  /**
   * Whether or not we should hide the navigation bar
   */
  private var shouldHide by mutableStateOf(false)
  private var isTransitioning by mutableStateOf(false)

  private var transitionJob: Job? = null

  suspend fun updateShouldHide(shouldHide: Boolean) {
    bark("NavBar") { "updateShouldHide($shouldHide)" }
    val currentScrollOffset = scrollOffset
    this@CampfireNavigationBarState.shouldHide = shouldHide

    transitionJob = scope.launch {
      if (shouldHide) {
        AnimationState(currentScrollOffset, initialVelocity = 0f).animateTo(
          targetValue = -scrollOffsetLimit,
          animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) {
//          bark("NavBar") { "shouldHide: $value" }
          _scrollOffset = value
          isTransitioning = isRunning
        }
      } else if (!shouldHide) {
        AnimationState(_scrollOffset, initialVelocity = 0f).animateTo(
          targetValue = scrollBehavior?.scrollOffset ?: 0f,
          animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) {
//          bark("NavBar") { "shouldNOTHide: $value" }
          _scrollOffset = value
          isTransitioning = isRunning
        }
      }
    }
  }

  /**
   * Computes the offset for the PlaybackBar in the UI
   */
  fun playbackBarOffset(bottomSystemInset: Float): Float {
    val computeInternalOffset: () -> Float = {
      val scrollOffset = (-_scrollOffset)
        .coerceIn(0f, scrollOffsetLimit)

      val remainingHeight = scrollOffsetLimit - scrollOffset
      remainingHeight.coerceAtLeast(bottomSystemInset)
    }

    if (isTransitioning) {
      return computeInternalOffset()
    }

    return scrollBehavior?.let { behavior ->
      val scrollOffset = (-behavior.scrollOffset)
        .coerceIn(0f, scrollOffsetLimit)

      val remainingHeight = scrollOffsetLimit - scrollOffset
      remainingHeight.coerceAtLeast(bottomSystemInset)
    } ?: computeInternalOffset()
  }

  fun installScrollBehavior(behavior: SearchBarScrollBehavior) {
    bark("NavBar") { "installScrollBehavior($behavior)" }
    scrollBehavior = behavior
    if (
      isTransitioning &&
      behavior.scrollOffset < 0f &&
      transitionJob?.isActive == true
    ) {
      transitionJob?.cancel()
      isTransitioning = false
    } else if (
      behavior.scrollOffset != _scrollOffset &&
      !shouldHide
    ) {
      transitionJob = scope.launch {
        AnimationState(_scrollOffset, initialVelocity = 0f).animateTo(
          targetValue = behavior.scrollOffset,
          animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) {
//          bark("NavBar") { "newScrollBehaviorTransition: $value" }
          _scrollOffset = value
          isTransitioning = isRunning
        }
      }
    }
  }

  fun clearScrollBehavior(behavior: SearchBarScrollBehavior) {
    bark("NavBar") { "clearScrollBehavior($behavior)" }
    if (scrollBehavior === behavior) {
      scrollBehavior = null
    }
  }
}

@Composable
fun rememberCampfireNavigationBarState(): CampfireNavigationBarState {
  val scope = rememberCoroutineScope()
  return remember {
    CampfireNavigationBarState(
      initialOffset = 0f,
      initialOffsetLimit = 0f,
      scope = scope,
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
