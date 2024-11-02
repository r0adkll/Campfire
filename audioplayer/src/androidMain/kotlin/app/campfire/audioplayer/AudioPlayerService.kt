package app.campfire.audioplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.campfire.common.settings.PlaybackSettings
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(AppScope::class)
interface AudioPlayerComponent {
  val playbackSettings: PlaybackSettings
  val serviceAudioPlayer: ServiceAudioPlayer
}

@SuppressLint("UnsafeOptInUsageError")
class AudioPlayerService : MediaSessionService() {

  private lateinit var player: ExoPlayerAudioPlayer
  private var session: MediaSession? = null

  private val component by lazy {
    ComponentHolder.component<AudioPlayerComponent>()
  }

  override fun onCreate() {
    super.onCreate()
    player = ExoPlayerAudioPlayer(this, component.playbackSettings)
    session = MediaSession.Builder(this, player.exoPlayer).build()

    component.serviceAudioPlayer.setDelegate(player)
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = session?.player!!
    if (
      !player.playWhenReady
      || player.mediaItemCount == 0
      || player.playbackState == Player.STATE_ENDED
    ) {
      // Stop the service if not playing, continue playing in the background
      // otherwise.
      stopSelf()
    }
  }

  override fun onDestroy() {
    session?.run {
      player.release()
      release()
      session = null
    }
    component.serviceAudioPlayer.setDelegate(null)
    super.onDestroy()
  }

  companion object {
    fun start(context: Context) {
      val intent = Intent(context, AudioPlayerService::class.java)
      context.startForegroundService(intent)
    }
  }
}
