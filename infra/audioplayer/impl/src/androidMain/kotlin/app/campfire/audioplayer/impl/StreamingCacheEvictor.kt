// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

/**
 * Drops everything the streaming cache holds for the given cache keys, so the next read fetches
 * them from the server again. Blocking disk I/O; call it off the main thread.
 */
fun interface StreamingCacheEvictor {
  fun evict(keys: Collection<String>)
}
