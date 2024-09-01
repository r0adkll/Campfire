package app.campfire.shared.di

import app.campfire.core.di.ComponentHolder

object IosComponentHolder {

  fun addComponent(component: Any) {
    ComponentHolder.components += component
  }
}
