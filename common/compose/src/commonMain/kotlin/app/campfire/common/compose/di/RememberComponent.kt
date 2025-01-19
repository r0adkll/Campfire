package app.campfire.common.compose.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.campfire.core.di.ComponentHolder

/**
 * Remember a given component on the DI Graph that can be fetched with [ComponentHolder.component]
 */
@Composable
inline fun <reified Component : Any> rememberComponent(): Component {
  return remember { ComponentHolder.component<Component>() }
}
