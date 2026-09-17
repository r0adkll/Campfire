// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.purge

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Corked
import app.campfire.core.model.LibraryId
import app.campfire.core.time.FatherTime
import app.campfire.libraries.api.LibraryItemPurger
import app.campfire.libraries.paging.fetchAllPages
import app.campfire.network.AudioBookShelfApi
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import com.russhwolf.settings.ObservableSettings
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Catches library items that were removed from the server without the app hearing about it —
 * while it was offline or had realtime sync off, or through a direct edit of the server's
 * database, which never emits a socket event. Once a day per library it lists every item on
 * the server and asks about any cached item missing from that listing.
 */
@ContributesMultibinding(UserScope::class, boundType = Scoped::class)
@Inject
class RemovedLibraryItemReconciler(
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val purger: LibraryItemPurger,
  private val userRepository: UserRepository,
  private val settings: ObservableSettings,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Scoped {

  companion object : Corked("RemovedLibraryItemReconciler")

  override suspend fun onCreate() {
    coroutineScopeHolder.get().launch {
      // Stay out of the way of the network work that starts with a session.
      delay(STARTUP_DELAY)
      userRepository.observeCurrentUser()
        .map { it.selectedLibraryId }
        .distinctUntilChanged()
        .collectLatest { libraryId ->
          try {
            reconcileIfStale(libraryId)
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            ebark(e) { "Reconciling library $libraryId failed" }
          }
        }
    }
  }

  suspend fun reconcileIfStale(libraryId: LibraryId) {
    val lastReconciledAt = settings.getLongOrNull(lastReconciledKey(libraryId)) ?: 0L
    if (fatherTime.nowInEpochMillis() - lastReconciledAt < RECONCILE_INTERVAL.inWholeMilliseconds) return

    if (reconcile(libraryId)) {
      settings.putLong(lastReconciledKey(libraryId), fatherTime.nowInEpochMillis())
    }
  }

  /**
   * @return true when the server's listing was fully read and the cache checked against it
   */
  suspend fun reconcile(libraryId: LibraryId): Boolean {
    var total = 0
    val serverItems = fetchAllPages(maxPages = MAX_PAGES) { page ->
      api.getLibraryItemsMinified(libraryId = libraryId, page = page, limit = PAGE_SIZE)
        .onSuccess { total = it.total }
    }.getOrElse {
      ebark(it) { "Unable to list library $libraryId" }
      return false
    }

    // A listing cut short (page cap, items shifting between pages) would make items look
    // missing. The server confirms each candidate anyway, but skip the pass to avoid the churn.
    if (serverItems.size < total) {
      wbark { "Listing for library $libraryId was incomplete (${serverItems.size}/$total), skipping" }
      return false
    }

    val serverIds = serverItems.mapTo(HashSet()) { it.id }
    val cachedIds = withContext(dispatcherProvider.databaseRead) {
      db.libraryItemsQueries.selectIdsForLibrary(libraryId).awaitAsList()
    }

    val candidates = cachedIds.filterNot { it in serverIds }
    if (candidates.isEmpty()) return true

    // Each candidate costs a request. Checking a random subset keeps a pass cheap, and means
    // items the server won't confirm either way (e.g. 403s) can't starve the rest.
    val removed = purger.purgeIfRemoved(candidates.shuffled().take(MAX_CANDIDATES_PER_PASS))
    ibark { "Library $libraryId: ${candidates.size} cached item(s) missing from server, purged ${removed.size}" }
    return true
  }

  private fun lastReconciledKey(libraryId: LibraryId) = "library_items_reconciled_at_$libraryId"
}

private val STARTUP_DELAY = 10.seconds
private val RECONCILE_INTERVAL = 24.hours
private const val PAGE_SIZE = 100
private const val MAX_PAGES = 1_000
private const val MAX_CANDIDATES_PER_PASS = 50
