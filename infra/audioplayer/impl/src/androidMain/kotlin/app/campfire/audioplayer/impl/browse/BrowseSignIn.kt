// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.browse

import android.app.Application
import android.app.PendingIntent
import android.os.Bundle
import androidx.media3.session.MediaConstants
import androidx.media3.session.SessionError
import app.campfire.core.di.AppScope
import app.campfire.infra.audioplayer.impl.R
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * What a media browser (Android Auto, Android Automotive OS) shows when nobody is signed in to
 * Campfire: a message, and on builds that can sign in on the browser's own screen, a button that
 * opens the sign-in activity.
 */
class BrowseSignInPrompt(
  val message: String,
  val action: Action? = null,
) {
  class Action(
    val label: String,
    val intent: PendingIntent,
  )

  /**
   * The browse error that asks for the prompt. Media3 copies it onto the platform session's
   * playback state for legacy browsers, which is how the car learns to show the button.
   */
  fun toSessionError(): SessionError {
    val extras = Bundle()
    action?.let {
      extras.putString(MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL_COMPAT, it.label)
      extras.putParcelable(MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT_COMPAT, it.intent)
    }
    return SessionError(SessionError.ERROR_SESSION_AUTHENTICATION_EXPIRED, message, extras)
  }
}

fun interface BrowseSignInPromptProvider {
  fun prompt(): BrowseSignInPrompt
}

/**
 * The phone app has no activity it may open on a car's screen, so Android Auto only tells the
 * driver to sign in on the phone.
 */
@ContributesBinding(AppScope::class)
@Inject
class PhoneBrowseSignInPromptProvider(
  private val application: Application,
) : BrowseSignInPromptProvider {
  override fun prompt(): BrowseSignInPrompt = BrowseSignInPrompt(
    message = application.getString(R.string.browse_sign_in_on_phone),
  )
}
