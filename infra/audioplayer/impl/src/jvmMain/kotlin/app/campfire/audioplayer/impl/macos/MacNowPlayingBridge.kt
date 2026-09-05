// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import app.campfire.audioplayer.impl.macos.ObjC.put
import app.campfire.audioplayer.impl.macos.ObjC.release
import app.campfire.audioplayer.impl.macos.ObjC.utf8
import app.campfire.core.logging.Cork
import com.sun.jna.Callback
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * [NowPlayingBridge] over MediaPlayer.framework (`MPNowPlayingInfoCenter`, `MPRemoteCommandCenter`).
 *
 * Every framework call is made on the AppKit main thread via [MainQueue]. Remote commands arrive
 * through a runtime-defined Objective-C class whose handler methods are JNA callbacks; the
 * callbacks are held in fields so they outlive the registration.
 *
 * macOS quirk (documented by Apple): `playbackState` must be set on every play/pause, otherwise
 * media keys keep launching Music and Control Center shows nothing.
 */
internal class MacNowPlayingBridge : NowPlayingBridge {

  private val mediaPlayer: NativeLibrary = NativeLibrary.getInstance(MEDIA_PLAYER_FRAMEWORK)

  init {
    // NSImage lives in AppKit; make sure the class is resolvable even before a window exists
    NativeLibrary.getInstance(APP_KIT_FRAMEWORK)
  }

  private val keyTitle = ObjC.global(mediaPlayer, "MPMediaItemPropertyTitle")
  private val keyArtist = ObjC.global(mediaPlayer, "MPMediaItemPropertyArtist")
  private val keyAlbum = ObjC.global(mediaPlayer, "MPMediaItemPropertyAlbumTitle")
  private val keyDuration = ObjC.global(mediaPlayer, "MPMediaItemPropertyPlaybackDuration")
  private val keyElapsed = ObjC.global(mediaPlayer, "MPNowPlayingInfoPropertyElapsedPlaybackTime")
  private val keyRate = ObjC.global(mediaPlayer, "MPNowPlayingInfoPropertyPlaybackRate")
  private val keyDefaultRate = ObjC.global(mediaPlayer, "MPNowPlayingInfoPropertyDefaultPlaybackRate")
  private val keyMediaType = ObjC.global(mediaPlayer, "MPNowPlayingInfoPropertyMediaType")
  private val keyArtwork = ObjC.global(mediaPlayer, "MPMediaItemPropertyArtwork")

  /**
   * The decoded cover the artwork request handler hands back. The system may call the handler of
   * an older MPMediaItemArtwork at any time, so every handler resolves to the *current* image and
   * the previous image is kept alive for one more rotation before it is released.
   */
  @Volatile
  private var currentImage: Pointer? = null
  private var previousImage: Pointer? = null
  private var currentArtworkBytes: ByteArray? = null
  private val artworkRequestBlock = ObjC.ImageRequestBlock { _, _ -> currentImage }

  @Volatile
  private var handler: RemoteCommandHandler? = null

  /** IMP callbacks, kept alive here. Each returns MPRemoteCommandHandlerStatusSuccess. */
  private val imps: Map<String, CommandImp> = mapOf(
    "handlePlay:" to CommandImp { handler?.play() },
    "handlePause:" to CommandImp { handler?.pause() },
    "handleToggle:" to CommandImp { handler?.togglePlayPause() },
    "handleSkipForward:" to CommandImp { handler?.skipForward() },
    "handleSkipBackward:" to CommandImp { handler?.skipBackward() },
    "handleNext:" to CommandImp { handler?.nextTrack() },
    "handlePrevious:" to CommandImp { handler?.previousTrack() },
    "handleChangePosition:" to CommandImp { event ->
      val seconds = ObjC.sendDouble(event, "positionTime")
      handler?.seekTo(seconds.seconds)
    },
  )

  private val commandsToSelectors = listOf(
    "playCommand" to "handlePlay:",
    "pauseCommand" to "handlePause:",
    "togglePlayPauseCommand" to "handleToggle:",
    "skipForwardCommand" to "handleSkipForward:",
    "skipBackwardCommand" to "handleSkipBackward:",
    "nextTrackCommand" to "handleNext:",
    "previousTrackCommand" to "handlePrevious:",
    "changePlaybackPositionCommand" to "handleChangePosition:",
  )

  /** The command target instance; created on the main thread on first use. */
  private var target: Pointer? = null

  private inner class CommandImp(private val action: (event: Pointer) -> Unit) : Callback {
    @Suppress("unused")
    fun invoke(self: Pointer?, selector: Pointer?, event: Pointer?): Long {
      try {
        if (event != null) action(event)
      } catch (t: Throwable) {
        ebark(t) { "Remote command handler failed" }
        return STATUS_COMMAND_FAILED
      }
      return STATUS_SUCCESS
    }
  }

  private fun center(): Pointer = ObjC.send(ObjC.cls("MPNowPlayingInfoCenter"), "defaultCenter")!!

  private fun commandCenter(): Pointer = ObjC.send(ObjC.cls("MPRemoteCommandCenter"), "sharedCommandCenter")!!

  private fun target(): Pointer {
    target?.let { return it }
    val cls = ObjC.defineClass(
      name = TARGET_CLASS,
      superclass = "NSObject",
      methods = imps.map { (selector, imp) -> ObjC.Method(selector, "q@:@", imp) },
    )
    return ObjC.send(cls, "new")!!.also { target = it }
  }

  override fun setNowPlaying(info: NowPlayingInfo?) = MainQueue.post {
    ObjC.autoreleased {
      val center = center()
      if (info == null) {
        ObjC.sendVoid(center, "setNowPlayingInfo:", null)
        return@autoreleased
      }
      val dict = ObjC.nsMutableDictionary()
      info.title?.let { dict.put(keyTitle, ObjC.nsString(it)) }
      info.artist?.let { dict.put(keyArtist, ObjC.nsString(it)) }
      info.album?.let { dict.put(keyAlbum, ObjC.nsString(it)) }
      dict.put(keyDuration, ObjC.nsNumber(info.duration.toDouble(DurationUnit.SECONDS)))
      dict.put(keyElapsed, ObjC.nsNumber(info.elapsed.toDouble(DurationUnit.SECONDS)))
      dict.put(keyRate, ObjC.nsNumber(info.rate))
      dict.put(keyDefaultRate, ObjC.nsNumber(info.defaultRate))
      dict.put(keyMediaType, ObjC.nsNumber(MEDIA_TYPE_AUDIO))
      artworkFor(info.artwork)?.let { artwork ->
        dict.put(keyArtwork, artwork)
        artwork.release()
      }
      ObjC.sendVoid(center, "setNowPlayingInfo:", dict)
    }
  }

  /** An owned MPMediaItemArtwork for [bytes], decoding a new NSImage only when the bytes change. */
  private fun artworkFor(bytes: ByteArray?): Pointer? {
    if (bytes == null) return null
    if (bytes !== currentArtworkBytes) {
      val image = ObjC.send(ObjC.cls("NSImage"), "alloc")
        ?.let { ObjC.send(it, "initWithData:", ObjC.nsData(bytes)) }
      if (image == null) {
        wbark { "Cover image could not be decoded (${bytes.size} bytes)" }
        return null
      }
      previousImage?.release()
      previousImage = currentImage
      currentImage = image
      currentArtworkBytes = bytes
    }
    val artwork = ObjC.send(ObjC.cls("MPMediaItemArtwork"), "alloc") ?: return null
    return ObjC.send(
      artwork,
      "initWithBoundsSize:requestHandler:",
      ARTWORK_BOUNDS,
      ARTWORK_BOUNDS,
      artworkRequestBlock.pointer,
    )
  }

  override fun setPlaybackState(state: NowPlayingState) = MainQueue.post {
    val code = when (state) {
      NowPlayingState.Playing -> PLAYBACK_STATE_PLAYING
      NowPlayingState.Paused -> PLAYBACK_STATE_PAUSED
      NowPlayingState.Stopped -> PLAYBACK_STATE_STOPPED
    }
    ObjC.sendVoid(center(), "setPlaybackState:", code)
  }

  override fun setCommandHandler(handler: RemoteCommandHandler?, skipForward: Duration, skipBackward: Duration) {
    this.handler = handler
    MainQueue.post {
      ObjC.autoreleased {
        val commandCenter = commandCenter()
        val target = target()
        commandsToSelectors.forEach { (command, selector) ->
          val cmd = ObjC.send(commandCenter, command) ?: return@forEach
          ObjC.sendVoid(cmd, "removeTarget:", target)
          if (handler != null) {
            ObjC.sendVoid(cmd, "addTarget:action:", target, ObjC.sel(selector))
          }
          ObjC.sendVoid(cmd, "setEnabled:", if (handler != null) 1L else 0L)
        }
        if (handler != null) {
          ObjC.send(commandCenter, "skipForwardCommand")?.let { cmd ->
            ObjC.sendVoid(
              cmd,
              "setPreferredIntervals:",
              ObjC.nsArray(ObjC.nsNumber(skipForward.toDouble(DurationUnit.SECONDS))),
            )
          }
          ObjC.send(commandCenter, "skipBackwardCommand")?.let { cmd ->
            ObjC.sendVoid(
              cmd,
              "setPreferredIntervals:",
              ObjC.nsArray(ObjC.nsNumber(skipBackward.toDouble(DurationUnit.SECONDS))),
            )
          }
        }
      }
    }
  }

  internal data class ReadBack(val title: String?, val playbackState: Long, val artworkResolves: Boolean)

  /** What the system currently holds for us; used by the integration test to prove the round trip. */
  internal fun readBack(): ReadBack = MainQueue.call {
    ObjC.autoreleased {
      val center = center()
      val info = ObjC.send(center, "nowPlayingInfo")
      val title = info?.let { ObjC.send(it, "objectForKey:", keyTitle) }?.utf8()
      // Asking the artwork for an image runs our request block through the real block ABI
      val artworkResolves = info?.let { ObjC.send(it, "objectForKey:", keyArtwork) }
        ?.let { ObjC.send(it, "imageWithSize:", 300.0, 300.0) } != null
      ReadBack(title, ObjC.sendLong(center, "playbackState"), artworkResolves)
    }
  }

  companion object : Cork {
    override val tag: String = "MacNowPlayingBridge"
    override val enabled: Boolean = true

    private const val MEDIA_PLAYER_FRAMEWORK = "/System/Library/Frameworks/MediaPlayer.framework/MediaPlayer"
    private const val APP_KIT_FRAMEWORK = "/System/Library/Frameworks/AppKit.framework/AppKit"

    /** Bounds advertised for the artwork; covers are requested at this width from the server. */
    private const val ARTWORK_BOUNDS = 1200.0
    private const val TARGET_CLASS = "CampfireRemoteCommandTarget"

    private const val STATUS_SUCCESS = 0L
    private const val STATUS_COMMAND_FAILED = 200L

    private const val PLAYBACK_STATE_PLAYING = 1L
    private const val PLAYBACK_STATE_PAUSED = 2L
    private const val PLAYBACK_STATE_STOPPED = 3L

    private const val MEDIA_TYPE_AUDIO = 1L
  }
}
