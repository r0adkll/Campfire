package app.campfire.common.compose.navigation

import androidx.compose.runtime.compositionLocalOf

val LocalSearchView = compositionLocalOf<SearchViewNavigationState?> { null }

class SearchViewNavigationState(
  private val onNavigate: () -> Unit,
) {

  fun navigateToSearchView() {
    onNavigate()
  }
}
