// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.connect

import app.campfire.auth.api.ConnectLinkInbox
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogRedaction
import app.campfire.core.navigation.ConnectLinkResult
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Keeps only the latest link: opening another one replaces it. */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class InMemoryConnectLinkInbox : ConnectLinkInbox {

  private val deliveredIds = mutableSetOf<Long>()
  private var held: Pair<Long, ConnectLinkResult>? = null

  override fun deliver(id: Long, result: ConnectLinkResult): Boolean {
    if (!deliveredIds.add(id)) return false
    if (result is ConnectLinkResult.Valid) {
      LogRedaction.registerSecret(result.link.clientId)
      LogRedaction.registerSecret(result.link.clientSecret)
    }
    held = id to result
    return true
  }

  override fun get(id: Long): ConnectLinkResult? = held?.takeIf { it.first == id }?.second

  override fun discard(id: Long) {
    if (held?.first == id) held = null
  }
}
