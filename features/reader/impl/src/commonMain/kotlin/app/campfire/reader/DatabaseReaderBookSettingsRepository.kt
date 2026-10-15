// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.data.ReaderBookSettings
import app.campfire.reader.api.ReaderBookSettingsRepository
import app.campfire.settings.api.ReaderFlow
import app.campfire.settings.api.ReaderFont
import app.campfire.settings.api.ReaderLayoutOverrides
import app.campfire.settings.api.ReaderMargin
import app.campfire.settings.api.ReaderPages
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@ContributesBinding(UserScope::class)
@Inject
class DatabaseReaderBookSettingsRepository(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : ReaderBookSettingsRepository {

  override fun observeOverrides(itemId: LibraryItemId): Flow<ReaderLayoutOverrides> =
    db.readerBookSettingsQueries.selectForItem(userSession.requiredUserId, itemId)
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .map { it?.asOverrides() ?: ReaderLayoutOverrides.None }

  override suspend fun setOverrides(itemId: LibraryItemId, overrides: ReaderLayoutOverrides) {
    val userId = userSession.requiredUserId
    withContext(dispatcherProvider.databaseWrite) {
      if (overrides.isEmpty) {
        db.readerBookSettingsQueries.deleteForItem(userId, itemId)
      } else {
        db.readerBookSettingsQueries.upsert(overrides.asDbModel(userId, itemId))
      }
    }
  }
}

private fun ReaderBookSettings.asOverrides() = ReaderLayoutOverrides(
  font = font?.let(ReaderFont::fromStorageKey),
  fontScale = fontScale,
  lineHeight = lineHeight?.toFloat(),
  margin = margin?.let(ReaderMargin::fromStorageKey),
  justify = justify,
  hyphenate = hyphenate,
  flow = flow?.let(ReaderFlow::fromStorageKey),
  pages = pages?.let(ReaderPages::fromStorageKey),
)

private fun ReaderLayoutOverrides.asDbModel(userId: String, itemId: LibraryItemId) = ReaderBookSettings(
  userId = userId,
  libraryItemId = itemId,
  font = font?.storageKey,
  fontScale = fontScale,
  lineHeight = lineHeight?.toDouble(),
  margin = margin?.storageKey,
  justify = justify,
  hyphenate = hyphenate,
  flow = flow?.storageKey,
  pages = pages?.storageKey,
)
