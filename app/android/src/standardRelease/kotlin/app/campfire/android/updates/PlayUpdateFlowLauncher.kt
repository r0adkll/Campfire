// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.updates

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import app.campfire.core.ComponentActivityPlugin
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import me.tatarka.inject.annotations.Inject

/**
 * Holds the [ActivityResultLauncher] that Google Play's in-app update flow uses to show its
 * update confirmation dialog. Registered against the host activity via the
 * [ComponentActivityPlugin] multibinding so [GooglePlayAppUpdateSource] can start the flow
 * from outside the activity and observe the user's decision through [results].
 */
@SingleIn(AppScope::class)
@Inject
@ContributesMultibinding(AppScope::class, boundType = ComponentActivityPlugin::class)
class PlayUpdateFlowLauncher : ComponentActivityPlugin {

  private val _results = MutableSharedFlow<ActivityResult>(extraBufferCapacity = 1)

  /** Emits the result of each Play update confirmation dialog (declines surface as non-OK). */
  val results: SharedFlow<ActivityResult> = _results

  var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private set

  override fun register(activity: ComponentActivity) {
    launcher = activity.registerForActivityResult(
      ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
      _results.tryEmit(result)
    }
  }

  override fun unregister() {
    launcher?.unregister()
    launcher = null
  }
}
