// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.di

import androidx.glance.appwidget.action.ActionCallback
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.WidgetMediaCommandSender
import app.campfire.core.di.ComponentHolder
import app.campfire.core.model.Session

abstract class AudioPlayerActionCallback : ActionCallback {

  protected val component: ActionCallbackComponent
    get() = ComponentHolder.component()

  protected val audioPlayer: AudioPlayer? get() {
    return component.audioPlayerHolder.currentPlayer.value
  }

  protected val commandSender: WidgetMediaCommandSender
    get() = component.widgetMediaCommandSender

  protected suspend fun getCurrentSession(): Session? {
    return component.sessionsRepository.getCurrentSession()
  }
}
