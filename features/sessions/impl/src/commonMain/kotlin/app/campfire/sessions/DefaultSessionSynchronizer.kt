package app.campfire.sessions

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.asSeconds
import app.campfire.core.extensions.epochMilliseconds
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Session
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.sessions.api.SessionSynchronizer
import app.campfire.sessions.db.SessionDataSource
import app.campfire.sessions.network.NetworkSessionMapper
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class DefaultSessionSynchronizer(
  private val api: AudioBookShelfApi,
  private val dataSource: SessionDataSource,
  private val mapper: NetworkSessionMapper,
  private val dispatcherProvider: DispatcherProvider,
) : SessionSynchronizer {

  override suspend fun sync(libraryItemId: LibraryItemId) {
    bark { "Starting Session Sync for $libraryItemId" }
    val session = dataSource.getSession(libraryItemId)
    if (session != null) {
      // Sync the "Playback Session" on the server
//      syncSession(session)

      // Sync the media progress on the server
      syncMediaProgress(libraryItemId, session)
    }
  }

  private suspend fun syncSession(session: Session) {
    val networkSession = mapper.map(session)
    withContext(dispatcherProvider.io) {
      val result = api.syncLocalSession(networkSession)
      if (result.isSuccess) {
        bark(LogPriority.INFO) { "Session(${session.libraryItem.id}) Sync Success" }
      } else {
        bark(LogPriority.ERROR) { "Session(${session.libraryItem.id}) Sync Failed" }
      }
    }
  }

  private suspend fun syncMediaProgress(libraryItemId: LibraryItemId, session: Session) {
    withContext(dispatcherProvider.io) {
      val mediaProgressUpdate = MediaProgressUpdatePayload(
        duration = session.duration.asSeconds(),
        progress = session.progress,
        currentTime = session.currentTime.asSeconds(),
        isFinished = session.isFinished,
        hideFromContinueListening = false,
        finishedAt = if (session.isFinished) {
          session.updatedAt.epochMilliseconds
        } else {
          null
        },
        startedAt = session.startedAt.epochMilliseconds,
      )

      val result = api.updateMediaProgress(libraryItemId, mediaProgressUpdate)
      if (result.isSuccess) {
        bark { "Session(${session.libraryItem.id}) Media Progress Update Success\n$mediaProgressUpdate" }
      } else {
        bark { "Session(${session.libraryItem.id}) Media Progress Update Failed" }
      }
    }
  }
}
