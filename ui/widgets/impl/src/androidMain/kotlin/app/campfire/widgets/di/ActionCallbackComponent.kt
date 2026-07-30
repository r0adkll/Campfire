// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.di

import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.OneShotPlaybackController
import app.campfire.audioplayer.WidgetMediaCommandSender
import app.campfire.core.di.UserScope
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.SleepSettings
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(UserScope::class)
interface ActionCallbackComponent {
  val sessionsRepository: SessionsRepository
  val audioPlayerHolder: AudioPlayerHolder
  val oneShotPlaybackController: OneShotPlaybackController
  val widgetMediaCommandSender: WidgetMediaCommandSender
  val sleepSettings: SleepSettings
}
