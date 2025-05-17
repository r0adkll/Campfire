package app.campfire.di

import app.campfire.common.root.CampfireContentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.UiScope
import dev.zacsweers.metro.ContributesGraphExtension
import dev.zacsweers.metro.SingleIn

@SingleIn(UiScope::class)
@ContributesGraphExtension(UiScope::class)
interface WindowComponent {
  val campfireContentProvider: CampfireContentProvider

  @ContributesGraphExtension(AppScope::class)
  interface Factory {
    fun create(): WindowComponent
  }
}
