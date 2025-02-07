package app.campfire.audioplayer.impl.sync

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@Inject
@ContributesMultibinding(AppScope::class)
class PlaybackSynchronizerAppInitializer(
  private val synchronizer: PlaybackSynchroOrchestrator,
) : AppInitializer {

  override suspend fun onInitialize() {
    synchronizer.sync()
  }
}
