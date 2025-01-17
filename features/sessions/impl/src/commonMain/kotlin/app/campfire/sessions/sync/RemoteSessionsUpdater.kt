package app.campfire.sessions.sync

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Cork
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.network.AudioBookShelfApi
import app.campfire.sessions.db.SessionDataSource
import app.campfire.sessions.network.NetworkSessionMapper
import com.r0adkll.kimchi.annotations.ContributesBinding
import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

interface RemoteSessionsUpdater {

  suspend fun update(skipInterval: Boolean = false)
}

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class NetworkRemoteSessionsUpdater(
  private val api: AudioBookShelfApi,
  private val sessionDataSource: SessionDataSource,
  private val networkSessionMapper: NetworkSessionMapper,
  private val userSession: UserSession,
  private val connectivity: Connectivity,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
) : RemoteSessionsUpdater {

  private var lastSyncTimeMs: Long = 0L
  private var currentSyncJob: Job? = null

  override suspend fun update(skipInterval: Boolean) = withContext(dispatcherProvider.computation) {
    // Check for running jobs
    if (currentSyncJob != null && currentSyncJob!!.isActive) {
      wbark { "A sync job is already in progress, skipping" }
      return@withContext
    }

    // Check for connectivity
    val status = connectivity.status()
    if (status.isConnected) {
      val elapsed = fatherTime.nowInEpochMillis() - lastSyncTimeMs
      val interval = when {
        status.isMetered -> SYNC_INTERVAL_METERED
        else -> SYNC_INTERVAL_NOT_METERED
      }

      if (elapsed >= interval || skipInterval) {
        ibark { "Starting session sync with $status connection" }
        currentSyncJob = async {
          try {
            syncLocalSessionsToServer()
          } catch (e: Exception) {
            ebark { "Error syncing local sessions to the server: $e" }
          } finally {
            lastSyncTimeMs = fatherTime.nowInEpochMillis()
          }
        }
      }
    } else {
      wbark { "Network is not currently connected deferring sync" }
    }
  }

  private suspend fun syncLocalSessionsToServer() {
    // Read local sessions from db
    val currentUserId = userSession.userId ?: return
    val localSessions = sessionDataSource.getSessions(currentUserId)

    if (localSessions.isNotEmpty()) {
      val networkPlaybackSessions = localSessions.map { networkSessionMapper.map(it) }
      val result = api.syncLocalSessions(networkPlaybackSessions)
      result
        .onSuccess { r ->
          ibark { "Local Session Sync for User($currentUserId) Successful!" }
          r.results.forEach {
            dbark { "--> Sync Result $it" }
          }
        }
        .onFailure { t ->
          ebark(t) { "Failed to sync for User($currentUserId)" }
        }
    } else {
      dbark { "Local sessions empty, skipping sync" }
    }
  }

  companion object : Cork {
    override val tag: String = "NetworkRemoteSessionsUpdater"

    private const val SYNC_INTERVAL_NOT_METERED = 15_000L // 15s
    private const val SYNC_INTERVAL_METERED = 60_000L // 1m
  }
}
