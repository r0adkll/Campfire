// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
