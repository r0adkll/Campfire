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
  presenter: CampfireAppbarPresenter,
  searchComponent: SearchComponent,
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
      presenter = presenter,
      searchComponent = searchComponent,
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
  presenter: CampfireAppbarPresenter,
  searchComponent: SearchComponent,
  modifier: Modifier = Modifier,
  scrollBehavior: SearchBarScrollBehavior? = null,
) {
  val drawerOpener = localDrawerOpener()
  val state = presenter.present()
  CampfireSearchAppBar(
    state = state,
    searchComponent = searchComponent,
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
