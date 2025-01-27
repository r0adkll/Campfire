package app.campfire.ios.logging

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.logging.Heartwood
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class IosLoggingInitializer : AppInitializer {

  override suspend fun onInitialize() {
    Heartwood.grow(IosBark)
  }
}
