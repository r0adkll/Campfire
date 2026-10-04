// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A [DataStore] held in memory, applying updates one at a time like the real one. */
class InMemoryPreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
  private val state = MutableStateFlow(initial)
  private val mutex = Mutex()

  override val data: StateFlow<Preferences> = state

  override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
    // Store a frozen copy, as DataStore does, so later edits can't reach it
    transform(state.value).toPreferences().also { state.value = it }
  }
}

/**
 * A [SettingsStore] over [dataStore] whose queue runs in [scope]. The default runs each write as it's made.
 */
fun testSettingsStore(
  dataStore: DataStore<Preferences> = InMemoryPreferencesDataStore(),
  scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined + Job()),
) = SettingsStore(dataStore, scope)
