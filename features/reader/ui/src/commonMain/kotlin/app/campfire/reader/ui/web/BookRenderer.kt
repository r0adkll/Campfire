// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Hosts the reader page in the platform's web view, on platforms where Campfire has one.
 * Everywhere else the [UnsupportedBookRenderer] binding applies, and a platform implementation
 * replaces it.
 */
interface BookRenderer {

  val isSupported: Boolean

  /**
   * The reader page, filling [modifier]'s bounds. It serves every request from [content],
   * evaluates [ReaderBridge.scripts] in order, and hands the page's messages to
   * [ReaderBridge.onMessage].
   */
  @Composable
  fun Content(
    content: ReaderContent,
    bridge: ReaderBridge,
    modifier: Modifier,
  )
}

/** The [BookRenderer] for platforms without one. */
@ContributesBinding(AppScope::class)
@Inject
class UnsupportedBookRenderer : BookRenderer {
  override val isSupported: Boolean get() = false

  @Composable
  override fun Content(
    content: ReaderContent,
    bridge: ReaderBridge,
    modifier: Modifier,
  ) = Unit
}
