package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireTopAppBarInsets
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.SupportingContentElevation
import app.campfire.common.compose.theme.PaytoneOneFontFamily

/**
 * A common [TopAppBar] implementation for use across the entire app. This commonizes
 * the title font family and scroll container colors based on [ContentLayout].
 * @see TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampfireTopAppBar(
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  navigationIcon: @Composable () -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
  expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
  windowInsets: WindowInsets = CampfireTopAppBarInsets,
  scrollBehavior: TopAppBarScrollBehavior? = null,
  containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current),
  scrolledContainerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(
    LocalAbsoluteTonalElevation.current + 4.dp,
  ),
) {
  val colors = TopAppBarDefaults.topAppBarColors(
    scrolledContainerColor = scrolledContainerColor,
    containerColor = containerColor,

  )

  TopAppBar(
    title = {
      ProvideTextStyle(
        MaterialTheme.typography.titleLarge.copy(
          fontFamily = PaytoneOneFontFamily,
        ),
      ) {
        title()
      }
    },
    navigationIcon = navigationIcon,
    actions = actions,
    colors = colors,
    scrollBehavior = scrollBehavior,
    expandedHeight = expandedHeight,
    windowInsets = windowInsets,
    modifier = modifier,
  )
}

/**
 * A common [TopAppBar] implementation for use across the entire app. This commonizes
 * the title font family and scroll container colors based on [ContentLayout].
 * @see TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampfireMediumTopAppBar(
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  navigationIcon: @Composable () -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
  windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
  scrollBehavior: TopAppBarScrollBehavior? = null,
) {
  val currentContentLayout = LocalContentLayout.current
  val colors = TopAppBarDefaults.topAppBarColors(
    scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
    containerColor = if (currentContentLayout == ContentLayout.Supporting) {
      MaterialTheme.colorScheme.surfaceColorAtElevation(SupportingContentElevation)
    } else {
      Color.Unspecified
    },
  )

  MediumTopAppBar(
    title = {
      ProvideTextStyle(
        MaterialTheme.typography.titleLarge.copy(
          fontFamily = PaytoneOneFontFamily,
        ),
      ) {
        title()
      }
    },
    navigationIcon = navigationIcon,
    actions = actions,
    colors = colors,
    scrollBehavior = scrollBehavior,
    windowInsets = windowInsets,
    modifier = modifier,
  )
}
