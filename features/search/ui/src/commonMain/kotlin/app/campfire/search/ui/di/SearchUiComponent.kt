package app.campfire.search.ui.di

import app.campfire.core.di.UserScope
import app.campfire.search.ui.SearchPresenter
import dev.zacsweers.metro.ContributesTo

@ContributesTo(UserScope::class)
interface SearchUiComponent {
  val searchPresenterFactory: SearchPresenter.Factory
}
