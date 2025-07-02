package app.campfire.widgets.di

import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.OneShotPlaybackController
import app.campfire.core.di.UserScope
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(UserScope::class)
interface ActionCallbackComponent {
  val sessionsRepository: SessionsRepository
  val audioPlayerHolder: AudioPlayerHolder
  val oneShotPlaybackController: OneShotPlaybackController
}
