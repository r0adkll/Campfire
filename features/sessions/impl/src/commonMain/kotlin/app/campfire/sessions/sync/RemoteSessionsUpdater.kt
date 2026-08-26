// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Cork
import app.campfire.core.model.Session
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.network.ApiException
import app.campfire.network.AudioBookShelfApi
import app.campfire.sessions.db.SessionDataSource
import app.campfire.sessions.network.NetworkSessionMapper
import com.r0adkll.kimchi.annotations.ContributesBinding
import dev.jordond.connectivity.Connectivity
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
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
  private val serverSessionAttacher: ServerSessionAttacher,
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

    // Cheap throttle first: this is called from the 500ms playback tick, so bail on the
    // unmetered interval before touching platform connectivity. The final (possibly
    // stricter metered) interval is re-checked against the actual connection below.
    val elapsedSinceSync = fatherTime.nowInEpochMillis() - lastSyncTimeMs
    if (elapsedSinceSync < SYNC_INTERVAL_NOT_METERED && !skipInterval) return@withContext

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

    // Sessions with an attach in flight are pending: exactly one sync path may ever report
    // a listening interval, so they wait for the attach to resolve to SERVER or LOCAL.
    val sessions = sessionDataSource.getSessions(currentUserId)
      .filterNot { serverSessionAttacher.isAttachPending(it.libraryItem.id) }

    val (serverOwned, localOwned) = sessions.partition { it.serverSessionId != null }

    serverOwned.forEach { session -> syncServerSession(session) }
    syncLocalSessions(localOwned)
  }

  /**
   * Reports the unreported listening delta into the row's server session, advancing the
   * watermark on success. A 404 means the server lost the session (restart, idle reaper,
   * same-device supersession): the row detaches and falls back to the local path — where
   * the watermark ensures only the still-unreported remainder is ever uploaded.
   */
  private suspend fun syncServerSession(session: Session) {
    val serverSessionId = session.serverSessionId ?: return
    val delta = session.timeListening - session.reportedTimeListening
    val isEnded = session.isFinished || session.isDeleted

    if (delta <= Duration.ZERO && !isEnded) return

    val result = if (isEnded) {
      // Close carries the final delta inline (or an empty body when there is none — a
      // zero-delta sync payload would plant a stray media progress write server-side)
      api.closePlaybackSession(
        sessionId = serverSessionId,
        currentTime = session.currentTime.asSecondsDouble(),
        timeListened = delta.asSecondsDouble(),
        duration = session.duration.asSecondsDouble(),
      )
    } else {
      api.syncPlaybackSession(
        sessionId = serverSessionId,
        currentTime = session.currentTime.asSecondsDouble(),
        timeListened = delta.asSecondsDouble(),
        duration = session.duration.asSecondsDouble(),
      )
    }

    result
      .onSuccess {
        if (isEnded) {
          sessionDataSource.deleteSession(session.libraryItem.id)
          ibark { "Closed server session $serverSessionId and deleted its row" }
        } else {
          sessionDataSource.updateReportedTimeListening(
            libraryItemId = session.libraryItem.id,
            reported = session.reportedTimeListening + delta,
          )
          dbark { "Synced ${delta.inWholeSeconds}s into server session $serverSessionId" }
        }
      }
      .onFailure { error ->
        if ((error as? ApiException)?.statusCode == 404) {
          wbark { "Server session $serverSessionId is gone; row falls back to local sync" }
          sessionDataSource.clearServerSession(session.libraryItem.id)
        } else {
          ebark(error) { "Failed to sync server session $serverSessionId" }
        }
      }
  }

  private suspend fun syncLocalSessions(sessions: List<Session>) {
    // Filter out any with insufficient listening time, counting only what the local path
    // still owes: time already reported through a (now dead) server session must never be
    // re-uploaded — the server sums listening stats across session rows.
    val localSessions = sessions
      .map { session -> session.copy(timeListening = session.timeListening - session.reportedTimeListening) }
      .filter { session ->
        // Omit sessions that have < 5s of listening time. These could likely be errant, or restoration sessions
        // and we should hold off on uploading them until they have a significant amount of listening time
        session.timeListening > 5.seconds
      }

    if (localSessions.isNotEmpty()) {
      val networkPlaybackSessions = localSessions.map { networkSessionMapper.map(it) }
      val result = api.syncLocalSessions(networkPlaybackSessions)
      result
        .onSuccess { r ->
          ibark { "Local Session Sync Successful!" }
          r.results.forEach { result ->
            dbark { "--> Sync Result $result" }
            if (!result.progressSynced) {
              // Not an error: the server's recency guard skipped the MediaProgress write
              // because another device wrote fresher progress (last-write-wins working)
              dbark { "Server skipped progress write for ${result.id} (fresher progress exists)" }
            }
            if (result.success) {
              // Check if session is a completed or deleted session
              val localSession = localSessions.find { it.id.toString() == result.id }
              if (localSession?.isFinished == true || localSession?.isDeleted == true) {
                sessionDataSource.deleteSession(localSession.libraryItem.id)
                ibark { "Completed Local Session Deleted!" }
              }
            }
          }
        }
        .onFailure { t ->
          ebark(t) { "Failed to sync for user" }
        }
    } else {
      dbark { "Local sessions empty, skipping sync" }
    }
  }

  private fun Duration.asSecondsDouble(): Double = inWholeMilliseconds / 1000.0

  companion object : Cork {
    override val tag: String = "NetworkRemoteSessionsUpdater"
    override val enabled: Boolean = true

    private const val SYNC_INTERVAL_NOT_METERED = 15_000L // 15s
    private const val SYNC_INTERVAL_METERED = 60_000L // 1m
  }
}
