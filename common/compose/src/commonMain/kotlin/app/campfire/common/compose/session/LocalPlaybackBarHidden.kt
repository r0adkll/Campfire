// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import app.campfire.common.screens.BaseScreen
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * True inside a screen whose presentation hides the playback bar, such as the theme builder.
 *
 * Like [LocalPlayerDocked], content insets key off this: nothing floats over the bottom of such a
 * screen, so it must not reserve a strip for the bar.
 */
val LocalPlaybackBarHidden = compositionLocalOf { false }

/**
 * Wraps [delegate] so each screen's content knows whether its own presentation hides the
 * playback bar. It's per screen rather than read from the top of the back stack, so a screen
 * animating out keeps its insets while the next one comes in.
 */
class PlaybackBarAwareDecoration(private val delegate: NavDecoration) : NavDecoration {

  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    delegate.DecoratedContent(args, navigator, modifier) { arg ->
      val hidesPlaybackBar = (arg.screen as? BaseScreen)?.presentation?.hidePlaybackBar == true
      CompositionLocalProvider(LocalPlaybackBarHidden provides hidesPlaybackBar) {
        content(arg)
      }
    }
  }
}
