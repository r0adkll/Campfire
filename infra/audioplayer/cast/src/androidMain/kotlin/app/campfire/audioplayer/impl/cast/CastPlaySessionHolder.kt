// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import app.campfire.account.api.ServerRepository
import app.campfire.account.api.UserSessionManager
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.ServerVersion
import app.campfire.core.model.Session
import app.campfire.core.session.serverUrl
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.DeviceInfo
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject

@ContributesTo(UserScope::class)
interface CastPlaySessionUserComponent {
  val audioBookShelfApi: AudioBookShelfApi
}

/**
 * Stages a server playback session whose id is the Cast receiver's media credential.
 *
 * `/public/session/{id}/track/{index}` serves track bytes with the session UUID as the sole
 * bearer credential (no auth headers, no expiry short of the server's 36h idle reaper), which
 * frees cast playback from the ~1h access-token TTL that `?token=` URLs carry. The session is
 * opened with `forceDirectPlay` (a ~10ms metadata call, no ffmpeg) on a `-cast`-suffixed device
 * id so the server's per-device session dedupe never collides with any future session the phone
 * opens for itself, and it is deliberately never synced — the phone's local accounting remains
 * the sole stats writer, and this session records zero listening time.
 *
 * Everything is fire-and-forget and best-effort: when staging hasn't happened (old server,
 * offline, /play failure, race with the queue send), [CampfireMediaItemConverter] falls back to
 * the `?token=` rewrite, which is exactly the previously shipped behavior.
 */
@SingleIn(AppScope::class)
@Inject
class CastPlaySessionHolder(
  private val serverRepository: ServerRepository,
  private val userSessionManager: UserSessionManager,
  private val campfireSettings: CampfireSettings,
  private val applicationInfo: ApplicationInfo,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) {

  @Volatile
  private var publicTrackUrls: Map<String, String> = emptyMap()

  @Volatile
  private var sessionId: String? = null

  @Volatile
  private var sessionItemKey: String? = null

  private val mutex = Mutex()

  /** The credential-free receiver URL for [trackContentUrl], if a session is staged for it. */
  fun publicUrlFor(trackContentUrl: String): String? = publicTrackUrls[trackContentUrl]

  /**
   * Opens (or re-opens, when the prepared item changed) a credential session for the currently
   * prepared playback session. Fire-and-forget; safe to call redundantly.
   */
  fun openForCurrentSession() {
    val session = audioPlayerHolder.currentPlayer.value?.preparedSession ?: return
    val key = itemKey(session)
    if (key == sessionItemKey && sessionId != null) return
    applicationScope.launch(dispatcherProvider.io) {
      mutex.withLock { open(session, key) }
    }
  }

  /** Closes any staged credential session (empty body — never a zero-delta sync). */
  fun close() {
    if (sessionId == null) return
    applicationScope.launch(dispatcherProvider.io) {
      mutex.withLock { closeCurrent() }
    }
  }

  private suspend fun open(session: Session, key: String) {
    try {
      val versionRaw = serverRepository.getCurrentServer()?.settings?.version ?: return
      val version = ServerVersion.parse(versionRaw)
      if (version == null || version < PUBLIC_SESSION_MIN_VERSION) {
        bark { "Server $versionRaw predates public session URLs; cast stays on token URLs" }
        return
      }

      val api = ComponentHolder.maybeComponent<CastPlaySessionUserComponent>()?.audioBookShelfApi ?: return
      val serverUrl = userSessionManager.current.serverUrl ?: return

      closeCurrent()

      val deviceId = "${campfireSettings.deviceId}$CAST_DEVICE_ID_SUFFIX"
      val playSession = api.startPlaybackSession(
        libraryItemId = session.libraryItem.id,
        episodeId = session.episodeId,
        deviceInfo = DeviceInfo(
          id = deviceId,
          userId = session.userId,
          deviceId = deviceId,
          osName = applicationInfo.osName,
          osVersion = applicationInfo.osVersion,
          clientName = "Campfire",
          clientVersion = applicationInfo.versionName,
          manufacturer = applicationInfo.manufacturer,
          model = applicationInfo.model,
          sdkVersion = applicationInfo.sdkVersion?.toString(),
        ),
        mediaPlayer = CAST_MEDIA_PLAYER,
        // forceDirectPlay wins server-side before mime negotiation is consulted
        supportedMimeTypes = emptyList(),
        forceDirectPlay = true,
      ).getOrElse { error ->
        bark(LogPriority.WARN, throwable = error) { "Unable to open cast credential session" }
        return
      }

      val episode = session.episode
      val urls = if (episode != null) {
        // Podcast sessions always expose exactly one server-side track, at index 1
        val track = episode.audioTrack ?: return
        mapOf(track.contentUrl to publicTrackUrl(serverUrl, playSession.id, 1))
      } else {
        session.libraryItem.media.tracks.associate { track ->
          track.contentUrl to publicTrackUrl(serverUrl, playSession.id, track.index)
        }
      }

      sessionId = playSession.id
      sessionItemKey = key
      publicTrackUrls = urls
      bark { "Staged cast credential session ${playSession.id} (${urls.size} tracks)" }
    } catch (e: Throwable) {
      bark(LogPriority.WARN, throwable = e) { "Failed to stage cast credential session" }
    }
  }

  private suspend fun closeCurrent() {
    val id = sessionId ?: return
    sessionId = null
    sessionItemKey = null
    publicTrackUrls = emptyMap()
    try {
      val api = ComponentHolder.maybeComponent<CastPlaySessionUserComponent>()?.audioBookShelfApi ?: return
      api.closePlaybackSession(id)
        .onFailure { error ->
          // Best-effort: the server's per-device dedupe and 36h reaper clean up strays
          bark(LogPriority.WARN, throwable = error) { "Unable to close cast credential session $id" }
        }
    } catch (e: Throwable) {
      bark(LogPriority.WARN, throwable = e) { "Unable to close cast credential session $id" }
    }
  }

  private fun itemKey(session: Session): String {
    return "${session.libraryItem.id}:${session.episodeId.orEmpty()}"
  }

  private fun publicTrackUrl(serverUrl: String, sessionId: String, trackIndex: Int): String {
    return "$serverUrl/public/session/$sessionId/track/$trackIndex"
  }

  companion object {
    private val PUBLIC_SESSION_MIN_VERSION = ServerVersion(2, 22, 0)
    private const val CAST_DEVICE_ID_SUFFIX = "-cast"
    private const val CAST_MEDIA_PLAYER = "chromecast"
  }
}
