package app.campfire.ui.appbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.navigation.LocalSearchView
import app.campfire.common.compose.navigation.localDrawerOpener
import app.campfire.common.compose.widgets.CampfireAppBar
import app.campfire.core.di.UserScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject

@ContributesTo(UserScope::class)
interface CampfireAppbarComponent {
  val campfireAppBarFactory: CampfireAppbarFactory
}

@Composable
fun CampfireAppbar(
  modifier: Modifier = Modifier,
  scrollBehavior: TopAppBarScrollBehavior?,
  component: CampfireAppbarComponent = rememberComponent(),
) {
  component.campfireAppBarFactory.Content(
    modifier = modifier,
    scrollBehavior = scrollBehavior,
  )
}

// TODO: The naming here could be better
@Inject
class CampfireAppbarFactory(
  private val presenter: CampfireAppbarPresenter,
) {

  @Composable
  fun Content(
    modifier: Modifier,
    scrollBehavior: TopAppBarScrollBehavior?,
  ) {
    CampfireAppBar(
      presenter = presenter,
      modifier = modifier,
      scrollBehavior = scrollBehavior,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampfireAppBar(
  presenter: CampfireAppbarPresenter,
  modifier: Modifier = Modifier,
  scrollBehavior: TopAppBarScrollBehavior?,
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
