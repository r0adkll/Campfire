// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.layout

import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.home.api.HomeLayoutSettings
import app.campfire.home.api.model.HomeLayoutEntry
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultHomeLayoutSettings(
  private val settings: ObservableSettings,
  private val userSession: UserSession,
) : HomeLayoutSettings {

  override fun observeLayout(libraryId: LibraryId): Flow<List<HomeLayoutEntry>?> {
    return settings.getStringOrNullFlow(key(libraryId))
      .distinctUntilChanged()
      .map { it?.decodeLayout() }
  }

  override fun setLayout(libraryId: LibraryId, entries: List<HomeLayoutEntry>) {
    val stored = entries.map { StoredEntry(it.shelfId, it.visible, it.label) }
    settings.putString(key(libraryId), json.encodeToString(stored))
  }

  override fun resetLayout(libraryId: LibraryId) {
    settings.remove(key(libraryId))
  }

  private fun key(libraryId: LibraryId): String {
    return "home_layout_${userSession.userId.orEmpty()}_$libraryId"
  }

  /**
   * An unreadable layout falls back to the server's order rather than breaking Home.
   */
  private fun String.decodeLayout(): List<HomeLayoutEntry>? {
    return try {
      json.decodeFromString<List<StoredEntry>>(this)
        .map { HomeLayoutEntry(it.id, it.visible, it.label) }
    } catch (e: IllegalArgumentException) {
      bark(LogPriority.WARN, throwable = e) { "Ignoring unreadable home layout" }
      null
    }
  }

  @Serializable
  private data class StoredEntry(
    val id: String,
    val visible: Boolean,
    val label: String = "",
  )

  private companion object {
    val json = Json { ignoreUnknownKeys = true }
  }
}
