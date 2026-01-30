package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.IndicatorBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.LoadingCampfireIcon

private val LoaderIndicatorSize = 72.dp
private val IndicatorMaxDistance = LoaderIndicatorSize + 24.dp
private val LoadingIndicatorElevation = 1.dp

expect val isNoisyCampfireLoadingIndicatorEnabled: Boolean

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CampfireLoadingIndicator(
  state: PullToRefreshState,
  isRefreshing: Boolean,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  elevation: Dp = LoadingIndicatorElevation,
  maxDistance: Dp = IndicatorMaxDistance,
) {
  if (isNoisyCampfireLoadingIndicatorEnabled) {
    NoisyCampfireLoadingIndicator(
      state = state,
      isRefreshing = isRefreshing,
      modifier = modifier,
      containerColor = containerColor,
      elevation = elevation,
      maxDistance = maxDistance,
    )
  } else {
    PullToRefreshDefaults.LoadingIndicator(
      state = state,
      isRefreshing = isRefreshing,
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      elevation = elevation,
      maxDistance = maxDistance,
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoisyCampfireLoadingIndicator(
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
    LoadingCampfireIcon(
      size = LoaderIndicatorSize - 16.dp,
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
    )
  }
}
