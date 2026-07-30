// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import app.campfire.common.compose.extensions.CampfireTimestampScheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Provides a [UriHandler] that intercepts the [CampfireTimestampScheme] scheme emitted by
 * `linkifyTimestamps()` and forwards the parsed offset to [onSeek]. Other URIs delegate to
 * the parent handler so external links keep opening normally.
 */
@Composable
fun WithTimestampUriHandler(
  onSeek: (Duration) -> Unit,
  content: @Composable () -> Unit,
) {
  val parent = LocalUriHandler.current
  val handler = remember(parent, onSeek) {
    TimestampUriHandler(parent, onSeek)
  }
  CompositionLocalProvider(LocalUriHandler provides handler) {
    content()
  }
}

private class TimestampUriHandler(
  private val delegate: UriHandler,
  private val onSeek: (Duration) -> Unit,
) : UriHandler {
  override fun openUri(uri: String) {
    if (uri.startsWith(CampfireTimestampScheme)) {
      val total = uri.removePrefix(CampfireTimestampScheme).toIntOrNull()
      if (total != null) onSeek(total.seconds)
    } else {
      delegate.openUri(uri)
    }
  }
}
