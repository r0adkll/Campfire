// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.credentials

import androidx.activity.ComponentActivity
import app.campfire.core.ComponentActivityPlugin
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.lang.ref.WeakReference

/**
 * Credential Manager shows its sheets from an Activity, so this tracks the current one.
 */
@SingleIn(AppScope::class)
@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ComponentActivityPlugin>())
class CredentialActivityHolder : ComponentActivityPlugin {

  private var reference: WeakReference<ComponentActivity>? = null

  val activity: ComponentActivity?
    get() = reference?.get()

  override fun register(activity: ComponentActivity) {
    reference = WeakReference(activity)
  }

  override fun unregister() {
    reference = null
  }
}
