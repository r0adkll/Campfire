package app.campfire.search.ui.di

import app.campfire.core.di.UserScope
import app.campfire.search.ui.SearchPresenterFactory
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(UserScope::class)
interface SearchUiComponent {
  val searchPresenterFactory: SearchPresenterFactory
}
