// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.campfire.core.ComponentActivityPlugin
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * Picks up audio downloads cut short each time the app comes to the foreground, where the
 * download service is allowed to start.
 */
@SingleIn(AppScope::class)
@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ComponentActivityPlugin>())
class DownloadsResumer(
  private val offlineDownloadManager: OfflineDownloadManager,
) : ComponentActivityPlugin, DefaultLifecycleObserver {

  private var activity: ComponentActivity? = null

  override fun register(activity: ComponentActivity) {
    this.activity = activity
    activity.lifecycle.addObserver(this)
  }

  override fun unregister() {
    activity?.lifecycle?.removeObserver(this)
    activity = null
  }

  override fun onStart(owner: LifecycleOwner) {
    offlineDownloadManager.resumeDownloads()
  }
}
