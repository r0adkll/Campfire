// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.settings.api.SettingsLoader
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * The stored settings. Writes return immediately and are applied in the order they were made;
 * [read] waits for every write made before it, so a read always sees the caller's own writes.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<SettingsLoader>())
@Inject
class SettingsStore(
  private val dataStore: DataStore<Preferences>,
  @ForScope(AppScope::class) scope: CoroutineScope,
) : SettingsLoader {

  private val tasks = Channel<suspend () -> Unit>(Channel.UNLIMITED)

  init {
    scope.launch {
      for (task in tasks) {
        try {
          task()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          // A failed write leaves the stored value as it was; later writes still apply
          bark(LogPriority.ERROR, throwable = e) { "Unable to write settings" }
        }
      }
    }
  }

  /** The stored settings, as of every write made before collecting, then every change to them. */
  val data: Flow<Preferences> = flow {
    enqueue { }
    emitAll(dataStore.data)
  }

  /** Applies [block] after every write made before it, without waiting for it. */
  fun edit(block: (MutablePreferences) -> Unit) {
    tasks.trySend { dataStore.edit(block) }
  }

  /** The stored settings, after every write made before this call. */
  suspend fun read(): Preferences = enqueue { dataStore.data.first() }

  /** Runs [block] on the stored settings, in order with every write, and returns what it stores. */
  suspend fun <T> update(block: (MutablePreferences) -> T): T = enqueue {
    var result: Any? = null
    dataStore.edit { result = block(it) }
    @Suppress("UNCHECKED_CAST")
    result as T
  }

  /** Begins reading the stored settings, and migrating the old ones, without waiting for it. */
  override fun startLoading() {
    tasks.trySend { dataStore.data.first() }
  }

  private suspend fun <T> enqueue(block: suspend () -> T): T {
    val result = CompletableDeferred<T>()
    tasks.send { result.completeWith(runCatching { block() }) }
    return result.await()
  }
}
