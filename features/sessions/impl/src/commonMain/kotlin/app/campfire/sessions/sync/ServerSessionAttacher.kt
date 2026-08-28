// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

import app.campfire.core.Platform.ANDROID
import app.campfire.core.Platform.DESKTOP
import app.campfire.core.Platform.IOS
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.currentPlatform
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.DeviceInfo
import app.campfire.sessions.db.SessionDataSource
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject

/**
 * Opportunistically attaches a server playback session (`POST /api/items/{id}/play`) to a
 * freshly created local session — the canonical ABS flow — without playback ever waiting on
 * it. Local playback starts immediately; when the attach lands (a ~10ms metadata call under
 * `forceDirectPlay` — no ffmpeg), the row's listening deltas route to
 * `/api/session/{id}/sync` instead of the after-the-fact local-session batch.
 *
 * While an attach is in flight the row is *pending*: [isAttachPending] lets the local-session
 * sweep skip it, so exactly one sync path ever reports a given listening interval. Pending
 * state is deliberately in-memory only — a process death mid-attach reverts the row to plain
 * local ownership, which is the designed fallback for every failure here (offline, old
 * server, /play error, timeout).
 */
interface ServerSessionAttacher {

  /** True while a /play attach is in flight for [libraryItemId]. */
  fun isAttachPending(libraryItemId: LibraryItemId): Boolean

  /** Fire-and-forget: attaches a server session to [session] when eligible. */
  fun attachAsync(session: Session)

  /**
   * Opens a transcode (HLS) server session for [session], returning its id and playlist
   * path, or null on any failure or timeout. Unlike [attachAsync] this is a
   * *bounded-blocking* call — an HLS queue cannot exist without the playlist URL — but the
   * /play response itself is ~15ms (the playlist is pre-generated; ffmpeg spawns
   * asynchronously), so the bound is a safety net, not an expected wait.
   */
  suspend fun openTranscodeSession(session: Session): TranscodeStream?

  /** A freshly opened transcode session and the playlist path its /play response reported. */
  data class TranscodeStream(
    val sessionId: String,
    val streamPath: String,
  )
}

@OptIn(ExperimentalAtomicApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class, boundType = ServerSessionAttacher::class)
@Inject
class DefaultServerSessionAttacher(
  private val api: AudioBookShelfApi,
  private val sessionDataSource: SessionDataSource,
  private val campfireSettings: CampfireSettings,
  private val applicationInfo: ApplicationInfo,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : ServerSessionAttacher, Cork {

  override val tag: String = "ServerSessionAttacher"
  override val enabled: Boolean = true

  // Copy-on-write set behind a multiplatform atomic: `synchronized` is JVM-only and this
  // class is commonMain (compiled for iOS too)
  private val pending = AtomicReference<Set<LibraryItemId>>(emptySet())

  override fun isAttachPending(libraryItemId: LibraryItemId): Boolean {
    return libraryItemId in pending.load()
  }

  override fun attachAsync(session: Session) {
    // Downloads play entirely locally and never open server sessions
    if (session.playMethod == PlayMethod.Local) return
    if (session.serverSessionId != null) return

    val itemId = session.libraryItem.id
    if (!markPending(itemId)) return

    applicationScope.launch(dispatcherProvider.io) {
      try {
        attach(session)
      } finally {
        clearPending(itemId)
      }
    }
  }

  /** Returns false when [itemId] was already pending. */
  private fun markPending(itemId: LibraryItemId): Boolean {
    while (true) {
      val current = pending.load()
      if (itemId in current) return false
      if (pending.compareAndSet(current, current + itemId)) return true
    }
  }

  private fun clearPending(itemId: LibraryItemId) {
    while (true) {
      val current = pending.load()
      if (pending.compareAndSet(current, current - itemId)) return
    }
  }

  override suspend fun openTranscodeSession(session: Session): ServerSessionAttacher.TranscodeStream? {
    val itemId = session.libraryItem.id
    if (!markPending(itemId)) return null
    try {
      val playSession = withTimeoutOrNull(TRANSCODE_OPEN_TIMEOUT) {
        api.startPlaybackSession(
          libraryItemId = itemId,
          episodeId = session.episodeId,
          deviceInfo = deviceInfo(session),
          mediaPlayer = platformMediaPlayer(),
          supportedMimeTypes = platformSupportedMimeTypes(),
          forceTranscode = true,
        ).getOrElse { error ->
          wbark(throwable = error) { "Transcode session open failed; falling back to direct play" }
          null
        }
      } ?: return null

      // The response's contentUrl is the stream's authoritative address — without it there
      // is nothing to hand the player, so treat its absence as an open failure
      val streamPath = playSession.audioTracks.firstOrNull()?.contentUrl
      if (streamPath == null) {
        wbark { "Transcode session ${playSession.id} reported no playlist URL; falling back to direct play" }
        return null
      }

      ibark { "Opened transcode session ${playSession.id} for $itemId at $streamPath" }
      return ServerSessionAttacher.TranscodeStream(
        sessionId = playSession.id,
        streamPath = streamPath,
      )
    } finally {
      clearPending(itemId)
    }
  }

  private suspend fun attach(session: Session) {
    // The caller's snapshot can be stale: the HLS router may have attached a transcode
    // session after this launch was scheduled (e.g. a double start from the UI). Firing
    // /play here anyway wouldn't just be wasteful — the server dedupes sessions per
    // device, so it would CLOSE that transcode session and 404 its playlist mid-prepare.
    val currentRow = sessionDataSource.getSession(session.libraryItem.id)
    if (currentRow?.serverSessionId != null) {
      dbark {
        "Row for ${session.libraryItem.id} already carries server session " +
          "${currentRow.serverSessionId}; skipping direct-play attach"
      }
      return
    }

    val playSession = withTimeoutOrNull(ATTACH_TIMEOUT) {
      api.startPlaybackSession(
        libraryItemId = session.libraryItem.id,
        episodeId = session.episodeId,
        deviceInfo = deviceInfo(session),
        mediaPlayer = platformMediaPlayer(),
        supportedMimeTypes = platformSupportedMimeTypes(),
        forceDirectPlay = true,
      ).getOrElse { error ->
        wbark(throwable = error) { "Server session attach failed; row stays locally owned" }
        null
      }
    }

    if (playSession == null) {
      dbark { "No server session attached for ${session.libraryItem.id}" }
      return
    }

    sessionDataSource.attachServerSession(
      libraryItemId = session.libraryItem.id,
      serverSessionId = playSession.id,
      episodeId = session.episodeId,
    )
    ibark { "Attached server session ${playSession.id} to ${session.libraryItem.id}" }
  }

  /** Always the complete object: the server nulls stored device fields missing from a payload. */
  private fun deviceInfo(session: Session): DeviceInfo {
    val deviceId = campfireSettings.deviceId
    return DeviceInfo(
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
    )
  }

  private fun platformMediaPlayer(): String = when (currentPlatform) {
    ANDROID -> "exo-player"
    IOS -> "av-player"
    DESKTOP -> "vlc"
  }

  /**
   * The mime types this platform's player can direct-play. Only consulted server-side when
   * force flags are absent, but sent truthfully anyway: it is what the server would use to
   * negotiate once transcode support lands, and omitting it silently forces transcode.
   */
  private fun platformSupportedMimeTypes(): List<String> = when (currentPlatform) {
    ANDROID, DESKTOP -> listOf(
      "audio/flac",
      "audio/mpeg",
      "audio/mp3",
      "audio/mp4",
      "audio/aac",
      "audio/mp4a-latm",
      "audio/x-m4a",
      "audio/x-m4b",
      "audio/ogg",
      "audio/vorbis",
      "audio/opus",
      "audio/webm",
      "audio/wav",
      "audio/x-wav",
    )

    IOS -> listOf(
      "audio/flac",
      "audio/mpeg",
      "audio/mp3",
      "audio/mp4",
      "audio/aac",
      "audio/mp4a-latm",
      "audio/x-m4a",
      "audio/x-m4b",
      "audio/wav",
      "audio/x-wav",
      "audio/aiff",
      "audio/x-aiff",
    )
  }

  companion object {
    private val ATTACH_TIMEOUT = 10_000L.milliseconds

    // Blocking-adjacent: this bound is what playback start waits on for HLS items
    private val TRANSCODE_OPEN_TIMEOUT = 4_000L.milliseconds
  }
}
