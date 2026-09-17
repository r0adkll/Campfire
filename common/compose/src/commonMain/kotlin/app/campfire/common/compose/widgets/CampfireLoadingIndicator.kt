// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.IndicatorBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LoaderIndicatorSize = 72.dp
private val IndicatorMaxDistance = LoaderIndicatorSize + 24.dp
private val LoadingIndicatorElevation = 1.dp

@Composable
fun CampfireLoadingIndicator(
  state: PullToRefreshState,
  isRefreshing: Boolean,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  elevation: Dp = LoadingIndicatorElevation,
  maxDistance: Dp = IndicatorMaxDistance,
) {
  IndicatorBox(
    modifier = modifier.size(LoaderIndicatorSize),
    state = state,
    isRefreshing = isRefreshing,
    containerColor = containerColor,
    elevation = elevation,
    maxDistance = maxDistance,
  ) {
    CampfireFlame(
      progress = { state.distanceFraction },
      isBurning = isRefreshing,
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp),
    )
  }
}
