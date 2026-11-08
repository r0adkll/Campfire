// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.automotive

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import app.campfire.audioplayer.impl.AudioPlayerService
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Where tapping Campfire's media session or a notification goes in the car: the car's own media
 * app, showing Campfire. The car app has no screens of its own to open.
 */
@ContributesBinding(AppScope::class)
@Inject
class MediaHostIntentProvider(
  private val application: Application,
) : ActivityIntentProvider {

  override fun provide(): Intent {
    val mediaComponent = ComponentName(application, AudioPlayerService::class.java)
    return Intent(ACTION_MEDIA_TEMPLATE)
      .putExtra(EXTRA_MEDIA_COMPONENT, mediaComponent.flattenToString())
  }

  private companion object {
    // android.car.media.CarMediaIntents, which ships with the car platform rather than the SDK
    const val ACTION_MEDIA_TEMPLATE = "android.car.intent.action.MEDIA_TEMPLATE"
    const val EXTRA_MEDIA_COMPONENT = "android.car.intent.extra.MEDIA_COMPONENT"
  }
}
