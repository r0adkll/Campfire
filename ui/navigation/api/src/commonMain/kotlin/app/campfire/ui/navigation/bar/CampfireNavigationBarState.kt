// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.bar

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.campfire.common.compose.util.withDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class CampfireNavigationBarState internal constructor(
  initialOffset: Float,
  initialOffsetLimit: Float,
  initialBottomBarOffset: Float,
  initialShouldHide: Boolean,
) {
  private var scrollBehavior by mutableStateOf<SearchBarScrollBehavior?>(null)
  var bottomBarOffset by mutableFloatStateOf(initialBottomBarOffset)
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
  private var shouldHide by mutableStateOf(initialShouldHide)
  private var isTransitioning by mutableStateOf(false)

  private var transitionJob: Job? = null

  fun CoroutineScope.updateShouldHide(shouldHide: Boolean) {
    val currentScrollOffset = scrollOffset
    this@CampfireNavigationBarState.shouldHide = shouldHide

    transitionJob = launch {
      if (shouldHide) {
        AnimationState(currentScrollOffset, initialVelocity = 0f).animateTo(
          targetValue = -scrollOffsetLimit,
          animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) {
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
          _scrollOffset = value
          isTransitioning = isRunning
        }
      }
    }
  }

  /**
   * Computes the offset for the PlaybackBar in the UI
   */
  fun playbackBarOffset(): Float {
    val computeInternalOffset: () -> Float = {
      val scrollOffset = (-_scrollOffset)
        .coerceIn(0f, scrollOffsetLimit)

      val remainingHeight = scrollOffsetLimit - scrollOffset
      remainingHeight.coerceAtLeast(bottomBarOffset)
    }

    if (isTransitioning || shouldHide) {
      return computeInternalOffset()
    }

    return scrollBehavior?.let { behavior ->
      val scrollOffset = (-behavior.scrollOffset)
        .coerceIn(0f, scrollOffsetLimit)

      val remainingHeight = scrollOffsetLimit - scrollOffset
      remainingHeight.coerceAtLeast(bottomBarOffset)
    } ?: computeInternalOffset()
  }

  fun CoroutineScope.installScrollBehavior(behavior: SearchBarScrollBehavior) {
    val hadPreviousBehavior = scrollBehavior != null
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
      !hadPreviousBehavior &&
      !shouldHide
    ) {
      transitionJob = launch {
        AnimationState(_scrollOffset, initialVelocity = 0f).animateTo(
          targetValue = behavior.scrollOffset,
          animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) {
          _scrollOffset = value
          isTransitioning = isRunning
        }
      }
    } else if (hadPreviousBehavior) {
      _scrollOffset = behavior.scrollOffset
    }
  }

  fun clearScrollBehavior(behavior: SearchBarScrollBehavior) {
    if (scrollBehavior === behavior) {
      scrollBehavior = null
    }
  }

  companion object {
    fun Saver(): Saver<CampfireNavigationBarState, *> =
      listSaver(
        save = {
          listOf(
            it._scrollOffset,
            it.scrollOffsetLimit,
            it.bottomBarOffset,
            it.shouldHide,
          )
        },
        restore = {
          CampfireNavigationBarState(
            initialOffset = it[0] as Float,
            initialOffsetLimit = it[1] as Float,
            initialBottomBarOffset = it[2] as Float,
            initialShouldHide = it[3] as Boolean,
          )
        },
      )
  }
}

@Composable
fun rememberCampfireNavigationBarState(): CampfireNavigationBarState {
  return rememberSaveable(saver = CampfireNavigationBarState.Saver()) {
    CampfireNavigationBarState(
      initialOffset = 0f,
      initialOffsetLimit = 0f,
      initialBottomBarOffset = 0f,
      initialShouldHide = false,
    )
  }
}

val LocalNavigationBarState = compositionLocalOf<CampfireNavigationBarState?> { null }

@Composable
fun AttachScrollBehaviorToLocalNavigationBar(scrollBehavior: SearchBarScrollBehavior) {
  val scope = rememberCoroutineScope()
  val navigationBarState = LocalNavigationBarState.current

  if (navigationBarState != null) {
    DisposableEffect(Unit) {
      with(navigationBarState) {
        scope.installScrollBehavior(scrollBehavior)
      }

      onDispose {
        navigationBarState.clearScrollBehavior(scrollBehavior)
      }
    }
  }
}

val CampfireNavigationBarWindowInsets: WindowInsets
  @Composable get() {
    val navigationBarState = LocalNavigationBarState.current
    return if (navigationBarState != null) {
      val playbackBarOffset = navigationBarState.playbackBarOffset()
      WindowInsets(
        bottom = withDensity { playbackBarOffset.toDp() },
      )
    } else {
      WindowInsets()
    }
  }
