// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.automotive

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark

/** Most cars have no browser, so a link that nothing can open is ignored rather than crashing. */
internal class CarUriHandler(
  private val activity: Activity,
) : UriHandler {
  override fun openUri(uri: String) {
    try {
      activity.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
    } catch (e: ActivityNotFoundException) {
      bark(LogPriority.WARN, throwable = e) { "Nothing on this car can open $uri" }
    }
  }
}
