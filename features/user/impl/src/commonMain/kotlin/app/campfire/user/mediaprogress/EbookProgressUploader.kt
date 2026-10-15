// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.mediaprogress

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Corked
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.session.UserSession
import app.campfire.core.session.user
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.data.MediaProgressFailedUpdate
import app.campfire.data.mapping.asDbModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.network.reachability.ServerReachability
import app.campfire.network.reachability.reconnectTriggers
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.jordond.connectivity.Connectivity
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Sends reading positions to the server. The reader saves a position locally on every page turn,
 * so an upload waits for the reader to settle on a page. A position the server can't take is
 * marked in `mediaProgressFailedUpdate` and sent again by [uploadPending].
 *
 * Only the ebook fields are sent: an item with audio shares its progress record with them, and
 * the listening progress isn't this reader's to send.
 */
interface EbookProgressUploader {

  /** Send [libraryItemId]'s saved position once the reader settles on a page. */
  fun request(libraryItemId: LibraryItemId)

  /** Send every position that couldn't be sent before. */
  suspend fun uploadPending()
}

@OptIn(FlowPreview::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultEbookProgressUploader(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : EbookProgressUploader {

  private val requests = MutableSharedFlow<LibraryItemId>(extraBufferCapacity = 64)
  private val pending = mutableSetOf<LibraryItemId>()
  private val lock = Mutex()

  private val uploads by lazy {
    coroutineScopeHolder.get().launch {
      requests
        .onEach { id -> lock.withLock { pending += id } }
        .debounce(SETTLE)
        .collect {
          val ids = lock.withLock { pending.toList().also { pending.clear() } }
          ids.forEach { upload(it) }
        }
    }
  }

  override fun request(libraryItemId: LibraryItemId) {
    uploads.start()
    requests.tryEmit(libraryItemId)
  }

  override suspend fun uploadPending() {
    val userId = userSession.userId ?: return
    val ids = withContext(dispatcherProvider.databaseRead) {
      db.mediaProgressBookkeepingQueries.getFailedUpdateItemsForUser(userId).awaitAsList()
    }
    if (ids.isNotEmpty()) ibark { "Sending ${ids.size} reading position(s) saved while away" }
    ids.forEach { upload(it) }
  }

  private suspend fun upload(libraryItemId: LibraryItemId) {
    val userId = userSession.userId ?: return
    val row = withContext(dispatcherProvider.databaseRead) {
      db.mediaProgressQueries.selectForEpisode(userId, libraryItemId, "").awaitAsOneOrNull()
    }
    val location = row?.ebookLocation ?: return

    api.updateMediaProgress(
      libraryItemId = libraryItemId,
      update = MediaProgressUpdatePayload(
        ebookLocation = location,
        ebookProgress = row.ebookProgress?.toFloat(),
      ),
    ).onSuccess {
      withContext(dispatcherProvider.databaseWrite) {
        db.mediaProgressBookkeepingQueries.clearFailedUpdateForUser(libraryItemId, userId)
      }
      // A record the server just created has an id here only once it's fetched back
      if (row.id == MediaProgress.UNKNOWN_ID) {
        api.getMediaProgress(libraryItemId).onSuccess { progress ->
          withContext(dispatcherProvider.databaseWrite) {
            db.mediaProgressQueries.insert(progress.asDbModel())
          }
        }
      }
    }.onFailure {
      wbark(it) { "Couldn't send the reading position for $libraryItemId; will retry" }
      withContext(dispatcherProvider.databaseWrite) {
        db.mediaProgressBookkeepingQueries.insertFailedUpdateIfAbsent(
          MediaProgressFailedUpdate(
            libraryItemId = libraryItemId,
            userId = userId,
            timestamp = fatherTime.nowInEpochMillis(),
          ),
        )
      }
    }
  }

  companion object : Corked("EbookProgressUploader")
}

/**
 * Sends reading positions saved while the server was out of reach: at sign-in, in case the app
 * closed with some waiting, and whenever the server may be reachable again.
 */
@ContributesIntoSet(UserScope::class, binding = binding<Scoped>())
@Inject
class ReconnectEbookProgressSync(
  private val userSession: UserSession,
  private val serverReachability: ServerReachability,
  private val connectivity: Connectivity,
  private val uploader: EbookProgressUploader,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Scoped {

  override suspend fun onCreate() {
    val serverUrl = userSession.user?.serverUrl ?: return
    coroutineScopeHolder.get().launch {
      uploader.uploadPending()
      reconnectTriggers(
        status = serverReachability.status,
        inRange = serverReachability.observeInRange(serverUrl),
        online = connectivity.statusUpdates.map { it.isConnected },
      ).collect { uploader.uploadPending() }
    }
  }
}

private val SETTLE = 2.seconds
