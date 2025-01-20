package app.campfire.ui.appbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.navigation.LocalSearchView
import app.campfire.common.compose.navigation.localDrawerOpener
import app.campfire.common.compose.widgets.CampfireAppBar
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

// Injectable typealias
@OptIn(ExperimentalMaterial3Api::class)
typealias CampfireAppBar = @Composable (
  modifier: Modifier,
  scrollBehavior: TopAppBarScrollBehavior?,
) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Inject
@Composable
fun CampfireAppBar(
  presenter: CampfireAppbarPresenter,
  @Assisted modifier: Modifier = Modifier,
  @Assisted scrollBehavior: TopAppBarScrollBehavior?,
) {
  val drawerOpener = localDrawerOpener()
  val searchViewNavigationState = LocalSearchView.current
  val state = presenter.present()
  CampfireAppBar(
    state = state,
    onNavigationClick = drawerOpener,
    onSearchClick = { searchViewNavigationState?.navigateToSearchView() },
    modifier = modifier,
    scrollBehavior = scrollBehavior,
  )
}
