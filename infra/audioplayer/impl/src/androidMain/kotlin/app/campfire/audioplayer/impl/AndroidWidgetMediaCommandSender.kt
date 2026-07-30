// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import android.os.Bundle
import androidx.media3.session.SessionCommand
import app.campfire.audioplayer.WidgetMediaCommandSender
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class AndroidWidgetMediaCommandSender(
  private val connector: OneShotMediaControllerConnector,
) : WidgetMediaCommandSender {

  override suspend fun playPause() {
    connector.withController { controller ->
      if (controller.isPlaying) {
        controller.pause()
      } else {
        controller.play()
      }
    }
  }

  override suspend fun seekForward() {
    connector.withController { controller ->
      controller.seekForward()
    }
  }

  override suspend fun seekBackward() {
    connector.withController { controller ->
      controller.seekBack()
    }
  }

  override suspend fun skipToNext() {
    connector.withController { controller ->
      controller.seekToNextMediaItem()
    }
  }

  override suspend fun skipToPrevious() {
    connector.withController { controller ->
      controller.seekToPreviousMediaItem()
    }
  }

  override suspend fun cyclePlaybackSpeed() {
    connector.withController { controller ->
      controller.sendCustomCommand(
        SessionCommand(WidgetSessionCommand.CYCLE_SPEED, Bundle.EMPTY),
        Bundle.EMPTY,
      )
    }
  }

  override suspend fun setSleepTimer(minutes: Int) {
    connector.withController { controller ->
      val args = Bundle().apply {
        putInt(WidgetSessionCommand.ARG_TIMER_MINUTES, minutes)
      }
      controller.sendCustomCommand(
        SessionCommand(WidgetSessionCommand.SET_SLEEP_TIMER, Bundle.EMPTY),
        args,
      )
    }
  }

  override suspend fun clearSleepTimer() {
    connector.withController { controller ->
      controller.sendCustomCommand(
        SessionCommand(WidgetSessionCommand.CLEAR_SLEEP_TIMER, Bundle.EMPTY),
        Bundle.EMPTY,
      )
    }
  }
}
