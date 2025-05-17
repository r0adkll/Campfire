package app.campfire.ios.di

import app.campfire.core.di.ComponentHolder
import dev.zacsweers.metro.asContribution
import dev.zacsweers.metro.createGraph

object IosDI {

  fun createApplicationComponent(): IosApplicationComponent {
    return createGraph<IosApplicationComponent>().also {
      ComponentHolder.components += it
    }
  }

  fun createHomeUiControllerComponent(): HomeUiControllerComponent {
    return ComponentHolder.component<IosApplicationComponent>()
      .asContribution<HomeUiControllerComponent.Factory>()
      .create()
      .also {
        ComponentHolder.components += it
      }
  }
}
