// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * The single daemon thread a desktop [EngineAudioPlayer][app.campfire.audioplayer.impl.EngineAudioPlayer]
 * confines its state and native engine calls to. The player closes it when stopped.
 */
fun newAudioEngineDispatcher(): CoroutineDispatcher {
  return Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "campfire-audio-engine").apply { isDaemon = true }
  }.asCoroutineDispatcher()
}
