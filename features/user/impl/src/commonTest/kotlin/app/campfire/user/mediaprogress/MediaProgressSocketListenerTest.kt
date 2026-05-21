package app.campfire.user.mediaprogress

import app.campfire.data.mapping.asDomainModel
import app.campfire.network.models.MediaProgress as NetworkMediaProgress
import app.campfire.network.models.MediaType
import app.campfire.socket.events.UserItemProgressUpdated
import app.campfire.socket.events.UserSessionClosed
import app.campfire.socket.payloads.UserItemProgressUpdatedPayload
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.FakeMediaProgressRepository.Invocation
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class MediaProgressSocketListenerTest {

  @Test
  fun `progress event upserts via repository with skipUpload`() = runTest {
    val fake = FakeMediaProgressRepository()
    val listener = MediaProgressSocketListener(fake)
    val payload = UserItemProgressUpdatedPayload(id = "prog-1", data = sampleNetworkProgress())

    listener.handle(UserItemProgressUpdated(payload))

    assertThat(fake.invocations).containsExactly(
      Invocation.UpdateProgress(
        newProgress = payload.data.asDomainModel(),
        force = false,
        skipUpload = true,
      ),
    )
  }

  @Test
  fun `non-progress events are ignored`() = runTest {
    val fake = FakeMediaProgressRepository()
    val listener = MediaProgressSocketListener(fake)

    listener.handle(UserSessionClosed(sessionId = "session-1"))

    assertThat(fake.invocations).isEmpty()
  }

  private fun sampleNetworkProgress() = NetworkMediaProgress(
    id = "prog-1",
    userId = "user-1",
    libraryItemId = "li-1",
    episodeId = null,
    mediaItemId = "li-1",
    mediaItemType = MediaType.Book,
    duration = 600f,
    progress = 0.5f,
    currentTime = 300f,
    isFinished = false,
    hideFromContinueListening = false,
    ebookLocation = null,
    ebookProgress = null,
    lastUpdate = 100L,
    startedAt = 50L,
    finishedAt = null,
  )
}
