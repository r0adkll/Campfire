package app.campfire.ui.appbar

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.navigation.localDrawerOpener
import app.campfire.search.api.ui.SearchComponent
import app.campfire.ui.theming.api.widgets.ThemeIconContent
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

// Injectable typealias
@OptIn(ExperimentalMaterial3Api::class)
typealias CampfireAppBar = @Composable (
  modifier: Modifier,
  scrollBehavior: SearchBarScrollBehavior?,
) -> Unit

object SharedAppBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Inject
@Composable
fun CampfireAppBar(
  searchComponent: SearchComponent,
  themeIconContent: ThemeIconContent,
  @Assisted modifier: Modifier = Modifier,
  @Assisted scrollBehavior: SearchBarScrollBehavior?,
) = SharedElementTransitionScope {
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)
  if (windowSizeClass.isSupportingPaneEnabled) {
    ExpandedCampfireAppBar(
      searchComponent = searchComponent,
      scrollBehavior = scrollBehavior,
      modifier = modifier
        .sharedElement(
          sharedContentState = rememberSharedContentState(SharedAppBar),
          animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
        ),
    )
  } else {
    CompactCampfireAppBar(
      searchComponent = searchComponent,
      themeIconContent = themeIconContent,
      scrollBehavior = scrollBehavior,
      modifier = modifier
        .sharedElement(
          sharedContentState = rememberSharedContentState(SharedAppBar),
          animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
        ),
    )
  }
}

@Composable
private fun CompactCampfireAppBar(
  searchComponent: SearchComponent,
  themeIconContent: ThemeIconContent,
  modifier: Modifier = Modifier,
  scrollBehavior: SearchBarScrollBehavior? = null,
) {
  val drawerOpener = localDrawerOpener()
  CampfireSearchAppBar(
    searchComponent = searchComponent,
    themeIconContent = themeIconContent,
    onNavigationClick = drawerOpener,
    modifier = modifier,
    scrollBehavior = scrollBehavior,
  )
}

@Composable
private fun ExpandedCampfireAppBar(
  searchComponent: SearchComponent,
  modifier: Modifier = Modifier,
  scrollBehavior: SearchBarScrollBehavior? = null,
) {
  CampfireDockedSearchBar(
    searchComponent = searchComponent,
    scrollBehavior = scrollBehavior,
    modifier = modifier,
  )
}
