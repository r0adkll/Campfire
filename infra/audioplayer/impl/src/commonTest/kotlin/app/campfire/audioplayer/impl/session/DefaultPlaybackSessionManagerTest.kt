// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.session

import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayer.Invocation
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRepository
import app.campfire.common.test.session
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.Session
import app.campfire.home.ui.libraryItem
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.test.FakePlaybackSettings
import app.campfire.user.test.FakeMediaProgressRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class DefaultPlaybackSessionManagerTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val dispatcherProvider = DispatcherProvider(
    io = testDispatcher,
    databaseWrite = testDispatcher,
    databaseRead = testDispatcher,
    computation = testDispatcher,
    main = testDispatcher,
  )

  private val sessionsRepository = FakeSessionsRepository()
  private val sessionQueue = FakeSessionQueue()
  private val player = FakeAudioPlayer()
  private val playbackSettings = FakePlaybackSettings()

  private val manager = DefaultPlaybackSessionManager(
    sessionsRepository = sessionsRepository,
    sessionQueue = sessionQueue,
    mediaProgressRepository = FakeMediaProgressRepository(),
    playbackHistoryRepository = FakePlaybackHistoryRepository(),
    audioPlayerHolder = FakeAudioPlayerHolder().apply { setCurrentPlayer(player) },
    playbackSettings = playbackSettings,
    dispatcherProvider = dispatcherProvider,
  )

  private val episodeA = episode("a")
  private val episodeB = episode("b")
  private val episodeC = episode("c")
  private val podcast = podcast(episodeA, episodeB, episodeC)
  private val book = libraryItem(id = "book")

  @Test
  fun `starting another episode moves the playing one to the front of the queue`() = runTest(testDispatcher) {
    sessionQueue.add(podcast, episodeC)
    sessionsRepository.currentSession = episodeSession(episodeA)
    sessionsRepository.createSession = episodeSession(episodeB)

    manager.startSession(podcast.id, episodeId = episodeB.id)

    assertThat(sessionQueue.keys()).containsExactly("podcast:a", "podcast:c")
  }

  @Test
  fun `starting a book moves the playing episode to the front of the queue`() = runTest(testDispatcher) {
    sessionsRepository.currentSession = episodeSession(episodeA)
    sessionsRepository.createSession = session(libraryItem = book)

    manager.startSession(book.id)

    assertThat(sessionQueue.keys()).containsExactly("podcast:a")
  }

  @Test
  fun `restarting the playing episode leaves the queue alone`() = runTest(testDispatcher) {
    sessionsRepository.currentSession = episodeSession(episodeA)
    sessionsRepository.createSession = episodeSession(episodeA)

    manager.startSession(podcast.id, episodeId = episodeA.id)

    assertThat(sessionQueue.keys()).isEmpty()
  }

  @Test
  fun `starting an episode moves the playing book to the front of the queue`() = runTest(testDispatcher) {
    sessionQueue.add(podcast, episodeC)
    sessionsRepository.currentSession = session(libraryItem = book)
    sessionsRepository.createSession = episodeSession(episodeB)

    manager.startSession(podcast.id, episodeId = episodeB.id)

    assertThat(sessionQueue.keys()).containsExactly("book", "podcast:c")
  }

  @Test
  fun `a playing episode missing from its podcast is not requeued`() = runTest(testDispatcher) {
    sessionsRepository.currentSession = session(libraryItem = podcast).copy(episodeId = "removed")
    sessionsRepository.createSession = session(libraryItem = book)

    manager.startSession(book.id)

    assertThat(sessionQueue.keys()).isEmpty()
  }

  @Test
  fun `a playing episode marked finished is not requeued`() = runTest(testDispatcher) {
    sessionsRepository.currentSession = episodeSession(episodeA).copy(isDeleted = true)
    sessionsRepository.createSession = episodeSession(episodeB)

    manager.startSession(podcast.id, episodeId = episodeB.id)

    assertThat(sessionQueue.keys()).isEmpty()
  }

  @Test
  fun `the playing episode is dropped when requeueing is turned off`() = runTest(testDispatcher) {
    sessionsRepository.currentSession = episodeSession(episodeA)
    sessionsRepository.createSession = episodeSession(episodeB)

    manager.startSession(podcast.id, episodeId = episodeB.id, requeueCurrent = false)

    assertThat(sessionQueue.keys()).isEmpty()
  }

  @Test
  fun `the playing episode is dropped when the requeue setting is off`() = runTest(testDispatcher) {
    playbackSettings.requeueOnSwitch = false
    sessionQueue.add(podcast, episodeC)
    sessionsRepository.currentSession = episodeSession(episodeA)
    sessionsRepository.createSession = episodeSession(episodeB)

    manager.startSession(podcast.id, episodeId = episodeB.id)

    assertThat(sessionQueue.keys()).containsExactly("podcast:c")
  }

  @Test
  fun `advancing past a finished episode does not requeue it`() = runTest(testDispatcher) {
    sessionQueue.add(podcast, episodeB)
    sessionsRepository.createSession = episodeSession(episodeA)
    manager.startSession(podcast.id, episodeId = episodeA.id)

    // The finished episode is still the active session when the queue advances
    sessionsRepository.currentSession = episodeSession(episodeA)
    sessionsRepository.createSession = episodeSession(episodeB)
    player.lastPrepare().onFinished(podcast.id)

    assertThat(sessionQueue.keys()).isEmpty()
    assertThat(player.preparedEpisodeIds()).containsExactly("a", "b")
  }

  @Test
  fun `skipping the playing episode does not requeue it`() = runTest(testDispatcher) {
    sessionQueue.add(podcast, episodeB)
    sessionsRepository.currentSession = episodeSession(episodeA)
    sessionsRepository.createSession = episodeSession(episodeB)

    manager.stopSession(podcast.id, clearQueue = false, episodeId = episodeA.id)

    assertThat(sessionQueue.keys()).isEmpty()
    assertThat(player.preparedEpisodeIds()).containsExactly("b")
  }

  private fun FakeSessionQueue.keys() = queue.map { it.key }

  private fun FakeAudioPlayer.lastPrepare() = invocations.filterIsInstance<Invocation.Prepare>().last()

  private fun FakeAudioPlayer.preparedEpisodeIds() = invocations
    .filterIsInstance<Invocation.Prepare>()
    .map { it.session.episodeId }

  private fun episodeSession(episode: PodcastEpisode): Session {
    return session(libraryItem = podcast).copy(episodeId = episode.id)
  }

  private fun episode(id: String) = PodcastEpisode(
    id = id,
    libraryItemId = "podcast",
    podcastId = "podcast-media",
    title = "Episode $id",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 60_000L,
    sizeInBytes = 0L,
  )

  private fun podcast(vararg episodes: PodcastEpisode): LibraryItem = libraryItem(
    id = "podcast",
    mediaType = MediaType.Podcast,
    media = Media.Podcast(
      id = "podcast-media",
      metadata = Media.Metadata.Podcast(
        title = "Podcast",
        author = null,
        description = null,
        releaseDate = null,
        genres = emptyList(),
        feedUrl = null,
        imageUrl = null,
        itunesPageUrl = null,
        itunesId = null,
        itunesArtistId = null,
        isExplicit = false,
        language = null,
        podcastType = null,
      ),
      coverImageUrl = "",
      coverPath = null,
      tags = emptyList(),
      sizeInBytes = 0L,
      episodes = episodes.toList(),
    ),
  )
}
