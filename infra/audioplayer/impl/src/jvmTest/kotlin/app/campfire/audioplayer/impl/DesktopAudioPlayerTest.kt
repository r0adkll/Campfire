// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalCoroutinesApi::class)

package app.campfire.audioplayer.impl

import app.campfire.audioplayer.AudioPlayer.State
import app.campfire.audioplayer.PlaybackEngineUnavailableException
import app.campfire.audioplayer.impl.engine.EngineState
import app.campfire.audioplayer.impl.engine.FakePlaybackEngine
import app.campfire.audioplayer.impl.engine.FakePlaybackEngine.Open
import app.campfire.audioplayer.impl.engine.PlaybackEngine
import app.campfire.audioplayer.impl.engine.PlaybackEngineEvent
import app.campfire.audioplayer.impl.sleep.FakeSleepTimerManager
import app.campfire.audioplayer.model.profileOrNull
import app.campfire.audioplayer.test.fixtures.chapter
import app.campfire.audioplayer.test.fixtures.session
import app.campfire.audioplayer.test.fixtures.track
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItemId
import app.campfire.settings.test.FakeEqualizerSettings
import app.campfire.settings.test.FakePlaybackSettings
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

class DesktopAudioPlayerTest {

  private val engine = FakePlaybackEngine()
  private val sleepTimer = FakeSleepTimerManager()
  private val settings = FakePlaybackSettings()
  private val equalizerSettings = FakeEqualizerSettings()
  private val finished = mutableListOf<LibraryItemId>()

  // Two tracks of 30min; three chapters at 0-10, 10-30, 30-60 minutes
  private val chapters = listOf(
    chapter(0, 0f, 600f),
    chapter(1, 600f, 1800f),
    chapter(2, 1800f, 3600f),
  )
  private val tracks = listOf(
    track(1, 0f, 1800f),
    track(2, 1800f, 1800f),
  )

  private fun book(
    currentTime: Duration = Duration.ZERO,
    chapters: List<Chapter> = this.chapters,
    hlsStreamUrl: String? = null,
  ) = session(chapters = chapters, tracks = tracks, currentTime = currentTime, hlsStreamUrl = hlsStreamUrl)

  private fun TestScope.player(
    factory: PlaybackEngine.Factory = PlaybackEngine.Factory { engine },
  ) = DesktopAudioPlayer(
    settings = settings,
    equalizerSettings = equalizerSettings,
    sleepTimerManagerFactory = sleepTimer.factory,
    engineFactory = factory,
    accessTokenProvider = { accessToken },
    engineDispatcher = UnconfinedTestDispatcher(testScheduler),
  )

  private var accessToken: String? = null

  private suspend fun DesktopAudioPlayer.prepareBook(
    currentTime: Duration = Duration.ZERO,
    playImmediately: Boolean = true,
    chapterId: Int? = null,
    chapters: List<Chapter> = this@DesktopAudioPlayerTest.chapters,
    hlsStreamUrl: String? = null,
  ) = prepare(book(currentTime, chapters, hlsStreamUrl), playImmediately, chapterId) { finished += it }

  private fun openOf(trackIndex: Int, offset: Duration, playWhenReady: Boolean = true): Open {
    val item = engine.opens.first { it.item.uri.startsWith(tracks[trackIndex].contentUrl) }.item
    return Open(item, offset, playWhenReady)
  }

  private fun playing() = engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Playing))

  private fun position(inItem: Duration) = engine.emit(PlaybackEngineEvent.PositionChanged(inItem))

  private fun ended() = engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Ended))

  @Test
  fun `prepare resumes inside the track containing the session time, to the millisecond`() = runTest {
    val player = player()
    player.prepareBook(currentTime = 40.minutes + 250.milliseconds)

    assertThat(engine.opens).containsExactly(openOf(1, 10.minutes + 250.milliseconds))
    assertThat(player.overallTime.value).isEqualTo(40.minutes + 250.milliseconds)
    assertThat(player.currentTime.value).isEqualTo(10.minutes + 250.milliseconds)
    assertThat(player.currentDuration.value).isEqualTo(30.minutes)
    assertThat(player.currentMetadata.value.title).isEqualTo("Chapter 3")
    assertThat(player.state.value).isEqualTo(State.Initializing)
    assertThat(sleepTimer.sessionStarts).isEqualTo(1)
  }

  @Test
  fun `prepare with a chapter id opens the chapter's track at the chapter's offset`() = runTest {
    val player = player()
    player.prepareBook(currentTime = 40.minutes, chapterId = 1)

    assertThat(engine.opens).containsExactly(openOf(0, 10.minutes))
    assertThat(player.overallTime.value).isEqualTo(10.minutes)
    assertThat(player.currentTime.value).isEqualTo(Duration.ZERO)
    assertThat(player.currentMetadata.value.title).isEqualTo("Chapter 2")
  }

  @Test
  fun `prepare without autoplay opens paused and play pause then starts playback`() = runTest {
    val player = player()
    player.prepareBook(playImmediately = false)
    assertThat(engine.opens).containsExactly(openOf(0, Duration.ZERO, playWhenReady = false))
    assertThat(sleepTimer.sessionStarts).isEqualTo(0)

    player.playPause()
    assertThat(engine.plays).isEqualTo(1)
    player.playPause()
    assertThat(engine.pauses).isEqualTo(1)
  }

  @Test
  fun `position events publish chapter-relative time against the absolute timeline`() = runTest {
    val player = player()
    player.prepareBook()
    playing()

    position(15.minutes)
    assertThat(player.state.value).isEqualTo(State.Playing)
    assertThat(player.overallTime.value).isEqualTo(15.minutes)
    assertThat(player.currentTime.value).isEqualTo(5.minutes)
    assertThat(player.currentDuration.value).isEqualTo(20.minutes)
    assertThat(player.currentMetadata.value.title).isEqualTo("Chapter 2")

    // Second track: item position is offset by the first track's length
    player.seekTo(2)
    position(5.minutes)
    assertThat(player.overallTime.value).isEqualTo(35.minutes)
    assertThat(player.currentTime.value).isEqualTo(5.minutes)
    assertThat(player.currentMetadata.value.title).isEqualTo("Chapter 3")
  }

  @Test
  fun `crossing a chapter boundary between ticks notifies the sleep timer, seeking does not`() = runTest {
    val player = player()
    player.prepareBook()
    playing()

    position(9.minutes + 59.seconds)
    position(10.minutes + 1.seconds)
    assertThat(sleepTimer.endOfChapterCalls).isEqualTo(1)

    player.seekTo(2)
    position(1.seconds)
    assertThat(sleepTimer.endOfChapterCalls).isEqualTo(1)
  }

  @Test
  fun `seeking to a chapter in another track opens that track and plays`() = runTest {
    val player = player()
    player.prepareBook(playImmediately = false)

    player.seekTo(2)
    assertThat(engine.opens.last()).isEqualTo(openOf(1, Duration.ZERO, playWhenReady = true))
    assertThat(engine.seeks).isEmpty()
    assertThat(player.overallTime.value).isEqualTo(30.minutes)
    assertThat(player.currentMetadata.value.title).isEqualTo("Chapter 3")
    assertThat(player.state.value).isEqualTo(State.Buffering)
  }

  @Test
  fun `seeking by progress scrubs within the current chapter using an in-item seek`() = runTest {
    val player = player()
    player.prepareBook()
    playing()
    position(15.minutes)

    player.seekTo(0.5f)
    assertThat(engine.seeks).containsExactly(20.minutes)
    assertThat(engine.opens.size).isEqualTo(1)
    assertThat(player.overallTime.value).isEqualTo(20.minutes)
    assertThat(player.currentTime.value).isEqualTo(10.minutes)
  }

  @Test
  fun `finishing a track opens the next one and finishing the last completes the session`() = runTest {
    val player = player()
    player.prepareBook()
    playing()

    ended()
    assertThat(engine.opens.last()).isEqualTo(openOf(1, Duration.ZERO))
    assertThat(player.state.value).isEqualTo(State.Buffering)
    assertThat(player.overallTime.value).isEqualTo(30.minutes)
    assertThat(player.currentMetadata.value.title).isEqualTo("Chapter 3")
    assertThat(finished).isEmpty()

    playing()
    ended()
    assertThat(player.state.value).isEqualTo(State.Finished)
    assertThat(finished).containsExactly("item-1")
    assertThat(engine.opens.size).isEqualTo(2)
  }

  @Test
  fun `an end of chapter timer firing on a chapterless track change opens the next track paused`() = runTest {
    val player = player()
    player.prepareBook(chapters = emptyList())
    playing()
    sleepTimer.endOfChapterResult = true

    ended()
    assertThat(sleepTimer.endOfChapterCalls).isEqualTo(1)
    assertThat(engine.opens.last()).isEqualTo(openOf(1, Duration.ZERO, playWhenReady = false))
  }

  @Test
  fun `skip next and previous follow chapter boundaries`() = runTest {
    val player = player()
    player.prepareBook()
    playing()

    position(2.minutes)
    player.skipToNext()
    assertThat(engine.seeks.last()).isEqualTo(10.minutes)
    assertThat(player.overallTime.value).isEqualTo(10.minutes)

    // More than the reset threshold into chapter 2: restart it
    position(12.minutes)
    player.skipToPrevious()
    assertThat(engine.seeks.last()).isEqualTo(10.minutes)

    // Just into chapter 2: back to chapter 1
    position(10.minutes + 2.seconds)
    player.skipToPrevious()
    assertThat(engine.seeks.last()).isEqualTo(Duration.ZERO)
  }

  @Test
  fun `seek forward across a track boundary opens the next track at the carried-over offset`() = runTest {
    val player = player()
    player.prepareBook()
    playing()
    position(29.minutes + 50.seconds)

    player.seekForward()
    assertThat(engine.opens.last()).isEqualTo(openOf(1, 20.seconds))
    assertThat(player.overallTime.value).isEqualTo(30.minutes + 20.seconds)

    position(5.seconds)
    player.seekBackward()
    assertThat(engine.opens.last()).isEqualTo(openOf(0, 29.minutes + 55.seconds))
  }

  @Test
  fun `an HLS session is one stream addressed by absolute time`() = runTest {
    val player = player()
    player.prepareBook(currentTime = 40.minutes, hlsStreamUrl = "https://abs/hls/stream.m3u8")

    val open = engine.opens.single()
    assertThat(open.item.uri).isEqualTo("https://abs/hls/stream.m3u8")
    assertThat(open.startPosition).isEqualTo(40.minutes)

    playing()
    position(45.minutes)
    assertThat(player.overallTime.value).isEqualTo(45.minutes)
    assertThat(player.currentTime.value).isEqualTo(15.minutes)
    assertThat(player.currentMetadata.value.title).isEqualTo("Chapter 3")

    // Chapter navigation never re-opens the stream
    player.seekTo(0)
    assertThat(engine.seeks).containsExactly(Duration.ZERO)
    assertThat(engine.opens.size).isEqualTo(1)
  }

  @Test
  fun `a chapterless book reports track-relative progress and skips by track`() = runTest {
    val player = player()
    player.prepareBook(chapters = emptyList())
    playing()

    position(5.minutes)
    assertThat(player.currentTime.value).isEqualTo(5.minutes)
    assertThat(player.currentDuration.value).isEqualTo(30.minutes)
    assertThat(player.currentMetadata.value.title).isEqualTo("Track 1")

    engine.emit(PlaybackEngineEvent.DurationChanged(30.minutes + 1.seconds))
    assertThat(player.currentDuration.value).isEqualTo(30.minutes + 1.seconds)

    player.skipToNext()
    assertThat(engine.opens.last()).isEqualTo(openOf(1, Duration.ZERO))
    assertThat(player.currentMetadata.value.title).isEqualTo("Track 2")
  }

  @Test
  fun `a missing engine surfaces as an unavailable error instead of a crash`() = runTest {
    val player = player(
      factory = PlaybackEngine.Factory { throw PlaybackEngineUnavailableException("no vlc") },
    )
    player.prepareBook()

    assertThat(player.error.value).isNotNull().isInstanceOf<PlaybackEngineUnavailableException>()
    assertThat(player.state.value).isEqualTo(State.Disabled)
    assertThat(engine.opens).isEmpty()
  }

  @Test
  fun `an engine error is published and leaves the controls usable`() = runTest {
    val player = player()
    player.prepareBook()
    engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Opening))
    assertThat(player.state.value).isEqualTo(State.Buffering)

    engine.emit(PlaybackEngineEvent.Error(IllegalStateException("boom")))
    assertThat(player.error.value).isNotNull().isInstanceOf<IllegalStateException>()
    assertThat(player.state.value).isEqualTo(State.Paused)
  }

  @Test
  fun `a stall is flagged only when positions stop, and clears once they advance again`() = runTest {
    val player = player()
    player.prepareBook()
    playing()

    // Routine cache reports during playback are not a stall
    engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Buffering))
    assertThat(player.state.value).isEqualTo(State.Playing)
    advanceTimeBy(500)
    position(1.seconds)
    advanceTimeBy(1_000)
    assertThat(player.state.value).isEqualTo(State.Playing)

    // No position tick after a cache report: stalled
    engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Buffering))
    advanceTimeBy(1_000)
    assertThat(player.state.value).isEqualTo(State.Buffering)

    position(2.seconds)
    assertThat(player.state.value).isEqualTo(State.Playing)
  }

  @Test
  fun `stop tears down the engine and ignores the stop event it causes`() = runTest {
    val player = player()
    player.prepareBook()
    playing()

    player.stop()
    assertThat(engine.stops).isEqualTo(1)
    assertThat(engine.released).isTrue()
    assertThat(player.state.value).isEqualTo(State.Disabled)
    assertThat(player.preparedSession).isNull()
  }

  @Test
  fun `a mid-transition stop event from the engine is not a disabled state`() = runTest {
    val player = player()
    player.prepareBook()
    playing()

    engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Idle))
    assertThat(player.state.value).isEqualTo(State.Playing)
  }

  @Test
  fun `speed and equalizer changes reach the engine`() = runTest {
    val player = player()
    player.prepareBook()

    player.setPlaybackSpeed(1.5f)
    assertThat(player.playbackSpeed.value).isEqualTo(1.5f)
    assertThat(engine.currentRate).isEqualTo(1.5f)

    val profile = equalizerSettings.equalizerProfile.copy(enabled = true, loudnessGainDb = 3f)
    player.setEqualizer(profile)
    assertThat(engine.equalizer?.enabled).isEqualTo(true)
    assertThat(engine.equalizer?.preampDb).isEqualTo(3f)
    assertThat(player.equalizer.value.profileOrNull).isEqualTo(profile)
  }

  @Test
  fun `play after a fade left the volume at zero restores it first`() = runTest {
    val player = player()
    player.prepareBook()
    playing()
    engine.volume = 0f

    player.playPause()
    assertThat(engine.volume).isEqualTo(1f)
    assertThat(engine.pauses).isEqualTo(1)
  }

  @Test
  fun `http track urls carry the current access token, local paths and pre-signed urls do not`() = runTest {
    accessToken = "abc123"
    val player = player()
    val tracks = listOf(
      track(1, 0f, 1800f).copy(contentUrl = "https://abs.example.com/api/items/i/file/1"),
      track(2, 1800f, 1800f).copy(contentUrl = "https://abs.example.com/api/items/i/file/2?x=1"),
      track(3, 3600f, 1800f).copy(contentUrl = "https://abs.example.com/api/items/i/file/3?token=old"),
      track(4, 5400f, 1800f).copy(contentUrl = "/Users/me/Downloads/book/4.m4b"),
    )
    player.prepare(session(tracks = tracks), playImmediately = true) { }

    assertThat(engine.opens.single().item.uri).isEqualTo("https://abs.example.com/api/items/i/file/1?token=abc123")

    player.seekTo(45.minutes)
    assertThat(engine.opens.last().item.uri).isEqualTo("https://abs.example.com/api/items/i/file/2?x=1&token=abc123")

    accessToken = "refreshed"
    player.seekTo(75.minutes)
    assertThat(engine.opens.last().item.uri).isEqualTo("https://abs.example.com/api/items/i/file/3?token=old")

    player.seekTo(105.minutes)
    assertThat(engine.opens.last().item.uri).isEqualTo("/Users/me/Downloads/book/4.m4b")
  }

  @Test
  fun `a missing access token leaves urls untouched`() = runTest {
    val player = player()
    player.prepareBook()
    assertThat(engine.opens.single().item.uri).isEqualTo(tracks[0].contentUrl)
  }

  @Test
  fun `cache reports while paused do not flip the state to buffering`() = runTest {
    val player = player()
    player.prepareBook()
    playing()
    player.pause()
    engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Paused))

    engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Buffering))
    assertThat(player.state.value).isEqualTo(State.Paused)

    engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Playing))
    engine.emit(PlaybackEngineEvent.StateChanged(EngineState.Buffering))
    advanceTimeBy(1_000)
    assertThat(player.state.value).isEqualTo(State.Buffering)
  }

  @Test
  fun `an engine that sends headers gets the token as a bearer header and an untouched url`() = runTest {
    accessToken = "abc123"
    engine.supportsRequestHeaders = true
    val player = player()
    val tracks = listOf(
      track(1, 0f, 1800f).copy(contentUrl = "https://abs.example.com/api/items/i/file/1"),
      track(2, 1800f, 1800f).copy(contentUrl = "/Users/me/Downloads/book/2.m4b"),
    )
    player.prepare(session(tracks = tracks), playImmediately = true) { }

    val streamed = engine.opens.single()
    assertThat(streamed.item.uri).isEqualTo("https://abs.example.com/api/items/i/file/1")
    assertThat(streamed.headers).isEqualTo(mapOf("Authorization" to "Bearer abc123"))

    player.seekTo(45.minutes)
    assertThat(engine.opens.last().item.uri).isEqualTo("/Users/me/Downloads/book/2.m4b")
    assertThat(engine.opens.last().headers).isEqualTo(emptyMap())
  }
}
