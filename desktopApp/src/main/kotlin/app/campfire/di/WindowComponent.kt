package app.campfire.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UiScope
import app.campfire.shared.root.CampfireContentWithInsets
import com.r0adkll.kimchi.annotations.ContributesSubcomponent

@SingleIn(UiScope::class)
@ContributesSubcomponent(
  scope = UiScope::class,
  parentScope = AppScope::class,
)
interface WindowComponent {
  val campfireContent: CampfireContentWithInsets

  @ContributesSubcomponent.Factory
  interface Factory {
    fun create(): WindowComponent
  }
}
