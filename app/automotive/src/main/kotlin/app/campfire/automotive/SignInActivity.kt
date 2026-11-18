// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.automotive

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import app.campfire.audioplayer.impl.browse.BrowseSignInPrompt
import app.campfire.audioplayer.impl.browse.BrowseSignInPromptProvider
import app.campfire.audioplayer.impl.browse.PhoneBrowseSignInPromptProvider
import app.campfire.common.root.SignInContent
import app.campfire.core.ComponentActivityPlugin
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject

/**
 * Signs in to Campfire on the car's screen. The car opens it from the "Sign in" button it shows
 * while nobody is signed in, and only lets it run while parked. It finishes back to the car's
 * media app once someone is signed in, or when the user backs out.
 */
class SignInActivity : ComponentActivity() {

  private val component by lazy { ComponentHolder.component<SignInComponent>() }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    component.componentActivityPlugins.forEach { it.register(this) }

    setContent {
      CompositionLocalProvider(
        LocalUriHandler provides CarUriHandler(this),
      ) {
        component.signInContent(
          onSignedIn = ::finish,
          onClose = ::finish,
        )
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    component.componentActivityPlugins.forEach { it.unregister() }
  }
}

@ContributesTo(AppScope::class)
interface SignInComponent {
  val signInContent: SignInContent
  val componentActivityPlugins: Set<ComponentActivityPlugin>
}

/** The car can sign in on its own screen, so its sign-in prompt has a button that opens [SignInActivity]. */
@ContributesBinding(AppScope::class, replaces = [PhoneBrowseSignInPromptProvider::class])
@Inject
class CarBrowseSignInPromptProvider(
  private val application: Application,
) : BrowseSignInPromptProvider {

  override fun prompt(): BrowseSignInPrompt = BrowseSignInPrompt(
    message = application.getString(R.string.browse_sign_in_message),
    action = BrowseSignInPrompt.Action(
      label = application.getString(R.string.browse_sign_in_action),
      intent = PendingIntent.getActivity(
        application,
        SIGN_IN_REQUEST_CODE,
        Intent(application, SignInActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      ),
    ),
  )

  private companion object {
    const val SIGN_IN_REQUEST_CODE = 760
  }
}
