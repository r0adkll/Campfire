package app.campfire.ui.appbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.widgets.CampfireAppBar
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

// Injectable typealias
@OptIn(ExperimentalMaterial3Api::class)
typealias CampfireAppBar = @Composable (
  onNavigationClick: () -> Unit,
  onSearchClick: () -> Unit,
  modifier: Modifier,
  scrollBehavior: TopAppBarScrollBehavior?,
) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Inject
@Composable
fun CampfireAppBar(
  presenter: CampfireAppbarPresenter,
  @Assisted onNavigationClick: () -> Unit,
  @Assisted onSearchClick: () -> Unit,
  @Assisted modifier: Modifier = Modifier,
  @Assisted scrollBehavior: TopAppBarScrollBehavior?,
) {
  val state = presenter.present()
  CampfireAppBar(
    state = state,
    onNavigationClick = onNavigationClick,
    onSearchClick = onSearchClick,
    modifier = modifier,
    scrollBehavior = scrollBehavior,
  )
}
