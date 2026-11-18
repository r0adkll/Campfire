// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.automotive.debug

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.os.Bundle
import app.campfire.audioplayer.impl.AudioPlayerService
import app.campfire.core.logging.bark

/**
 * Debug builds only: asks Campfire to play a search the way a voice assistant does, through the
 * platform media session's `playFromSearch`, so voice requests can be tried on an emulator.
 *
 * ```
 * adb shell am broadcast --user 10 -n app.campfire.android/app.campfire.automotive.debug.VoiceSearchReceiver \
 *   --es query "project hail mary"
 * ```
 *
 * An empty or missing query is a bare "play Campfire".
 */
class VoiceSearchReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val query = intent.getStringExtra(EXTRA_QUERY).orEmpty()
    val pending = goAsync()
    val appContext = context.applicationContext
    lateinit var browser: MediaBrowser
    browser = MediaBrowser(
      appContext,
      ComponentName(appContext, AudioPlayerService::class.java),
      object : MediaBrowser.ConnectionCallback() {
        override fun onConnected() {
          bark { "VoiceSearchReceiver: playFromSearch(\"$query\")" }
          MediaController(appContext, browser.sessionToken).transportControls.playFromSearch(query, Bundle())
          browser.disconnect()
          pending.finish()
        }

        override fun onConnectionFailed() {
          bark { "VoiceSearchReceiver: couldn't connect to the media session" }
          pending.finish()
        }
      },
      null,
    )
    browser.connect()
  }

  private companion object {
    const val EXTRA_QUERY = "query"
  }
}
