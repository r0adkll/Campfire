package app.campfire.user.mediaprogress

import app.campfire.core.di.UserScope
import app.campfire.data.mapping.asDomainModel
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import app.campfire.socket.events.UserItemProgressUpdated
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)
@Inject
class MediaProgressSocketListener(
  private val mediaProgressRepository: MediaProgressRepository,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    if (event !is UserItemProgressUpdated) return
    mediaProgressRepository.updateProgress(
      newProgress = event.payload.data.asDomainModel(),
      skipUpload = true,
    )
  }
}
